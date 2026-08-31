package org.apache.click.showcase.fisio.page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.Table;
import org.apache.click.control.TextField;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.service.ClienteService;

/**
 * Página de Listagem de Clientes.
 */
public class ClienteViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private Form filterForm = new Form("filterForm");
    private Table table = new Table("table");
    private ActionLink editLink = new ActionLink("edit", "Editar", this, "onEditClick");
    private ActionLink deleteLink = new ActionLink("delete", "Excluir", this, "onDeleteClick");

    private ClienteService clienteService = new ClienteService();

    public ClienteViewPage() {
        addModel("title", "Clientes");

        // --- Form de Filtro (layout TABLE para grid horizontal compacto) ---
        addControl(filterForm);
        filterForm.setColumns(2);

        TextField nameFilter = new TextField("nameFilter", "Nome");
        filterForm.add(nameFilter);
        filterForm.add(new Submit("search", "Filtrar", this, "onSearchClick"));
        filterForm.add(new Submit("clear", "Limpar", this, "onClearClick"));

        // --- Tabela ---
        addControl(table);
        addControl(editLink);
        addControl(deleteLink);

        table.setPageSize(5);
        table.setShowBanner(true);
        table.setSortable(true);

        Column colNome = new Column("nome", "Nome");
        colNome.setWidth("180px");
        table.addColumn(colNome);

        Column colCpf = new Column("cpf", "CPF");
        table.addColumn(colCpf);

        Column colDtNascimento = new Column("dtNascimento", "DtNascimento");
        colDtNascimento.setTextAlign("center");
        colDtNascimento.setWidth("80px");
        table.addColumn(colDtNascimento);

        Column colStatus = new Column("status", "Situação");
        colStatus.setTextAlign("right");
        table.addColumn(colStatus);

        Column colAction = new Column("action", "Ações");
        colAction.setSortable(false);
        ActionLink[] links = new ActionLink[]{editLink, deleteLink};
        colAction.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(colAction);

        deleteLink.setAttribute("onclick", "return window.confirm('Confirma exclusão?');");

        table.setDataProvider(new DataProvider<Cliente>() {
            @Override
            public List<Cliente> getData() {
                List<Cliente> result = null;
                String filter = filterForm.getFieldValue("nameFilter");
                if (filter != null && !filter.trim().isEmpty()) {
                    result = clienteService.getAllLikeNome(filter);
                } else {
                    result = clienteService.getAll();
                }
                return result;
            }
        });
    }

    public boolean onSearchClick() {
        return true;
    }

    public boolean onClearClick() {
        filterForm.clearValues();
        return true;
    }

    public boolean onEditClick() {
        Integer id = editLink.getValueInteger();
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        setRedirect(ClienteEditPage.class, params);
        return false;
    }

    public boolean onDeleteClick() {
        Integer id = deleteLink.getValueInteger();
        clienteService.delete(id);
        return true;
    }
}
