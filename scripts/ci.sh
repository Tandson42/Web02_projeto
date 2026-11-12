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
csrf_page() { # jar caminho -> token CSRF presente na página
    curl -s --max-time 10 -b "$1" -c "$1" "$BASE$2" \
        | grep -oE 'name="_csrf" value="[^"]*"' | head -1 \
        | sed 's/.*value="//;s/"$//'
}
csrf_token() { # jar -> token do formulário de login
    csrf_page "$1" /login
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
J_MEDICO="$TMP/medico.txt"
J_SEC="$TMP/secretario.txt"
J_ADMIN="$TMP/admin.txt"
J_ERR="$TMP/err.txt"
J_NEW="$TMP/new.txt"

# ---- anônimo ----
assert_eq "anônimo em / é redirecionado para /login" \
    "302 $BASE/login" "$(req /dev/null "$BASE/")"
assert_eq "anônimo em /medico/list é redirecionado para /login" \
    "302 $BASE/login" "$(req /dev/null "$BASE/medico/list")"
assert_eq "anônimo em /minhas-consultas é redirecionado para /login" \
    "302 $BASE/login" "$(req /dev/null "$BASE/minhas-consultas")"

# ---- página de login ----
assert_eq "GET /login devolve 200" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' "$BASE/login")"
contains "GET /login exibe o formulário" \
    "$(curl -s --max-time 10 "$BASE/login")" "Informe usuário e senha"

# ---- paciente (user/123) ----
assert_eq "login user/123 autentica e volta para /" \
    "302 $BASE/" "$(login "$J_USER" user 123)"

home_paciente="$(curl -s --max-time 10 -b "$J_USER" "$BASE/")"
contains "home do paciente exibe o usuário autenticado" \
    "$home_paciente" "Olá, <strong>user</strong>"
contains "home do paciente exibe link para Minhas Consultas" \
    "$home_paciente" "/minhas-consultas"
not_contains "paciente não vê Médicos na home" \
    "$home_paciente" "Cadastro e listagem de médicos"
not_contains "paciente não vê Pacientes na home" \
    "$home_paciente" "Cadastro e listagem de pacientes"
not_contains "paciente não vê Consultas na home" \
    "$home_paciente" "Agendamento e listagem de consultas"
not_contains "paciente não vê Usuários na navbar" \
    "$home_paciente" '<a class="nav-link" href="/usuario/list">Usuários</a>'

assert_eq "paciente acessa /minhas-consultas (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/minhas-consultas")"
contains "página minhas-consultas exibe consultas do paciente" \
    "$(curl -s --max-time 10 -b "$J_USER" "$BASE/minhas-consultas")" "Consultas de"
assert_eq "paciente não acessa /paciente/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/paciente/list")"
assert_eq "paciente não acessa /medico/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/medico/list")"
assert_eq "paciente não acessa /consulta/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/consulta/list")"
assert_eq "paciente não acessa /usuario/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/usuario/list")"
assert_eq "paciente não acessa /meus-pacientes (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_USER" "$BASE/meus-pacientes")"

# ---- médico (carlos/123) ----
assert_eq "login carlos/123 autentica e volta para /" \
    "302 $BASE/" "$(login "$J_MEDICO" carlos 123)"

home_medico="$(curl -s --max-time 10 -b "$J_MEDICO" "$BASE/")"
not_contains "médico não vê Meus Pacientes na home" \
    "$home_medico" "/meus-pacientes"
contains "home do médico exibe link para Minhas Consultas" \
    "$home_medico" "/minhas-consultas-medico"
not_contains "médico não vê Médicos na home" \
    "$home_medico" "Cadastro e listagem de médicos"
not_contains "médico não vê Pacientes na home" \
    "$home_medico" "Cadastro e listagem de pacientes"
not_contains "médico não vê Usuários na home" \
    "$home_medico" "Gerenciamento de contas e permissões de acesso"

