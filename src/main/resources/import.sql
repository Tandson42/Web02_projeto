-- ============================================================
-- Usuários, perfis e associação com pessoas
--
-- Credenciais de demonstração:
--   admin      / admin -> ADMIN e SECRETARIO (conta administrativa completa)
--   secretario / 123   -> SECRETARIO (acesso a médicos, pacientes, consultas, usuários)
--   user       / 123   -> PACIENTE (Maria Silva - vê apenas suas consultas)
--   joao       / 123   -> PACIENTE (João Souza - vê apenas suas consultas)
--   carlos     / 123   -> MEDICO (Dr. Carlos Lima - vê seus pacientes e consultas)
--   ana        / 123   -> MEDICO (Dra. Ana Costa - vê seus pacientes e consultas)
--   clinica    / 123   -> SECRETARIO (Clínica Saúde LTDA)
-- As senhas abaixo já estão codificadas com BCrypt.
-- ============================================================
INSERT INTO perfil (nome) VALUES ('ADMIN');
INSERT INTO perfil (nome) VALUES ('SECRETARIO');
INSERT INTO perfil (nome) VALUES ('PACIENTE');
INSERT INTO perfil (nome) VALUES ('MEDICO');
INSERT INTO perfil (nome) VALUES ('USER');

INSERT INTO usuario (usuario, senha, ativo) VALUES ('admin', '$2a$10$bNNAqeWsFoLwIRaghWNWrO5vKCq648XRbRMxrlY29pP.hVRvttLEq', TRUE);
INSERT INTO usuario (usuario, senha, ativo) VALUES ('user', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW', TRUE);
INSERT INTO usuario (usuario, senha, ativo) VALUES ('joao', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW', TRUE);
INSERT INTO usuario (usuario, senha, ativo) VALUES ('carlos', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW', TRUE);
INSERT INTO usuario (usuario, senha, ativo) VALUES ('ana', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW', TRUE);
INSERT INTO usuario (usuario, senha, ativo) VALUES ('clinica', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW', TRUE);
INSERT INTO usuario (usuario, senha, ativo) VALUES ('secretario', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW', TRUE);

-- Perfis associados:
-- admin: ADMIN (1) e SECRETARIO (2)
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (1, 1);
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (1, 2);
-- user (Maria): PACIENTE (3)
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (2, 3);
-- joao (João): PACIENTE (3)
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (3, 3);
-- carlos (Dr. Carlos): MEDICO (4)
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (4, 4);
-- ana (Dra. Ana): MEDICO (4)
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (5, 4);
-- clinica: SECRETARIO (2)
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (6, 2);
-- secretario: SECRETARIO (2)
INSERT INTO usuario_perfil (id_usuario, id_perfil) VALUES (7, 2);

-- ============================================================
-- Pessoas, pacientes, médicos e consultas
--
-- O id de "pessoa" é gerado pelo autoincremento (1, 2, 3, ...).
-- As tabelas filhas (JOINED) reutilizam o mesmo id recém-gerado.
-- ============================================================

-- Paciente 1: Maria Silva (Pessoa -> PessoaFisica -> Paciente) => id 1
INSERT INTO pessoa (email, telefone) VALUES ('maria@email.com', '(63) 99911-2233');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (1, 'Maria Silva', '123.456.789-00');
INSERT INTO paciente (id_pessoa_fisica) VALUES (1);

-- Paciente 2: João Souza (Pessoa -> PessoaFisica -> Paciente) => id 2
INSERT INTO pessoa (email, telefone) VALUES ('joao@email.com', '(63) 99822-3344');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (2, 'João Souza', '987.654.321-00');
INSERT INTO paciente (id_pessoa_fisica) VALUES (2);

-- Medico 1: Dr. Carlos Lima (Pessoa -> PessoaFisica -> Medico) => id 3
INSERT INTO pessoa (email, telefone) VALUES ('carlos@email.com', '(63) 99711-5566');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (3, 'Dr. Carlos Lima', '111.222.333-44');
INSERT INTO medico (id_pessoa_fisica, crm, valor_consulta) VALUES (3, 'CRM-12345', 250.00);

-- Medico 2: Dra. Ana Costa (Pessoa -> PessoaFisica -> Medico) => id 4
INSERT INTO pessoa (email, telefone) VALUES ('ana@email.com', '(63) 99622-7788');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (4, 'Dra. Ana Costa', '555.666.777-88');
INSERT INTO medico (id_pessoa_fisica, crm, valor_consulta) VALUES (4, 'CRM-54321', 350.00);

-- PessoaJuridica: empresa que testa o outro ramo da herança (Pessoa -> PessoaJuridica) => id 5
INSERT INTO pessoa (email, telefone) VALUES ('contato@clinicasaude.com', '(63) 3211-0000');
INSERT INTO pessoa_juridica (id_pessoa, razao_social, cnpj) VALUES (5, 'Clínica Saúde LTDA', '12.345.678/0001-90');

-- Associa cada pessoa de demonstração à sua conta de acesso.
UPDATE pessoa SET id_usuario = 2 WHERE id = 1;
UPDATE pessoa SET id_usuario = 3 WHERE id = 2;
UPDATE pessoa SET id_usuario = 4 WHERE id = 3;
UPDATE pessoa SET id_usuario = 5 WHERE id = 4;
UPDATE pessoa SET id_usuario = 6 WHERE id = 5;

-- Consultas (id auto; paciente_id/medico_id apontam para os ids cadastrados acima)
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-11-16 09:00:00', 250.00, 'Retorno de exame', 1, 3);
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-11-17 14:30:00', 350.00, 'Primeira consulta', 1, 4);
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-11-18 10:15:00', 250.00, 'Consulta de rotina', 2, 3);

-- Agenda de horários disponíveis dos médicos (medico_id: 3 = Dr. Carlos, 4 = Dra. Ana)
INSERT INTO agenda (medico_id, data, horario, status, version) VALUES (3, DATE '2026-11-20', TIME '08:00:00', 'DISPONIVEL', 0);
INSERT INTO agenda (medico_id, data, horario, status, version) VALUES (3, DATE '2026-11-20', TIME '09:30:00', 'DISPONIVEL', 0);
INSERT INTO agenda (medico_id, data, horario, status, version) VALUES (4, DATE '2026-11-21', TIME '14:00:00', 'DISPONIVEL', 0);
INSERT INTO agenda (medico_id, data, horario, status, version) VALUES (4, DATE '2026-11-23', TIME '10:30:00', 'DISPONIVEL', 0);
INSERT INTO agenda (medico_id, data, horario, status, version) VALUES (3, DATE '2026-11-22', TIME '11:00:00', 'AGENDADO', 0);
INSERT INTO agenda (medico_id, data, horario, status, version) VALUES (3, DATE '2026-11-25', TIME '15:00:00', 'DISPONIVEL', 0);

-- Agendamento de demonstração aguardando confirmação de presença (Maria Silva com Dr. Carlos)
INSERT INTO agendamento (agenda_id, paciente_id, status, valor, data_agendamento) VALUES (5, 1, 'AGENDADO', 250.00, TIMESTAMP '2026-11-18 10:00:00');

-- Secretários associados aos médicos (secretario=7, clinica=6; Dr. Carlos=3, Dra. Ana=4)
INSERT INTO medico_secretario (medico_id, secretario_id, pode_alterar_agenda, pode_alterar_preco) VALUES (3, 7, TRUE, FALSE);
INSERT INTO medico_secretario (medico_id, secretario_id, pode_alterar_agenda, pode_alterar_preco) VALUES (3, 6, FALSE, FALSE);
INSERT INTO medico_secretario (medico_id, secretario_id, pode_alterar_agenda, pode_alterar_preco) VALUES (4, 7, FALSE, TRUE);