-- ==========================================
-- 1. POPULANDO CONVÊNIOS
-- ==========================================
INSERT INTO convenio (nome, cnpj) VALUES
('Unimed Teresina', '12345678000199'),
('Amil Saúde', '98765432000188');

-- ==========================================
-- 2. POPULANDO CLIENTES
-- ==========================================
INSERT INTO cliente (nome, cpf, data_nascimento, telefone, convenio_id, numero_carteirinha, status_clinico) VALUES
('Carlos Eduardo Santos', '11122233344', '1985-04-12', '(86) 99911-2233', 1, '30240011223344', 'ATIVO'),
('Mariana Costa Lima', '55566677788', '1993-09-22', '(86) 98844-5566', NULL, NULL, 'ATIVO'), -- Particular
('Ana Beatriz Rocha', '99900011122', '1978-11-05', '(86) 99455-7788', 2, '90128833445500', 'ATIVO');

-- ==========================================
-- 3. POPULANDO PROFISSIONAIS
-- ==========================================
INSERT INTO profissional (nome, crefito_ou_registro, telefone) VALUES
('Dr. Marcos Vinícius (Fisioterapeuta)', 'CREFITO-12345-F', '(86) 99988-1111'),
('Dra. Amanda Rodrigues (Pilates/RPG)', 'CREFITO-67890-F', '(86) 98122-3344'),
('Juliana Melo (Massoterapeuta)', 'REG-MASS-456', '(86) 98833-2211');

-- ==========================================
-- 4. POPULANDO MODALIDADES / SERVIÇOS
-- ==========================================
INSERT INTO modalidade (nome, duracao_minutos, valor_base) VALUES
('Fisioterapia Ortopédica', 50, 90.00),
('Pilates Clínico', 50, 80.00),
('RPG (Reeducação Postural)', 60, 130.00),
('Massagem Relaxante', 60, 100.00),
('Liberação Miofascial', 45, 120.00);

-- ==========================================
-- 5. POPULANDO SESSÕES (Histórico de Agendamentos)
-- ==========================================
-- Caso 1: Carlos fez uma Avaliação Inicial de RPG e depois iniciou o tratamento de rotina
INSERT INTO sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, tipo_sessao, tipo_pagamento, status_sessao, observacoes_recepcao) VALUES
(1, 2, 3, '2026-06-01 09:00:00', '2026-06-01 10:00:00', 'AVALIACAO_INICIAL', 'CONVENIO', 'REALIZADA', 'Paciente trouxe exames de imagem da coluna.'),
(1, 2, 3, '2026-06-03 09:00:00', '2026-06-03 10:00:00', 'TRATAMENTO_ROTINA', 'CONVENIO', 'AGENDADA', 'Primeira sessão do plano de tratamento.');

-- Caso 2: Mariana agendou uma sessão de Pilates, mas acabou faltando
INSERT INTO sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, tipo_sessao, tipo_pagamento, status_sessao, observacoes_recepcao) VALUES
(2, 2, 2, '2026-06-01 17:00:00', '2026-06-01 17:50:00', 'TRATAMENTO_ROTINA', 'PARTICULAR', 'FALTOU', 'Avisou em cima da hora que ficou presa no trabalho.');

-- Caso 3: Ana Beatriz marcou uma Avaliação de Fisioterapia e uma Liberação Miofascial
INSERT INTO sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, tipo_sessao, tipo_pagamento, status_sessao, observacoes_recepcao) VALUES
(3, 1, 1, '2026-06-02 14:00:00', '2026-06-02 14:50:00', 'AVALIACAO_INICIAL', 'CONVENIO', 'REALIZADA', 'Queixa de dor no joelho pós-corrida.'),
(3, 3, 5, '2026-06-02 15:00:00', '2026-06-02 15:45:00', 'TRATAMENTO_ROTINA', 'PARTICULAR', 'REALIZADA', 'Alívio de pontos de gatilho na panturrilha.');

-- ==========================================
-- 6. POPULANDO PRONTUÁRIOS (Evolução Clínica)
-- ==========================================
-- Prontuário da Avaliação de RPG do Carlos (sessao_id = 1)
INSERT INTO sessao_prontuario (sessao_id, queixa_subjetiva, avaliacao_objetiva, conduta_procedimento, plano_orientacoes) VALUES
(1,
 'Paciente relata dor lombar contínua (nota 6/10) que piora ao passar muito tempo sentado no escritório.',
 'Evidenciada hipercifose dorsal discreta e encurtamento severo de cadeia posterior.',
 'Aplicação de testes de flexibilidade e avaliação postural estática nas fotos de perfil.',
 'Prescritas 10 sessões de RPG. Orientado a realizar pausas ativas no trabalho a cada 2 horas.');

-- Prontuário da Avaliação de Fisioterapia da Ana Beatriz (sessao_id = 4)
INSERT INTO sessao_prontuario (sessao_id, queixa_subjetiva, avaliacao_objetiva, conduta_procedimento, plano_orientacoes) VALUES
(4,
 'Fisgada na lateral do joelho direito ao correr cerca de 5km.',
 'Teste de Ober positivo. Dor à palpação do trato iliotibial.',
 'Avaliação cinemática da marcha e testes de força de glúteo médio.',
 'Iniciar fisioterapia focada em fortalecimento de quadril e analgesia local. Evitar corridas longas nesta semana.');

-- Prontuário da Liberação Miofascial da Ana Beatriz (sessao_id = 5)
INSERT INTO sessao_prontuario (sessao_id, queixa_subjetiva, avaliacao_objetiva, conduta_procedimento, plano_orientacoes) VALUES
(5,
 'Sensação de peso e fadiga muscular nas pernas.',
 'Presença de bandas tensas palpáveis no músculo gastrocnêmio bilateral.',
 'Liberação miofascial manual e com o auxílio de instrumentos (ganchos/rolos).',
 'Orientada automassagem com bola de tênis na sola do pé e alongamentos leves.');

-- Dados fictícios para teste da tela visual
INSERT INTO pagamento_sessao (sessao_id, valor_pago, situacao_financeira, data_pagamento, forma_pagamento) VALUES
(1, 130.00, 'PAGO', '2026-06-01 10:00:00', 'CONVENIO'),     -- Avaliação do Carlos (Paga pelo Convênio)
(2, 130.00, 'PENDENTE', NULL, NULL),                         -- Próxima sessão do Carlos (Pendente)
(3, 80.00, 'PENDENTE', NULL, NULL),                          -- Pilates que a Mariana faltou (Pendente de acerto)
(4, 90.00, 'PAGO', '2026-06-02 14:50:00', 'CONVENIO'),       -- Avaliação da Ana (Paga pelo Convênio)
(5, 120.00, 'PAGO', '2026-06-02 15:45:00', 'PIX');            -- Liberação da Ana (Paga no PIX)

--Faturamento
INSERT INTO faturamento (cliente_id, tipo_faturamento, valor_total_faturado, status_faturamento, observacoes)
VALUES (2, 'PARTICULAR', 100.00, 'CONSOLIDADO', 'Massagem avulsa - Paga na recepção');
-- Retorna o faturamento_id gerado (Ex: ID 50)
INSERT INTO faturamento_item (faturamento_id, sessao_id, valor_item)
VALUES (50, 12, 100.00);
-- (Onde sessao_id 12 é a massagem que acabou de acontecer)
INSERT INTO recebimento_parcela (faturamento_id, numero_parcela, valor_parcela, data_vencimento, status_pagamento, data_pagamento, forma_pagamento)
VALUES (50, 1, 100.00, CURRENT_DATE, 'PAGO', CURRENT_TIMESTAMP, 'PIX');
