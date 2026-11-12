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
import com.gilbertoca.fisio.model.Convenio;
import com.gilbertoca.fisio.service.ConvenioService;

/**
 * Convênio view page. Filtro por nome na tabela, encaminhado ao service
 * (clienteService.getAllLikeNome(filters.get("nome")) pattern).
 */
public class ConvenioViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private final ConvenioService convenioService = new ConvenioService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Edit", ConvenioEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Delete", this, "onDelete");

    public ConvenioViewPage() {
        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        table.addColumn(new Column("cnpj"));

        Column actionsCol = new Column("actions");
        AbstractLink[] links = new AbstractLink[] { editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        table.setDataProvider(new DataProvider<Convenio>() {
            @Override
            public List<Convenio> getData() {
                Map<String, String> filters = table.getFilters();
                return convenioService.getAllLikeNome(filters.get("nome"));
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
        addModel("title", "Convênios View");
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        Integer id = deleteLink.getValueInteger();
        if (id != null) {
            convenioService.delete(id.intValue());
        }
        setRedirect(ConvenioViewPage.class);
        return false;
    }
}
