-- Cabeçalho do Faturamento (A "Fatura" Comercial)
CREATE TABLE faturamento (
    id SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    tipo_faturamento VARCHAR(20) NOT NULL,
    id_convenio INT REFERENCES convenio(id) ON DELETE SET NULL,
    valor_total_faturado DECIMAL(10,2) NOT NULL,
    data_emissao TIMESTAMP NOT NULL,
    status_faturamento VARCHAR(20) NOT NULL,
    observacoes TEXT
);

-- Itens de Faturamento (Vínculo das sessões clínicas)
CREATE TABLE faturamento_item (
    id SERIAL PRIMARY KEY,
    id_faturamento INT NOT NULL REFERENCES faturamento(id) ON DELETE CASCADE,
    id_sessao INT UNIQUE NOT NULL REFERENCES sessao(id),
    valor_item DECIMAL(10,2) NOT NULL
);

-- Parcelas e Entrada de Fluxo de Caixa Real
CREATE TABLE recebimento_parcela (
    id SERIAL PRIMARY KEY,
    id_faturamento INT NOT NULL REFERENCES faturamento(id) ON DELETE CASCADE,
    numero_parcela INT NOT NULL,
    valor_parcela DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    status_pagamento VARCHAR(20) NOT NULL,
    data_pagamento TIMESTAMP,
    forma_pagamento VARCHAR(30)
);
