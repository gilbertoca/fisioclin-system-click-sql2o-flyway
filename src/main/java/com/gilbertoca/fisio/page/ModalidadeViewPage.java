package com.gilbertoca.fisio.page;

import org.apache.click.control.AbstractLink;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;

import java.util.List;
import java.util.Map;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.service.ModalidadeService;

/**
 * Modalidade view page. Filtro por nome na tabela, encaminhado ao service
 * (clienteService.getAllLikeNome(filters.get("nome")) pattern).
 */
public class ModalidadeViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private final ModalidadeService modalidadeService = new ModalidadeService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Edit", ModalidadeEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Delete", this, "onDelete");

    public ModalidadeViewPage() {
        addModel("title", "Modalidade View");

        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        table.addColumn(new Column("duracaoMinutos"));
        table.addColumn(new Column("valorBase"));
        table.addColumn(new Column("capacidadeSimultanea"));

        Column actionsCol = new Column("actions");
        AbstractLink[] links = new AbstractLink[] { editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        table.setDataProvider(new DataProvider<Modalidade>() {
            @Override
            public List<Modalidade> getData() {
                Map<String, String> filters = table.getFilters();
                return modalidadeService.getAllLikeNome(filters.get("nome"));
            }
        });

        form.add(table);
        addControl(editLink);
        addControl(deleteLink);
        addControl(table);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Modalidades View");
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        Integer id = deleteLink.getValueInteger();
        if (id != null) {
            modalidadeService.delete(id.intValue());
        }
        setRedirect(ModalidadeViewPage.class);
        return false;
    }
}
