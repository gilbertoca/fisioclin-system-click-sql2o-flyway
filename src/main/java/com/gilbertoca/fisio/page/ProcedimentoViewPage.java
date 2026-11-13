package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.service.ProcedimentoService;
import java.util.List;
import java.util.Map;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;

/**
 * Catálogo de procedimentos: listagem com filtro por nome, encaminhado ao
 * service (procedimentoService.getAllLikeNome(filters.get("nome")) — padrão
 * das listagens). Delete com guard: procedimento usado em roteiro clínico é
 * histórico do atendimento e não sai do catálogo — o service bloqueia
 * (IllegalStateException) e a página avisa em $mensagem, sem error page.
 */
public class ProcedimentoViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private final ProcedimentoService procedimentoService = new ProcedimentoService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Edit", ProcedimentoEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Delete", this, "onDelete");

    public ProcedimentoViewPage() {
        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        table.addColumn(new Column("codigoTuss"));
        table.addColumn(new Column("modalidade.nome"));
        table.addColumn(new Column("situacao"));

        Column actionsCol = new Column("actions");
        AbstractLink[] links = new AbstractLink[] { editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        table.setDataProvider(new DataProvider<Procedimento>() {
            @Override
            public List<Procedimento> getData() {
                Map<String, String> filters = table.getFilters();
                return procedimentoService.getAllLikeNome(filters.get("nome"));
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
        addModel("title", "Procedimentos");
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        Integer id = deleteLink.getValueInteger();
        if (id == null) {
            return false;
        }
        try {
            procedimentoService.delete(id);
            setRedirect(ProcedimentoViewPage.class);
        } catch (IllegalStateException ex) {
            // Procedimento já usado em roteiros: histórico do atendimento não
            // é apagável — avisa em $mensagem em vez de estourar a error page.
            addModel("mensagem", ex.getMessage());
        }
        return false;
    }
}
