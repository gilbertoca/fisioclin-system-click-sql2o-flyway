SELECT
    s.id_sessao AS "Código",
    TO_CHAR(s.data_hora_inicio, 'DD/MM/YYYY HH24:MI') AS "Data/Hora",
    m.nome AS "Modalidade",
    s.tipo_sessao AS "Tipo",

    -- Status Operacional (Agendada, Realizada, Faltou/Adiada)
    s.status_sessao AS "Status Operacional",

    -- Situação Financeira Cruzada
    COALESCE(p.situacao_financeira, 'NÃO LANÇADO') AS "Situação Financeira",
    s.tipo_pagamento AS "Origem",

    -- Indicador se o profissional já preencheu o prontuário clínico
    CASE WHEN pr.id_prontuario IS NOT NULL THEN 'Preenchido' ELSE 'Pendente' END AS "Prontuário"

FROM sessao s
JOIN modalidade m ON s.id_modalidade = m.id_modalidade
LEFT JOIN pagamento_sessao p ON s.id_sessao = p.id_sessao
LEFT JOIN prontuario_sessao pr ON s.id_sessao = pr.id_sessao
WHERE s.id_cliente = 1 -- Filtro dinâmico do Cliente Selecionado na sua tela Click
ORDER BY s.data_hora_inicio DESC;
