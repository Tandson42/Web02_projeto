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

| Usuário | Senha | Papéis | Pessoa vinculada | O que pode ver |
|---|---|---|---|---|
| `admin` | `admin` | ADMIN, SECRETARIO | — | Acesso total a todas as áreas |
| `secretario` | `123` | SECRETARIO | — | Vê tudo (médicos, pacientes, consultas, usuários) |
| `user` | `123` | PACIENTE | Maria Silva | Vê apenas suas próprias consultas (`/minhas-consultas`) |
| `joao` | `123` | PACIENTE | João Souza | Vê apenas suas próprias consultas (`/minhas-consultas`) |
| `carlos` | `123` | MEDICO | Dr. Carlos Lima | Vê seus pacientes (`/meus-pacientes`) e suas consultas (`/minhas-consultas-medico`) |
| `ana` | `123` | MEDICO | Dra. Ana Costa | Vê seus pacientes (`/meus-pacientes`) e suas consultas (`/minhas-consultas-medico`) |
| `clinica` | `123` | SECRETARIO | Clínica Saúde LTDA | Acesso de secretário |

As senhas são gravadas no banco com hash BCrypt.

## Matriz de acesso

| Rota | Anônimo | PACIENTE | MEDICO | SECRETARIO / ADMIN |
|---|---|---|---|---|
| `/login`, arquivos estáticos | Liberado | Liberado | Liberado | Liberado |
| `/` (Menu) | Redireciona para login | Sim (vê card de suas consultas) | Sim (vê cards de seus pacientes e consultas) | Sim (vê todos os cards) |
| `/minhas-consultas` | Redireciona para login | Sim | 403 | Sim |
| `/meus-pacientes`, `/minhas-consultas-medico` | Redireciona para login | 403 | Sim | Sim |
| `/paciente/consultas/**` | Redireciona para login | 403 | Sim | Sim |
| `/paciente/**` (list, save, edit, remove) | Redireciona para login | 403 | 403 | Sim |
| `/medico/**` | Redireciona para login | 403 | 403 | Sim |
| `/consulta/**` | Redireciona para login | 403 | 403 | Sim |
| `/usuario/**` | Redireciona para login | 403 | 403 | Sim |

Novos pacientes cadastrados recebem automaticamente o perfil `PACIENTE`.
Novos médicos cadastrados recebem automaticamente o perfil `MEDICO`.
Os perfis podem ser gerenciados na tela **Usuários e papéis**.

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