# Clínica Web — autenticação e autorização

Projeto Spring Boot da disciplina de Programação para Web II, com autenticação
e autorização baseadas em usuários, papéis e senhas armazenados no banco H2.

## Como executar

```bash
mvn spring-boot:run
```

Acesse `http://localhost:8080/login`.

O banco H2 está em memória (`jdbc:h2:mem:dbname`) e é recriado a cada
inicialização. Os dados iniciais estão em `src/main/resources/import.sql`.

## Credenciais de demonstração

| Usuário | Senha | Papéis | Pessoa vinculada |
|---|---|---|---|
| `admin` | `admin` | ADMIN, USER | — |
| `user` | `123` | USER | Maria Silva |
| `joao` | `123` | USER | João Souza |
| `carlos` | `123` | USER | Dr. Carlos Lima |
| `ana` | `123` | USER | Dra. Ana Costa |
| `clinica` | `123` | USER | Clínica Saúde LTDA |

As senhas são gravadas no banco com hash BCrypt.

## Matriz de acesso

| Rota | Anônimo | USER | ADMIN |
|---|---|---|---|
| `/login`, arquivos estáticos | Liberado | Liberado | Liberado |
| `/` | Redireciona para login | Sim | Sim |
| `/paciente/**`, `/consulta/**` | Redireciona para login | Sim | Sim |
| `/medico/**` | Redireciona para login | 403 | Sim |
| `/usuario/**` | Redireciona para login | 403 | Sim |

Novos pacientes e médicos recebem inicialmente o papel `USER`. O papel
`ADMIN` pode ser atribuído na tela **Usuários e papéis**.

## Principais classes

- `model/entity/Usuario.java`: conta de acesso (`UserDetails`) e associação com perfis.
- `model/entity/Perfil.java`: papel de acesso (`GrantedAuthority`).
- `model/entity/Pessoa.java`: associação um-para-um entre pessoa e conta.
- `service/UsuarioDetailsService.java`: carrega usuários do banco para o Spring Security.
- `service/UsuarioService.java`: cria/atualiza credenciais e gerencia papéis.
- `config/SecurityConfiguration.java`: regras de autorização e `BCryptPasswordEncoder`.
- `controller/UsuarioController.java`: área administrativa de usuários e papéis.

## Verificação

```bash
bash scripts/ci.sh
```

O script executa os testes Maven e valida por HTTP o login, o controle de
acesso por papel, o cadastro de uma pessoa com credenciais e o logout.