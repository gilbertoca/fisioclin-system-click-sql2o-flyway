SELECT
    s.sessao_id AS "Código",
    TO_CHAR(s.data_hora_inicio, 'DD/MM/YYYY HH24:MI') AS "Data/Hora",
    m.nome AS "Modalidade",
    s.tipo_sessao AS "Tipo",

    -- Status Operacional (Agendada, Realizada, Faltou/Adiada)
    s.status_sessao AS "Status Operacional",

    -- Situação Financeira Cruzada
    COALESCE(p.situacao_financeira, 'NÃO LANÇADO') AS "Situação Financeira",
    s.tipo_pagamento AS "Origem",

    -- Indicador se o profissional já preencheu o prontuário clínico
    CASE WHEN pr.prontuario_id IS NOT NULL THEN 'Preenchido' ELSE 'Pendente' END AS "Prontuário"

FROM sessao s
JOIN modalidade m ON s.modalidade_id = m.modalidade_id
LEFT JOIN pagamento_sessao p ON s.sessao_id = p.sessao_id
LEFT JOIN sessao_prontuario pr ON s.sessao_id = pr.sessao_id
WHERE s.cliente_id = 1 -- Filtro dinâmico do Cliente Selecionado na sua tela Click
ORDER BY s.data_hora_inicio DESC;
