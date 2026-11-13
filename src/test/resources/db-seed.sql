-- ============================================================================
-- FisioClin — Seed de Dados Fictícios Compatível com DDL Final (V1)
-- ============================================================================

-- 1. MODALIDADES
INSERT INTO fisio.modalidade (modalidade_id, nome, duracao_minutos, valor_base, capacidade_simultanea) VALUES
(1, 'Fisioterapia Ortopédica', 50, 150.00, 1),
(2, 'Pilates Clínico',         50, 110.00, 4),
(3, 'RPG',                     50, 160.00, 1),
(4, 'Consulta Ortopédica',     30, 350.00, 1),
(5, 'Liberação Miofascial',  50, 140.00, 1),
(6, 'Massagem Desportiva',   50, 130.00, 1),
(7, 'Drenagem Linfática',    50, 120.00, 1);

-- 2. CATÁLOGO DE PROCEDIMENTOS / RECURSOS
INSERT INTO fisio.procedimento (procedimento_id, modalidade_id, nome, codigo_tuss, procedimento_situacao) VALUES
(1, 1, 'Eletroterapia (TENS / FES)',   '20103212', 'ATIVO'),
(2, 1, 'Ultrassom Terapêutico',        '20103654', 'ATIVO'),
(3, 1, 'Liberação Miofascial Manual',  '20103484', 'ATIVO'),
(4, 1, 'Cinesioterapia Motora',        '20103140', 'ATIVO'),
(5, 2, 'Exercícios no Reformer',       NULL,       'ATIVO'),
(6, 2, 'Exercícios no Cadillac',       NULL,       'ATIVO'),
(7, 2, 'Fortalecimento Solo / Bola',   NULL,       'ATIVO'),
(8, 3, 'Postura Rã no Chão',           '20103522', 'ATIVO'),
(9, 4, 'Infiltração Articular',        '30713137', 'ATIVO'),
(10, 5, 'Liberação Instrumental (IASTM / Crochetagem)', NULL, 'ATIVO'),
(11, 5, 'Ventosaterapia',                               NULL, 'ATIVO'),
(12, 6, 'Massagem Profunda / Deslizamento',             NULL, 'ATIVO');

-- 3. CONVÊNIOS
INSERT INTO fisio.convenio (convenio_id, cnpj, nome) VALUES
(1, '02812468000106', 'Unimed Nacional'),
(2, '92693118000160', 'Bradesco Saúde');

-- 4. PROFISSIONAIS
INSERT INTO fisio.profissional (profissional_id, cpf, nome, dt_nascimento, email, telefone, registro_numero, registro_orgao, login) VALUES
(1, '11122233344', 'Dra. Camila Nogueira',   '1988-05-14', 'camila.fisio@fisioclin.com.br', '63999112233', '145890-F', 'CREFITO-12', 'camila.nogueira'),
(2, '22233344455', 'Dr. Rafael Mendes',     '1985-09-22', 'rafael.pilates@fisioclin.com.br','63999445566', '198432-F', 'CREFITO-12', 'rafael.mendes'),
(3, '33344455566', 'Dr. Henrique Alencar',  '1980-02-10', 'henrique.orto@fisioclin.com.br', '63999778899', '4521-TO',  'CRM-TO',     'henrique.alencar');

-- 5. CLIENTES / PACIENTES
INSERT INTO fisio.cliente (cliente_id, cpf, nome, dt_nascimento, email, telefone, convenio_id, convenio_numero, cliente_situacao) VALUES
(1, '44455566677', 'Mariana Costa Silva',    '1992-07-19', 'mariana.costa@email.com',  '63984123456', NULL, NULL,              'ATIVO'),
(2, '55566677788', 'João Pedro Oliveira',    '1979-11-03', 'joao.oliveira@email.com',  '63984654321', 1,    '0045892145001',   'ATIVO'),
(3, '66677788899', 'Beatriz Santos Rocha',   '1995-03-28', 'beatriz.rocha@email.com',  '63981112233', NULL, NULL,              'ATIVO'),
(4, '77788899900', 'Carlos Eduardo Lima',    '1968-12-15', 'carlos.lima@email.com',    '63981998877', 2,    '998877665544',    'ATIVO');

