package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Faturamento;
import org.apache.click.showcase.fisio.model.RecebimentoParcela;
import org.apache.click.showcase.fisio.model.Sessao;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.math.BigDecimal;
import java.util.List;

public class FisioBillingService {

    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public FisioBillingService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public Integer faturarSessoesParticular(Integer clienteId, List<Sessao> sessoesAReceber, BigDecimal valorSessao, int totalParcelas) {
        if (sessoesAReceber == null || sessoesAReceber.isEmpty()) {
            throw new IllegalArgumentException("Lote de sessões clínico vazio.");
        }

        // 1. Cria a Transação de Domínio e deixa o Objeto Rico processar a matemática interna
        BigDecimal valorTotalFatura = valorSessao.multiply(BigDecimal.valueOf(sessoesAReceber.size()));

        Faturamento faturaDominio = new Faturamento();
        faturaDominio.setclienteId(clienteId);
        faturaDominio.setTipoFaturamento("PARTICULAR");
        faturaDominio.setStatusFaturamento("CONSOLIDADO");
        faturaDominio.setObservacoes("Pacote gerado via modelo de domínio SOM rico.");
        faturaDominio.gerarParcelasParticionadas(valorTotalFatura, totalParcelas); // Encapsulamento em ação!

        // 2. Orquestração da Persistência Atômica no Banco de Dados
        try (Connection conn = sql2o.beginTransaction()) {

            // Salva o Cabeçalho principal
            Integer idFaturamentoGerado = conn.createQuery(queryLoader.get("billing.insertFaturamento"), true)
                    .bind(faturaDominio)
                    .executeUpdate()
                    .getKey(Integer.class);

            // Salva os Itens Vinculados
            String sqlItem = queryLoader.get("faturamento_item.create");
            for (Sessao sessao : sessoesAReceber) {
                conn.createQuery(sqlItem)
                        .addParameter("idFaturamento", idFaturamentoGerado)
                        .addParameter("idSessao", sessao.getId())
                        .addParameter("valorItem", valorSessao)
                        .executeUpdate();
            }

            // Salva as Parcelas extraídas de dentro do Grafo do Objeto Faturamento
            String sqlParcela = queryLoader.get("recebimento_parcela.create");
            for (RecebimentoParcela parcela : faturaDominio.getParcelas()) {

                // Amarra o relacionamento bidirecional SOM no grafo antes de salvar
                faturaDominio.setId(idFaturamentoGerado);
                parcela.setIdFaturamento(idFaturamentoGerado);

                //parcela.setFaturamento(faturaDominio);

                conn.createQuery(sqlParcela)
                        .bind(parcela) // O bind do Sql2o lê perfeitamente caminhos como faturamento.id do objeto!
                        .executeUpdate();
            }

            conn.commit();
            return idFaturamentoGerado;
        }
    }
}
