#!/usr/bin/env bash
#
# CI local do projeto — execute antes de cada push:
#
#   ./scripts/ci.sh
#
# O que ele faz:
#   1) Executa os testes do projeto (mvn test);
#   2) Sobe a aplicação em uma porta de teste e valida por HTTP
#      os cenários de segurança (Spring Security + Thymeleaf).
#
# Sai com código 0 se tudo passou; código 1 se algo falhou.
# Não faça push se este script terminar com FALHA.

set -u
cd "$(dirname "$0")/.."

PORT="${PORT:-8081}"
BASE="http://localhost:$PORT"
TMP="$(mktemp -d)"
APP_LOG="$TMP/app.log"
APP_PID=""

cleanup() {
    if [ -n "$APP_PID" ]; then
        kill -- "-$APP_PID" 2>/dev/null || { pkill -P "$APP_PID" 2>/dev/null; kill "$APP_PID" 2>/dev/null; }
        wait "$APP_PID" 2>/dev/null
    fi
    rm -rf "$TMP"
}
trap cleanup EXIT

# ---------- localiza o Maven ----------
find_mvn() {
    if command -v mvn >/dev/null 2>&1; then echo "mvn"; return; fi
    local dist
    dist="$(find "$HOME/.m2/wrapper/dists" -maxdepth 5 -type f -path '*/bin/mvn' 2>/dev/null | sort | tail -1)"
    if [ -n "$dist" ]; then echo "$dist"; return; fi
    if [ -x ./mvnw ]; then echo "./mvnw"; return; fi
    echo "ERRO: Maven não encontrado (nem mvn, nem mvnw, nem wrapper em ~/.m2)" >&2
    exit 1
}
MVN="$(find_mvn)"

# ---------- helpers de asserção ----------
FAIL=0
ok()   { printf '  [OK]     %s\n' "$1"; }
bad()  { printf '  [FALHA]  %s\n' "$1"; FAIL=$((FAIL + 1)); }

assert_eq() { # descrição esperado obtido
    if [ "$2" = "$3" ]; then ok "$1"; else bad "$1 — esperado [$2], obtido [$3]"; fi
}
contains() { # descrição texto trecho
    case "$2" in *"$3"*) ok "$1" ;; *) bad "$1 — trecho não encontrado: $3" ;; esac
}
not_contains() { # descrição texto trecho
    case "$2" in *"$3"*) bad "$1 — não deveria conter: $3" ;; *) ok "$1" ;; esac
}

# ---------- helpers HTTP ----------
# curl -w combinado: imprime "codigo url_de_redirecionamento"
req() { # jar [curl args...]
    local jar="$1"; shift
    curl -s --max-time 10 -o /dev/null -w '%{http_code} %{redirect_url}' -b "$jar" -c "$jar" "$@"
}
csrf_token() { # jar -> token do formulário da página
    curl -s --max-time 10 -b "$1" -c "$1" "$BASE/login" \
        | grep -oE 'name="_csrf" value="[^"]*"' | head -1 \
        | sed 's/.*value="//;s/"$//'
}
login() { # jar usuário senha
    local jar="$1" user="$2" pass="$3" token
    token="$(csrf_token "$jar")"
    req "$jar" -d "username=$user" -d "password=$pass" -d "_csrf=$token" "$BASE/login"
}

# ============================================================
echo "==> 1/2  mvn test"
if ! "$MVN" -q test; then
    echo "FALHA: os testes do projeto não passaram."
    exit 1
fi
echo "    testes OK"
echo

echo "==> 2/2  smoke test de segurança (porta $PORT)"
if command -v setsid >/dev/null 2>&1; then
    setsid "$MVN" -q spring-boot:run \
        -Dspring-boot.run.arguments="--server.port=$PORT" >"$APP_LOG" 2>&1 &
else
    "$MVN" -q spring-boot:run \
        -Dspring-boot.run.arguments="--server.port=$PORT" >"$APP_LOG" 2>&1 &
