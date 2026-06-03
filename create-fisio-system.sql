-- ============================================================================
-- 1. CADASTROS BASE (ENTIDADES INDEPENDENTES)
-- ============================================================================

-- Cadastro de Planos de Saúde / Convênios
CREATE TABLE convenio (
    id_convenio SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cnpj VARCHAR(14) UNIQUE
);

-- Cadastro de Fisioterapeutas / Terapeutas / Instrutores
CREATE TABLE profissional (
    id_profissional SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    crefito_ou_registro VARCHAR(20) UNIQUE NOT NULL,
    telefone VARCHAR(15) NOT NULL
);

-- Tabela Única de Modalidades / Serviços Prestados
CREATE TABLE modalidade (
    id_modalidade SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL, -- Ex: 'Fisioterapia', 'Pilates', 'RPG', 'Massagem', 'Liberação'
    duracao_minutos INT NOT NULL DEFAULT 50,
    valor_base DECIMAL(10,2) NOT NULL
);

-- ============================================================================
-- 2. CADASTROS DEPENDENTES (ENTIDADES COM CHAVES ESTRANGEIRAS)
-- ============================================================================

-- Cadastro Unificado de Clientes/Pacientes
CREATE TABLE cliente (
    id_cliente SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(15) NOT NULL,
    id_convenio INT REFERENCES convenio(id_convenio) ON DELETE SET NULL,
    numero_carteirinha VARCHAR(50),
    status_clinico VARCHAR(20) DEFAULT 'ATIVO' CHECK (status_clinico IN ('ATIVO', 'INATIVO'))
);

-- Central Única de Sessões (Agenda, Atendimento e Fluxo Operacional)
CREATE TABLE sessao (
    id_sessao SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    id_profissional INT NOT NULL REFERENCES profissional(id_profissional),
    id_modalidade INT NOT NULL REFERENCES modalidade(id_modalidade),
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    tipo_sessao VARCHAR(25) NOT NULL
        CHECK (tipo_sessao IN ('AVALIACAO_INICIAL', 'TRATAMENTO_ROTINA', 'REAVALIACAO')),
    tipo_pagamento VARCHAR(20) NOT NULL
        CHECK (tipo_pagamento IN ('PARTICULAR', 'CONVENIO')),
    status_sessao VARCHAR(20) NOT NULL DEFAULT 'AGENDADA'
        CHECK (status_sessao IN ('AGENDADA', 'CONFIRMADA', 'REALIZADA', 'FALTOU', 'CANCELADA')),
    observacoes_recepcao TEXT
);

-- ============================================================================
-- 3. MÓDULO CLÍNICO (EVOLUÇÃO E PRONTUÁRIO)
-- ============================================================================

-- Prontuário / Registro Clínico da Sessão (1:1 com Sessão)
CREATE TABLE prontuario_sessao (
    id_prontuario SERIAL PRIMARY KEY,
    id_sessao INT UNIQUE NOT NULL REFERENCES sessao(id_sessao) ON DELETE CASCADE,
    data_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    queixa_subjetiva TEXT,          -- Relato do cliente (Escala de dor, sintomas)
    avaliacao_objetiva TEXT,         -- Testes físicos e palpação (crucial em AVALIACAO)
    conduta_procedimento TEXT,      -- O que foi executado (Exercícios, manipulações)
    plano_orientacoes TEXT          -- Recomendações para casa ou próximos passos
);

-- ============================================================================
-- 4. MÓDULO FINANCEIRO (BILLING E FLUXO DE CAIXA)
-- ============================================================================

-- Cabeçalho do Faturamento (A Fatura / Conta gerada para o Cliente ou Convênio)
CREATE TABLE faturamento (
    id_faturamento SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    tipo_faturamento VARCHAR(20) NOT NULL CHECK (tipo_faturamento IN ('PARTICULAR', 'CONVENIO')),
    id_convenio INT REFERENCES convenio(id_convenio) ON DELETE SET NULL,
    valor_total_faturado DECIMAL(10,2) NOT NULL,
    data_emissao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_faturamento VARCHAR(20) NOT NULL DEFAULT 'ABERTO'
        CHECK (status_faturamento IN ('ABERTO', 'CONSOLIDADO', 'CANCELADO')),
    observacoes TEXT
);

-- Itens do Faturamento (Vincula quais sessões estão sendo cobradas na fatura)
CREATE TABLE faturamento_item (
    id_faturamento_item SERIAL PRIMARY KEY,
    id_faturamento INT NOT NULL REFERENCES faturamento(id_faturamento) ON DELETE CASCADE,
    id_sessao INT UNIQUE NOT NULL REFERENCES sessao(id_sessao) ON DELETE RESTRICT, -- Impede deletar sessão faturada
    valor_item DECIMAL(10,2) NOT NULL
);

-- Parcelas de Recebimento (Fluxo de Caixa real associado ao faturamento)
CREATE TABLE recebimento_parcela (
    id_parcela SERIAL PRIMARY KEY,
    id_faturamento INT NOT NULL REFERENCES faturamento(id_faturamento) ON DELETE CASCADE,
    numero_parcela INT NOT NULL DEFAULT 1,
    valor_parcela DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status_pagamento IN ('PENDENTE', 'PAGO', 'GLOSADO_CONVENIO', 'ATRASADO')),
    data_pagamento TIMESTAMP,
    forma_pagamento VARCHAR(30)
        CHECK (forma_pagamento IN ('DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX', 'TRANSFERENCIA', 'FATURA_CONVENIO'))
);