-- 6. SESSÕES (AGENDA + PRONTUÁRIO UNIFICADO COM NOMES DO SEU DDL)
INSERT INTO fisio.sessao (
    sessao_id, cliente_id, profissional_id, modalidade_id,
    dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao,
    observacoes_recepcao, registro_relato, registro_avaliacao, registro_conduta, registro_orientacoes,
    registro_dt_inicio, registro_dt_fim
) VALUES
-- Sessão 1: Avaliação Particular Realizada
(
    1, 1, 1, 1,
    '2026-11-10 08:00:00', '2026-11-10 08:50:00', 'AVALIACAO', 'PARTICULAR', 'REALIZADA',
    'Primeira vez na clínica, encaminhada pelo ortopedista.',
    'Paciente relata dor lombar irradiando para glúteo direito há 3 semanas (EVA 7/10).',
    'Encurtamento severo de isquiotibiais, teste de Lasègue positivo a 45° à direita.',
    'Realizada analgesia inicial e soltura miofascial em região lombar e piriforme.',
    'Iniciar protocolo de 10 sessões 2x por semana; evitar impacto.',
    '2026-11-10 08:05:00', '2026-11-10 08:52:00'
),
-- Sessão 2: Tratamento Convênio Unimed Realizada
(
    2, 2, 1, 1,
    '2026-11-10 09:00:00', '2026-11-10 09:50:00', 'TRATAMENTO', 'CONVENIO', 'REALIZADA',
    'Guia Unimed autorizada nº 884920.',
    'Refere melhora de 40% na rigidez matinal do ombro direito (EVA 4/10).',
    'ADM de flexão ativa atingiu 145°, sem crepitação dolorosa.',
    'Mobilização articular grau II, exercícios pendulares e fortalecimento de manguito.',
    'Progredir carga elástica na próxima sessão.',
    '2026-11-10 09:00:00', '2026-11-10 09:48:00'
),
-- Sessão 3: Pacote de Pilates Particular (Realizada)
(
    3, 3, 2, 2,
    '2026-11-11 10:00:00', '2026-11-11 10:50:00', 'TRATAMENTO', 'PARTICULAR', 'REALIZADA',
    'Pacote mensal fechado na recepção.',
    'Sem queixas álgicas hoje, boa disposição.',
    'Boa ativação de transverso do abdômen, leve instabilidade pélvica unilateral.',
    'Série de estabilidade de core e mobilidade torácica nos aparelhos.',
    'Manter progressão de molas na próxima aula.',
    '2026-11-11 10:02:00', '2026-11-11 10:50:00'
),
-- Sessão 4: Pacote de Pilates Particular (Agendada futura)
(
    4, 3, 2, 2,
    '2026-11-13 10:00:00', '2026-11-13 10:50:00', 'TRATAMENTO', 'PARTICULAR', 'AGENDADA',
    '2ª sessão do pacote mensal.',
    NULL, NULL, NULL, NULL,
    NULL, NULL
),
-- Sessão 5: Pacote de Pilates Particular (Faltou)
(
    5, 3, 2, 2,
    '2026-11-09 10:00:00', '2026-11-09 10:50:00', 'TRATAMENTO', 'PARTICULAR', 'FALTOU',
    'Paciente avisou em cima da hora; regra do contrato desconta do pacote.',
    NULL, NULL, NULL, NULL,
    NULL, NULL
),
-- Sessão 6: Consulta Médica em Andamento
(
    6, 4, 3, 4,
    '2026-11-13 14:00:00', '2026-11-13 14:30:00', 'AVALIACAO', 'CONVENIO', 'EM_ANDAMENTO',
    'Trouxe ressonância magnética do joelho esquerdo.',
    'Dor medial no joelho esquerdo ao subir escadas há 2 meses.',
    NULL, NULL, NULL,
    '2026-11-13 14:03:00', NULL
);

-- 7. ROTEIRO / PROCEDIMENTOS APLICADOS NAS SESSÕES
INSERT INTO fisio.sessao_procedimento (sessao_id, procedimento_id, observacao) VALUES
(1, 1, 'TENS convencional 20 min em região lombar L4-S1'),
(1, 3, 'Liberação de piriforme e quadrado lombar direito'),
(2, 4, 'Exercícios isométricos e rotação externa com theraband leve'),
(3, 5, 'Footwork e Elephant - 2 molas vermelhas, 3x10'),
(3, 6, 'Roll back e Swan - mola azul, 3x8');

-- 8. FATURAMENTO
INSERT INTO fisio.faturamento (
    faturamento_id, cliente_id, pagamento_origem, convenio_id,
    valor_bruto, valor_desconto, valor_liquido, data_emissao, faturamento_situacao, observacoes
) VALUES
(1, 1, 'PARTICULAR', NULL, 150.00, 0.00,  150.00, '2026-11-10 08:55:00', 'CONSOLIDADO', 'Pagamento avulso na saída da avaliação.'),
(2, 3, 'PARTICULAR', NULL, 330.00, 30.00, 300.00, '2026-11-09 09:30:00', 'CONSOLIDADO', 'Pacote promocional 3 aulas de Pilates (R$ 100/aula).'),
(3, 2, 'CONVENIO',   1,    150.00, 0.00,  150.00, '2026-11-10 18:00:00', 'CONSOLIDADO', 'Lote TISS nº 202611-01 - Unimed.');

-- 9. ITENS DO FATURAMENTO
INSERT INTO fisio.faturamento_item (faturamento_item_id, faturamento_id, sessao_id, valor_item) VALUES
(1, 1, 1, 150.00),
(2, 2, 3, 100.00),
(3, 2, 4, 100.00),
(4, 2, 5, 100.00),
(5, 3, 2, 150.00);

-- 10. PARCELAS DE RECEBIMENTO
INSERT INTO fisio.recebimento_parcela (
    parcela_id, faturamento_id, numero_parcela, valor_parcela, data_vencimento,
    parcela_situacao, data_pagamento, pagamento_meio
) VALUES
(1, 1, 1, 150.00, '2026-11-10', 'PAGO',     '2026-11-10 08:56:00', 'PIX'),
(2, 2, 1, 150.00, '2026-11-09', 'PAGO',     '2026-11-09 09:32:00', 'CARTAO_CREDITO'),
(3, 2, 2, 150.00, '2026-12-09', 'PENDENTE', NULL,                  'CARTAO_CREDITO'),
(4, 3, 1, 150.00, '2026-12-10', 'PENDENTE', NULL,                  'FATURA_CONVENIO');
