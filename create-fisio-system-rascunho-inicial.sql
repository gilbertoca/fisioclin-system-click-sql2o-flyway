-- 1. Cadastro de Convênios
CREATE TABLE convenio (
    id_convenio SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cnpj VARCHAR(14) UNIQUE
);

-- 2. Cadastro Unificado de Clientes/Pacientes
CREATE TABLE cliente (
    id_cliente SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(15) NOT NULL,
    id_convenio INT REFERENCES convenio(id_convenio),
    numero_carteirinha VARCHAR(50),
    status_clinico VARCHAR(20) DEFAULT 'ATIVO' CHECK (status_clinico IN ('ATIVO', 'INATIVO'))
);

-- 3. Cadastro de Fisioterapeutas / Terapeutas
CREATE TABLE profissional (
    id_profissional SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    crefito_ou_registro VARCHAR(20) UNIQUE NOT NULL, -- Suporta CREFITO ou registros de massoterapeutas
    telefone VARCHAR(15) NOT NULL
);

-- 4. Tabela Única de Modalidades / Serviços
CREATE TABLE modalidade (
    id_modalidade SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL, -- 'Fisioterapia', 'Pilates', 'RPG', 'Massagem', 'Liberação'
    duracao_minutos INT NOT NULL DEFAULT 50,
    valor_base DECIMAL(10,2) NOT NULL
);

-- 5. Central Única de Sessões (Agenda e Atendimento)
CREATE TABLE sessao (
    id_sessao SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL REFERENCES cliente(id_cliente),
    id_profissional INT NOT NULL REFERENCES profissional(id_profissional),
    id_modalidade INT NOT NULL REFERENCES modalidade(id_modalidade),

    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,

    -- Define a finalidade do atendimento
    tipo_sessao VARCHAR(25) NOT NULL
        CHECK (tipo_sessao IN ('AVALIACAO_INICIAL', 'TRATAMENTO_ROTINA', 'REAVALIACAO')),

    tipo_pagamento VARCHAR(20) NOT NULL
        CHECK (tipo_pagamento IN ('PARTICULAR', 'CONVENIO')),

    status_sessao VARCHAR(20) NOT NULL DEFAULT 'AGENDADA'
        CHECK (status_sessao IN ('AGENDADA', 'CONFIRMADA', 'REALIZADA', 'FALTOU', 'CANCELADA')),

    observacoes_recepcao TEXT
);

-- 6. Prontuário / Evolução / Registro Clínico da Sessão
CREATE TABLE prontuario_sessao (
    id_prontuario SERIAL PRIMARY KEY,
    id_sessao INT UNIQUE NOT NULL REFERENCES sessao(id_sessao) ON DELETE CASCADE,
    data_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Notas clínicas flexíveis que servem tanto para Avaliação quanto para Evolução diária
    queixa_subjetiva TEXT,          -- O que o cliente relatou (ex: "Dor 7 na escala visual")
    avaliacao_objetiva TEXT,         -- Testes físicos, palpação, postura (crucial se for tipo AVALIACAO)
    conduta_procedimento TEXT,      -- O que foi feito (ex: "Exercícios de core no Cadillac", "Liberação de trapézio")
    plano_orientacoes TEXT          -- Próximos passos ou recomendações para casa
);

-- Adicione esta tabela para controlar a situação financeira de cada sessão
CREATE TABLE pagamento_sessao (
    id_pagamento SERIAL PRIMARY KEY,
    id_sessao INT UNIQUE NOT NULL REFERENCES sessao(id_sessao) ON DELETE CASCADE,
    valor_pago DECIMAL(10,2) NOT NULL,
    situacao_financeira VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (situacao_financeira IN ('PENDENTE', 'PAGO', 'REEMBOLSADO', 'ISENTO')),
    data_pagamento TIMESTAMP,
    forma_pagamento VARCHAR(30) CHECK (forma_pagamento IN ('DINHEIRO', 'CARTAO', 'PIX', 'CONVENIO'))
);

-- 1. Cabeçalho do Faturamento (A "Fatura" ou "Conta" do Cliente/Convênio)
CREATE TABLE faturamento (
    id_faturamento SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL REFERENCES cliente(id_cliente),

    -- Identifica se a fatura é para o próprio Paciente ou se será cobrada do Convênio
    tipo_faturamento VARCHAR(20) NOT NULL CHECK (tipo_faturamento IN ('PARTICULAR', 'CONVENIO')),
    id_convenio INT, -- Preenchido apenas se tipo_faturamento = 'CONVENIO'

    valor_total_faturado DECIMAL(10,2) NOT NULL,
    data_emissao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_faturamento VARCHAR(20) NOT NULL DEFAULT 'ABERTO'
        CHECK (status_faturamento IN ('ABERTO', 'CONSOLIDADO', 'CANCELADO')),
    observacoes TEXT
);

-- 2. Itens do Faturamento (Vincula as sessões que estão sendo cobradas nesta fatura)
CREATE TABLE faturamento_item (
    id_faturamento_item SERIAL PRIMARY KEY,
    id_faturamento INT NOT NULL REFERENCES faturamento(id_faturamento) ON DELETE CASCADE,
    id_sessao INT UNIQUE NOT NULL REFERENCES sessao(id_sessao), -- Garante que uma sessão só seja faturada uma vez
    valor_item DECIMAL(10,2) NOT NULL
);

-- 3. Fluxo de Caixa / Parcelas (Os recebimentos reais associados ao faturamento)
CREATE TABLE recebimento_parcela (
    id_parcela SERIAL PRIMARY KEY,
    id_faturamento INT NOT NULL REFERENCES faturamento(id_faturamento) ON DELETE CASCADE,
    numero_parcela INT NOT NULL DEFAULT 1,
    valor_parcela DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,

    -- Controle da entrada do dinheiro
    status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status_pagamento IN ('PENDENTE', 'PAGO', 'GLOSADO_CONVENIO', 'ATRASADO')),
    data_pagamento TIMESTAMP,
    forma_pagamento VARCHAR(30)
        CHECK (forma_pagamento IN ('DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX', 'TRANSFERENCIA', 'FATURA_CONVENIO'))
);
