-- Paciente 1: Maria Silva (Pessoa -> PessoaFisica -> Paciente)
INSERT INTO pessoa (id, email, telefone) VALUES (1, 'maria@email.com', '(63) 99911-2233');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (1, 'Maria Silva', '123.456.789-00');
INSERT INTO paciente (id_pessoa_fisica) VALUES (1);

-- Paciente 2: João Souza (Pessoa -> PessoaFisica -> Paciente)
INSERT INTO pessoa (id, email, telefone) VALUES (2, 'joao@email.com', '(63) 99822-3344');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (2, 'João Souza', '987.654.321-00');
INSERT INTO paciente (id_pessoa_fisica) VALUES (2);

-- Medico 1: Dr. Carlos Lima (Pessoa -> PessoaFisica -> Medico)
INSERT INTO pessoa (id, email, telefone) VALUES (3, 'carlos@email.com', '(63) 99711-5566');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (3, 'Dr. Carlos Lima', '111.222.333-44');
INSERT INTO medico (id_pessoa_fisica, crm) VALUES (3, 'CRM-12345');

-- Medico 2: Dra. Ana Costa (Pessoa -> PessoaFisica -> Medico)
INSERT INTO pessoa (id, email, telefone) VALUES (4, 'ana@email.com', '(63) 99622-7788');
INSERT INTO pessoa_fisica (id_pessoa, nome, cpf) VALUES (4, 'Dra. Ana Costa', '555.666.777-88');
INSERT INTO medico (id_pessoa_fisica, crm) VALUES (4, 'CRM-54321');

-- PessoaJuridica: empresa que testa o outro ramo da herança (Pessoa -> PessoaJuridica)
INSERT INTO pessoa (id, email, telefone) VALUES (5, 'contato@clinicasaude.com', '(63) 3211-0000');
INSERT INTO pessoa_juridica (id_pessoa, razao_social, cnpj) VALUES (5, 'Clínica Saúde LTDA', '12.345.678/0001-90');

-- Consultas (id auto; paciente_id/medico_id apontam para os ids cadastrados acima)
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-09-01 09:00:00', 250.00, 'Retorno de exame', 1, 3);
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-09-02 14:30:00', 350.00, 'Primeira consulta', 1, 4);
insert into consulta (data, valor, observacao, paciente_id, medico_id) values (TIMESTAMP '2026-09-03 10:15:00', 150.00, 'Consulta de rotina', 2, 4);
