package org.apache.click.showcase.fisio.page;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.service.ClienteService;

public class ClienteViewPage extends LayoutPage {
    private static final long serialVersionUID = 1L;

    // Grid Components
    protected Table table = new Table("table");
    protected PageLink linkNew = new PageLink("linkNew", "Cadastrar Paciente", ClienteEditPage.class);
    
    // Inline Row Action Link
    protected ActionLink linkDelete = new ActionLink("linkDelete", "Excluir", this, "onDeleteClick");

    private ClienteService clienteService;

    public ClienteViewPage() {
       
        table.addColumn(new Column("nome", "Nome"));
        table.addColumn(new Column("cpf", "CPF"));
        table.addColumn(new Column("telefone", "Telefone"));
        
        // Relationship navigation decorator
        Column colConvenio = new Column("convenio", "Convênio");
        colConvenio.setDecorator((row, context) -> {
            Cliente c = (Cliente) row;
            return c.getConvenio() != null ? c.getConvenio().getNome() : "Particular 🟢";
        });
        table.addColumn(colConvenio);
        table.addColumn(new Column("statusClinico", "Status"));

        // Setup Inline Action Column for Edit/Delete routing
        Column colActions = new Column("acoes", "Ações");
        colActions.setDecorator((row, context) -> {
            Cliente cliente = (Cliente) row;
            String idStr = cliente.getId().toString();
            
            // 1. Generate clean edit hyperlink targeting ClienteEditPage
            String editUrl = context.getPagePath(ClienteEditPage.class) + "?id=" + idStr;
            
            // 2. Configure the inline row delete action token parameters
            linkDelete.setValue(idStr);
            String deleteHtml = linkDelete.toString();
            
            return "<a href='" + editUrl + "' class='pure-button' style='padding: 0.3em 0.8em; margin-right: 0.5em; background: #f39c12; color: #fff;'>Editar</a>" +
                   "<span onclick=\"return confirm('Deseja realmente excluir este paciente?')\">" + deleteHtml + "</span>";
        });
        table.addColumn(colActions);

        addControl(table);
        addControl(linkNew);
        addControl(linkDelete);
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    public boolean onDeleteClick() {
        Integer targetId = linkDelete.getValueInteger();
        if (targetId != null && clienteService != null) {
            try {
                clienteService.delete(targetId);
            } catch (Exception ex) {
                // Flash alert error messages can be bound to context here
            }
        }
        return true; // Reloads current directory view state instantly
    }

    @Override
    public void onRender() {
        super.onRender();
        if (clienteService != null) {
            table.setRowList(clienteService.getAll());
        }
    }
}
