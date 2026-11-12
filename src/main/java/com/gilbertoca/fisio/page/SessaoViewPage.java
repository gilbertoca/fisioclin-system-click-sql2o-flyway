package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.service.SessaoService;
import java.util.List;
import java.util.Map;
import org.apache.click.control.ActionLink;
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
    /** Desfaz o agendamento (erro de entrada da recepção): só sessão AGENDADA. */
    private ActionLink deleteLink = new ActionLink("delete", "Delete", this, "onDelete");

    public SessaoViewPage() {
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
        AbstractLink[] links = new AbstractLink[] { atenderLink, editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        // The delete link opens the Bahunya modal. The href Click rendered
        // is preserved on the link and copied to the modal's confirm
        // anchor at click time.
        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

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
        addControl(deleteLink);
        addControl(table);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Agenda do Dia");
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        Integer id = deleteLink.getValueInteger();
        if (id == null) {
            return false;
        }
        try {
            sessaoService.delete(id);
            setRedirect(SessaoViewPage.class);
        } catch (IllegalStateException ex) {
            // Histórico (EM_ANDAMENTO/REALIZADA/FALTOU/CANCELADA) não é
            // apagável: avisa a recepção em vez de estourar a error page.
            addModel("mensagem", ex.getMessage());
        }
        return false;
    }
}
