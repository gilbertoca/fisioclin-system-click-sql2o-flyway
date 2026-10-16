package com.gilbertoca.fisio.page;

import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;

import java.util.List;
import org.apache.click.control.ActionLink;
import org.apache.click.control.PageLink;
import org.apache.click.control.TablePaginator;
import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.service.ClienteService;
import java.util.Map;

/**
 * Cliente view page.
 *
 * Contains a filter form, a Table with pagination/sorting, and an
 * actions column rendered by LinkDecorator. The delete action is
 * confirmed via a Bulma modal; the real deletion happens in
 * onDelete() through the ActionLink.
 *
 * IMPORTANT: The ActionLink and PageLink used inside the LinkDecorator
 * are private fields, so they must be registered with the page's control
 * tree via addControl(...). Without that, Click cannot dispatch
 * ?actionLink=delete to onDelete().
 */
public class ClienteViewPage extends BorderPage {

    private final ClienteService clienteService = new ClienteService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Edit", ClienteEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Delete", this, "onDelete");

    public ClienteViewPage() {
        addModel("title", "Cliente View");
        
        form.setNoLayout(true);
        // Bulma's native table classes. Do NOT use Table.CLASS_ITS, which
        // is Click's own non-Bulma class nome.
        //table.setClass("table");
        //table.setPageSize(5);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        Column emailCol = new Column("email");
        emailCol.setSortable(true);
        table.addColumn(emailCol);

        table.addColumn(new Column("telefone"));

        Column actionsCol = new Column("actions");
        //actionsCol.setTitle("Actions");
        AbstractLink[] links = new AbstractLink[] { editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        // The delete link opens the Bulma modal. The href Click rendered
        // is preserved on the link and copied to the modal's confirm
        // anchor at click time.
        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        table.setDataProvider(new DataProvider<Cliente>() {
            @Override
            public List<Cliente> getData() {
                Map<String, String> filters = table.getFilters(); // Returns {"nome" -> "John"}
                System.out.println("table.getFilters()" + filters);
                List<Cliente> all = clienteService.getAllLikeNome(filters.get("nome"));
                return all;
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
        System.out.println("onInit()");
        addModel("title", "Clientes View");
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        System.out.println("onDelete()");
        Integer id = deleteLink.getValueInteger();
        if (id != null) {
            clienteService.delete(id.intValue());
        }
        setRedirect(ClienteViewPage.class);
        return false;
    }
}
