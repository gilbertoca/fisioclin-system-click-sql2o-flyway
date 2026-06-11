-- 1. Cadastro de Convênios
CREATE TABLE convenio (
    convenio_id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cnpj VARCHAR(14) UNIQUE
);

-- 2. Cadastro Unificado de Clientes/Pacientes
CREATE TABLE cliente (
    cliente_id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(15) NOT NULL,
    convenio_id INT REFERENCES convenio(convenio_id),
    numero_carteirinha VARCHAR(50),
    status_clinico VARCHAR(20) DEFAULT 'ATIVO' CHECK (status_clinico IN ('ATIVO', 'INATIVO'))
);

-- 3. Cadastro de Fisioterapeutas / Terapeutas
CREATE TABLE profissional (
    profissional_id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    crefito_ou_registro VARCHAR(20) UNIQUE NOT NULL, -- Suporta CREFITO ou registros de massoterapeutas
    telefone VARCHAR(15) NOT NULL
);

-- 4. Tabela Única de Modalidades / Serviços
CREATE TABLE modalidade (
    modalidade_id SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL, -- 'Fisioterapia', 'Pilates', 'RPG', 'Massagem', 'Liberação'
    duracao_minutos INT NOT NULL DEFAULT 50,
    valor_base DECIMAL(10,2) NOT NULL
);

-- 5. Central Única de Sessões (Agenda e Atendimento)
CREATE TABLE sessao (
    sessao_id SERIAL PRIMARY KEY,
    cliente_id INT NOT NULL REFERENCES cliente(cliente_id),
    profissional_id INT NOT NULL REFERENCES profissional(profissional_id),
    modalidade_id INT NOT NULL REFERENCES modalidade(modalidade_id),

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
CREATE TABLE sessao_prontuario (
    prontuario_id SERIAL PRIMARY KEY,
    sessao_id INT UNIQUE NOT NULL REFERENCES sessao(sessao_id) ON DELETE CASCADE,
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
    sessao_id INT UNIQUE NOT NULL REFERENCES sessao(sessao_id) ON DELETE CASCADE,
    valor_pago DECIMAL(10,2) NOT NULL,
    situacao_financeira VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (situacao_financeira IN ('PENDENTE', 'PAGO', 'REEMBOLSADO', 'ISENTO')),
    data_pagamento TIMESTAMP,
    forma_pagamento VARCHAR(30) CHECK (forma_pagamento IN ('DINHEIRO', 'CARTAO', 'PIX', 'CONVENIO'))
);

-- 1. Cabeçalho do Faturamento (A "Fatura" ou "Conta" do Cliente/Convênio)
CREATE TABLE faturamento (
    faturamento_id SERIAL PRIMARY KEY,
    cliente_id INT NOT NULL REFERENCES cliente(cliente_id),

    -- Identifica se a fatura é para o próprio Paciente ou se será cobrada do Convênio
    tipo_faturamento VARCHAR(20) NOT NULL CHECK (tipo_faturamento IN ('PARTICULAR', 'CONVENIO')),
    convenio_id INT, -- Preenchido apenas se tipo_faturamento = 'CONVENIO'

    valor_total_faturado DECIMAL(10,2) NOT NULL,
    data_emissao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_faturamento VARCHAR(20) NOT NULL DEFAULT 'ABERTO'
        CHECK (status_faturamento IN ('ABERTO', 'CONSOLIDADO', 'CANCELADO')),
    observacoes TEXT
);

-- 2. Itens do Faturamento (Vincula as sessões que estão sendo cobradas nesta fatura)
CREATE TABLE faturamento_item (
    faturamento_id_item SERIAL PRIMARY KEY,
    faturamento_id INT NOT NULL REFERENCES faturamento(faturamento_id) ON DELETE CASCADE,
    sessao_id INT UNIQUE NOT NULL REFERENCES sessao(sessao_id), -- Garante que uma sessão só seja faturada uma vez
    valor_item DECIMAL(10,2) NOT NULL
);

-- 3. Fluxo de Caixa / Parcelas (Os recebimentos reais associados ao faturamento)
CREATE TABLE recebimento_parcela (
    parcela_id SERIAL PRIMARY KEY,
    faturamento_id INT NOT NULL REFERENCES faturamento(faturamento_id) ON DELETE CASCADE,
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