assert_eq "médico acessa /minhas-consultas-medico (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_MEDICO" "$BASE/minhas-consultas-medico")"
assert_eq "médico não acessa /meus-pacientes (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_MEDICO" "$BASE/meus-pacientes")"
assert_eq "médico não acessa /medico/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_MEDICO" "$BASE/medico/list")"
assert_eq "médico não acessa /paciente/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_MEDICO" "$BASE/paciente/list")"
assert_eq "médico não acessa /consulta/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_MEDICO" "$BASE/consulta/list")"
assert_eq "médico não acessa /usuario/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_MEDICO" "$BASE/usuario/list")"

# ---- secretário (secretario/123) ----
assert_eq "login secretario/123 autentica e volta para /" \
    "302 $BASE/" "$(login "$J_SEC" secretario 123)"

home_sec="$(curl -s --max-time 10 -b "$J_SEC" "$BASE/")"
contains "secretário vê Médicos na navbar" \
    "$home_sec" '<a class="nav-link" href="/medico/list">Médicos</a>'
contains "secretário vê Pacientes na navbar" \
    "$home_sec" '<a class="nav-link" href="/paciente/list">Pacientes</a>'
contains "secretário vê Consultas na navbar" \
    "$home_sec" '<a class="nav-link" href="/consulta/list">Consultas</a>'
contains "secretário vê Usuários na navbar" \
    "$home_sec" '<a class="nav-link" href="/usuario/list">Usuários</a>'

assert_eq "secretário acessa /medico/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_SEC" "$BASE/medico/list")"
assert_eq "secretário acessa /paciente/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_SEC" "$BASE/paciente/list")"
assert_eq "secretário acessa /consulta/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_SEC" "$BASE/consulta/list")"
assert_eq "secretário acessa /usuario/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_SEC" "$BASE/usuario/list")"

# ---- credenciais erradas ----
assert_eq "senha errada volta para /login?error" \
    "302 $BASE/login?error" "$(login "$J_ERR" user ERRADA)"
contains "página de erro exibe a mensagem" \
    "$(curl -s --max-time 10 "$BASE/login?error")" "Usuário ou senha inválidos."

# ---- admin ----
assert_eq "login admin/admin autentica e volta para /" \
    "302 $BASE/" "$(login "$J_ADMIN" admin admin)"

assert_eq "admin acessa /medico/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_ADMIN" "$BASE/medico/list")"
assert_eq "admin acessa /usuario/list (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_ADMIN" "$BASE/usuario/list")"
contains "admin vê a tela de usuários e papéis" \
    "$(curl -s --max-time 10 -b "$J_ADMIN" "$BASE/usuario/list")" "Usuários e papéis"
assert_eq "admin acessa /usuario/edit/2 (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_ADMIN" "$BASE/usuario/edit/2")"

# ---- cadastro de paciente com criação de credenciais PACIENTE ----
token="$(csrf_page "$J_SEC" /paciente/form)"
assert_eq "secretário cadastra paciente com credenciais" \
    "302 $BASE/paciente/list" \
    "$(req "$J_SEC" \
        --data-urlencode "nome=Paciente CI" \
        --data-urlencode "cpf=000.000.000-00" \
        --data-urlencode "email=paciente.ci@email.com" \
        --data-urlencode "telefone=(63) 90000-0000" \
        -d "login=ci_paciente" \
        -d "senha=ci123456" \
        -d "_csrf=$token" \
        "$BASE/paciente/save")"

assert_eq "novo paciente cadastrado autentica no sistema" \
    "302 $BASE/" "$(login "$J_NEW" ci_paciente ci123456)"
assert_eq "novo paciente acessa /minhas-consultas (200)" "200" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_NEW" "$BASE/minhas-consultas")"
assert_eq "novo paciente não acessa /paciente/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_NEW" "$BASE/paciente/list")"
assert_eq "novo paciente não acessa /medico/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_NEW" "$BASE/medico/list")"
assert_eq "novo paciente não acessa /usuario/list (403)" "403" \
    "$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -b "$J_NEW" "$BASE/usuario/list")"
contains "novo paciente aparece na área administrativa de usuários" \
    "$(curl -s --max-time 10 -b "$J_SEC" "$BASE/usuario/list")" "ci_paciente"

# ---- logout ----
token="$(printf '%s' "$home_paciente" | grep -oE 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"$//')"
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
