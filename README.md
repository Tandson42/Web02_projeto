# Clínica Web — Agenda da Clínica, Agendamentos e Segurança

Projeto Spring Boot da disciplina de **Programação para Web II**, com autenticação e autorização baseadas em banco de dados (Spring Security + H2/JPA) e gerenciamento completo de **Agenda da Clínica**, **Agendamentos** e **Registro de Consultas**.

---

## Como executar

```bash
mvn spring-boot:run
```

Acesse `http://localhost:8080/login` (ou `/cadastro` para criar uma conta de paciente).

O banco H2 está em memória (`jdbc:h2:mem:dbname`) e é recriado a cada inicialização. Os dados de demonstração estão em `src/main/resources/import.sql`.

### Executar testes

```bash
mvn test
```

---

## Funcionalidades principais

### 1. Fluxo da Agenda da Clínica
- **Agenda (`DISPONIVEL` / `AGENDADO`)**: disponibilidade de horários dos médicos, com bloqueio de duplicidades para o mesmo médico, bloqueio de datas/horários no passado e controle de concorrência (`@Version`).
- **Agendamento (`AGENDADO` / `REALIZADO` / `CANCELADO`)**: reserva de um horário disponível por um paciente, atualizando a agenda para `AGENDADO` **sem criar consulta** neste momento.
- **Confirmação de Presença e Registro de Consulta**: operação exclusiva da clínica (`SECRETARIO` / `ADMIN`), que cria a entidade `Consulta` vinculada ao `Agendamento` e atualiza o status do agendamento para `REALIZADO`.
- **Cancelamento**: atualiza o agendamento para `CANCELADO` e libera o horário na agenda novamente para `DISPONIVEL`, permitindo que outro paciente o agende preservando o histórico.

### 2. Preço fixo por médico (congelamento no agendamento)
- Cada médico possui seu `valorConsulta` cadastrado.
- Ao realizar o agendamento, o valor atual é **congelado (snapshot)** no `Agendamento` e herdado automaticamente pela `Consulta` na confirmação de presença. Reajustes futuros afetam apenas novos agendamentos.

### 3. Permissões individuais do Médico sobre seus Secretários
- O médico gerencia seus secretários associados em **Meus Secretários** (`/medico/secretarios`), podendo **Ativar** ou **Desligar** individualmente para cada secretário:
  - **Alterar Agenda** (cadastrar/remover horários do médico);
  - **Alterar Preço da Consulta** (alterar o `valorConsulta` do médico).

### 4. Observações Clínicas
- Apenas o **médico responsável pela consulta** pode adicionar ou editar observações (`/consulta/observacao/{id}`).
- O **secretário associado ao médico** (e o `ADMIN`) pode visualizar em modo **somente leitura**.

### 5. Interfaces por perfil (Calendário estilo Google Agenda)
- **Médico** (2 visões): **Agenda de Consultas** (calendário interativo + horários + histórico das próprias consultas + observações) e **Meus Secretários**.
- **Secretário** (3 visões): **Agenda** (calendário unificado, horários, agendamentos com confirmação de presença/cancelamento e histórico), **Pacientes** e **Médicos**.
- **Paciente** (1 visão): **Minha Agenda** (calendário pessoal com seus agendamentos e histórico de consultas realizadas + **preview visual na agenda** ao solicitar uma nova consulta) e auto-cadastro público em `/cadastro`.
- **Admin**: acesso total + gerenciamento de **Usuários e papéis** (`/usuario/**`) e cadastro/edição/exclusão de médicos.

---

## Credenciais de demonstração

| Usuário | Senha | Papéis | Vínculo | Permissões iniciais |
|---|---|---|---|---|
| `admin` | `admin` | `ADMIN`, `SECRETARIO` | — | Acesso administrativo completo |
| `secretario` | `123` | `SECRETARIO` | — | Dr. Carlos: Agenda **Ativada**, Preço **Desligado** · Dra. Ana: Agenda **Desligada**, Preço **Ativado** |
| `clinica` | `123` | `SECRETARIO` | Clínica Saúde LTDA | Dr. Carlos: Agenda **Desligada**, Preço **Desligado** |
| `carlos` | `123` | `MEDICO` | Dr. Carlos Lima (R\$ 250,00) | Agenda de Consultas e Meus Secretários |
| `ana` | `123` | `MEDICO` | Dra. Ana Costa (R\$ 350,00) | Agenda de Consultas e Meus Secretários |
| `user` | `123` | `PACIENTE` | Maria Silva | Minha Agenda e solicitação de consultas |
| `joao` | `123` | `PACIENTE` | João Souza | Minha Agenda e solicitação de consultas |

As senhas são armazenadas com hash **BCrypt**.

---

## Matriz de acesso

| Rota | Anônimo | PACIENTE | MEDICO | SECRETARIO | ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|
| `/login`, `/cadastro`, estáticos | Sim | Sim | Sim | Sim | Sim |
| `/minhas-consultas/**` | Login | Próprias | 403 | 403 | 403 |
| `/agenda/disponiveis` | Login | Sim | 403 | Sim | Sim |
| `/agenda/list`, `/agenda/eventos`, `/agenda/form`, `/agenda/save`, `/agenda/remove/**` | Login | 403 | Própria agenda | Com permissão do médico | Total |
| `/agendamento/novo/**`, `/agendamento/save`, `/agendamento/cancelar/**` | Login | Próprios | 403 | Todos | Todos |
| `/agendamento/confirmar/**` | Login | 403 | 403 | Sim | Sim |
| `/medico/secretarios/**` | Login | 403 | Próprios | 403 | 403 |
| `/medico/preco/**` | Login | 403 | Próprio | Com permissão do médico | Total |
| `/medico/list`, `/medico/consultas/**` | Login | 403 | 403 | Sim | Sim |
| `/medico/form`, `/medico/save`, `/medico/edit/**`, `/medico/remove/**` | Login | 403 | 403 | 403 | Sim |
| `GET /consulta/observacao/**` | Login | 403 | Próprias (edita) | Secretário associado (leitura) | Leitura |
| `POST /consulta/observacao/**` | Login | 403 | Próprias | 403 | 403 |
| `/paciente/**`, `/consulta/**` | Login | 403 | 403 | Sim | Sim |
| `/usuario/**` | Login | 403 | 403 | 403 | Sim |

---

## Principais classes

- **Entidades (`model/entity/`):** `Agenda`, `Agendamento`, `Consulta`, `Medico`, `Paciente`, `Pessoa`, `PessoaFisica`, `PessoaJuridica`, `Usuario`, `Perfil`, `MedicoSecretario`, `StatusAgenda`, `StatusAgendamento`.
- **Repositórios (`model/jdbc/repository/`):** `AgendaRepository`, `AgendamentoRepository`, `ConsultaRepository`, `MedicoRepository`, `PacienteRepository`, `MedicoSecretarioRepository`, `UsuarioRepository`, `PerfilRepository`.
- **Serviços (`service/`):** `AgendaService`, `UsuarioService`, `UsuarioDetailsService`.
- **Segurança (`config/`):** `SecurityConfiguration`.
- **Controllers (`controller/`):** `AgendaController`, `AgendamentoController`, `ConsultaController`, `MedicoController`, `PacienteController`, `UsuarioController`, `HomeController`, `LoginController`.
- **Testes (`src/test/java/`):** `AgendaFluxoTests` (fluxo completo de cadastro, duplicidade, data passada, agendamento, congelamento de preço, cancelamento, reagendamento e confirmação de presença).