fi
APP_PID=$!

# aguarda a aplicação subir (máx. 180s)
up=0
for _ in $(seq 1 90); do
    code="$(curl -s --max-time 2 -o /dev/null -w '%{http_code}' "$BASE/login" 2>/dev/null || echo 000)"
    [ "$code" = "200" ] && { up=1; break; }
    if ! kill -0 "$APP_PID" 2>/dev/null; then break; fi
    sleep 2
done
if [ "$up" != "1" ]; then
    echo "FALHA: a aplicação não subiu em $BASE. Log:"
    tail -30 "$APP_LOG"
    exit 1
fi
echo "    aplicação no ar"
echo

J_USER="$TMP/user.txt"
J_ERR="$TMP/err.txt"
J_ADMIN="$TMP/admin.txt"

# ---- anônimo ----
assert_eq "anônimo em / é redirecionado para /login" \
    "302 $BASE/login" "$(req /dev/null "$BASE/")"
assert_eq "anônimo em /medico/list é redirecionado para /login" \
    "302 $BASE/login" "$(req /dev/null "$BASE/medico/list")"

# ---- página de login ----
assert_eq "GET /login devolve 200" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' "$BASE/login")"
contains "GET /login exibe o formulário" \
    "$(curl -s --max-time 10 "$BASE/login")" "Informe usuário e senha"

# ---- usuário comum ----
assert_eq "login user/123 autentica e volta para /" \
    "302 $BASE/" "$(login "$J_USER" user 123)"

home="$(curl -s --max-time 10 -b "$J_USER" "$BASE/")"
contains "home exibe o usuário autenticado (sec:authentication)" \
    "$home" "Olá, <strong>user</strong>"
contains "home exibe o botão Sair" "$home" ">Sair<"
not_contains "user não vê o item Médicos na navbar" \
    "$home" '<a class="nav-link" href="/medico/list">Médicos</a>'
not_contains "user não vê o card Médicos na home" \
    "$home" "Cadastro e listagem de médicos"

assert_eq "user não acessa /medico/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/medico/list")"
assert_eq "user acessa /paciente/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/paciente/list")"
assert_eq "user acessa /consulta/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/consulta/list")"

# ---- credenciais erradas ----
assert_eq "senha errada volta para /login?error" \
    "302 $BASE/login?error" "$(login "$J_ERR" user ERRADA)"
contains "página de erro exibe a mensagem" \
    "$(curl -s --max-time 10 "$BASE/login?error")" "Usuário ou senha inválidos."

# ---- admin ----
assert_eq "login admin/admin autentica e volta para /" \
    "302 $BASE/" "$(login "$J_ADMIN" admin admin)"

home_admin="$(curl -s --max-time 10 -b "$J_ADMIN" "$BASE/")"
contains "admin vê o item Médicos na navbar" \
    "$home_admin" '<a class="nav-link" href="/medico/list">Médicos</a>'
contains "admin vê o card Médicos na home" \
    "$home_admin" "Cadastro e listagem de médicos"
assert_eq "admin acessa /medico/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_ADMIN" "$BASE/medico/list")"

# ---- logout ----
token="$(printf '%s' "$home" | grep -oE 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"$//')"
assert_eq "logout via POST encerra a sessão" \
    "302 $BASE/login?logout" "$(req "$J_USER" -d "_csrf=$token" "$BASE/logout")"
assert_eq "após logout, / volta a exigir login" \
    "302 $BASE/login" "$(req "$J_USER" "$BASE/")"
contains "página /login?logout exibe a mensagem" \
    "$(curl -s --max-time 10 "$BASE/login?logout")" "Você saiu do sistema."

# ---- resumo ----
echo
if [ "$FAIL" -eq 0 ]; then
    echo "RESULTADO: tudo OK — seguro para push."
    exit 0
else
    echo "RESULTADO: $FAIL verificação(ões) falharam — NÃO faça push."
    exit 1
fi
