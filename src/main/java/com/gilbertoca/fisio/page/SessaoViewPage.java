package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.service.SessaoService;
import java.util.List;
import java.util.Map;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;

/**
 * Agenda: TODOS os registros de sessão (atual e futura), ordenados pela
 * última inserção — as recém-agendadas aparecem no topo — com o grafo rico
 * (cliente, profissional, modalidade) carregado via Sql2o. Filtro por nome do
 * profissional na tabela, encaminhado ao service
 * (sessaoService.listarAgenda(filters.get("profissional.nome"))).
 */
public class SessaoViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private final SessaoService sessaoService = new SessaoService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Edit", SessaoEditPage.class);
    private PageLink atenderLink = new PageLink("atender", "Atender", SessaoProntuarioEditPage.class);

    public SessaoViewPage() {
        addModel("title", "Sessão View");

        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column inicioCol = new Column("dataHoraInicio");
        inicioCol.setSortable(true);
        table.addColumn(inicioCol);

        table.addColumn(new Column("cliente.nome"));

        Column profissionalCol = new Column("profissional.nome");
        profissionalCol.setFilterBy("profissional.nome");
        table.addColumn(profissionalCol);

        table.addColumn(new Column("modalidade.nome"));
        table.addColumn(new Column("sessaoTipo"));
        table.addColumn(new Column("situacao"));

        Column actionsCol = new Column("actions");
        AbstractLink[] links = new AbstractLink[] { atenderLink, editLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        table.setDataProvider(new DataProvider<Sessao>() {
            @Override
            public List<Sessao> getData() {
                Map<String, String> filters = table.getFilters();
                return sessaoService.listarAgenda(filters.get("profissional.nome"));
            }
        });

        form.add(table);
        addControl(editLink);
        addControl(atenderLink);
        addControl(table);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Agenda do Dia");
    }
}
