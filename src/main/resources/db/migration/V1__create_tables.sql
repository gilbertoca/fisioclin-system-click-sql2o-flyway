-- Cadastro de Planos de Saúde / Convênios
CREATE TABLE convenio (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cnpj VARCHAR(14) UNIQUE
);

-- Cadastro de Fisioterapeutas / Terapeutas / Instrutores
CREATE TABLE profissional (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    crefito_ou_registro VARCHAR(20) UNIQUE NOT NULL,
    telefone VARCHAR(15) NOT NULL
);

-- Tabela Única de Modalidades / Serviços Prestados
CREATE TABLE modalidade (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    duracao_minutos INT NOT NULL DEFAULT 50,
    valor_base DECIMAL(10,2) NOT NULL
);

-- Cadastro Unificado de Clientes/Pacientes
CREATE TABLE cliente (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(15) NOT NULL,
    id_convenio INT REFERENCES convenio(id) ON DELETE SET NULL,
    numero_carteirinha VARCHAR(50),
    status_clinico VARCHAR(20) DEFAULT 'ATIVO'
);

-- Central Única de Sessões (Agenda e Atendimento)
CREATE TABLE sessao (
    id SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    id_profissional INT NOT NULL REFERENCES profissional(id),
    id_modalidade INT NOT NULL REFERENCES modalidade(id),
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    tipo_sessao VARCHAR(25) NOT NULL,
    tipo_pagamento VARCHAR(20) NOT NULL,
    status_sessao VARCHAR(20) NOT NULL DEFAULT 'AGENDADA',
    observacoes_recepcao TEXT
);
