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

INSERT INTO usuario (usuario, senha) VALUES ('admin', '$2a$10$bNNAqeWsFoLwIRaghWNWrO5vKCq648XRbRMxrlY29pP.hVRvttLEq');
INSERT INTO usuario (usuario, senha) VALUES ('user', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW');
INSERT INTO usuario (usuario, senha) VALUES ('joao', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW');
INSERT INTO usuario (usuario, senha) VALUES ('carlos', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW');
INSERT INTO usuario (usuario, senha) VALUES ('ana', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW');
INSERT INTO usuario (usuario, senha) VALUES ('clinica', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW');
INSERT INTO usuario (usuario, senha) VALUES ('secretario', '$2a$10$dj.LiJmM71c21rsaJzWrfurcSJArX8wY9KYW4smnC1jqi88EZuPrW');

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
INSERT INTO medico (id_pessoa_fisica, crm) VALUES (3, 'CRM-12345');

-- Medico 2: Dra. Ana Costa (Pessoa -> PessoaFisica -> Medico) => id 4
INSERT INTO pessoa (email, telefone) VALUES ('ana@email.com', '(63) 99622-7788');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (4, 'Dra. Ana Costa', '555.666.777-88');
INSERT INTO medico (id_pessoa_fisica, crm) VALUES (4, 'CRM-54321');

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
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-09-01 09:00:00', 250.00, 'Retorno de exame', 1, 3);
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-09-02 14:30:00', 350.00, 'Primeira consulta', 1, 4);
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-09-03 10:15:00', 150.00, 'Consulta de rotina', 2, 4);