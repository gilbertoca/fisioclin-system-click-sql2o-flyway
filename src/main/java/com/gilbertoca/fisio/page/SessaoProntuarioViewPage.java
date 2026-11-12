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
import org.apache.click.util.Bindable;

/**
 * Lista de atendimentos (agenda atual e futura: TODOS os registros de sessão,
 * ordenados pela última inserção — as recém-agendadas no topo). Cada linha é
 * o atendimento de UM paciente; sessões em grupo (ex.: pilates) aparecem como
 * linhas consecutivas no mesmo slot, uma por paciente, cada uma com seu
 * prontuário individual.
 *
 * Prototype: o "profissional logado" é um atributo de sessão
 * (SESSION_PROFISSIONAL_ID). Quando a autorização por papel
 * (admin/gerencia/profissional) for modelada, este atributo passa a ser
 * derivado do papel no login. Filtro por nome do profissional na tabela,
 * encaminhado ao service. O filtro do cliente é feito na sala, na página de
 * atendimento (SessaoProntuarioEditPage).
 */
public class SessaoProntuarioViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    /** Gancho futuro da autorização por papel. Por enquanto é um atributo de sessão livre. */
    public static final String SESSION_PROFISSIONAL_ID = "profissionalLogadoId";

    @Bindable protected Integer id;

    private final Form formTable = new Form("formTable");
    private final Table table = new Table("table");
    private final PageLink atenderLink = new PageLink("atender", "Atender", SessaoProntuarioEditPage.class);
    private final PageLink editLink = new PageLink("edit", "Edit", SessaoEditPage.class);

    private final SessaoService sessaoService = new SessaoService();

    public SessaoProntuarioViewPage() {
        formTable.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column inicioCol = new Column("dataHoraInicio");
        inicioCol.setSortable(true);
        table.addColumn(inicioCol);

        Column profissionalCol = new Column("profissional.nome");
        profissionalCol.setFilterBy("profissional.nome");
        table.addColumn(profissionalCol);

        table.addColumn(new Column("cliente.nome"));
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

        formTable.add(table);
        addControl(atenderLink);
        addControl(editLink);
        addControl(table);
        addControl(formTable);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Atendimentos");
    }
}
