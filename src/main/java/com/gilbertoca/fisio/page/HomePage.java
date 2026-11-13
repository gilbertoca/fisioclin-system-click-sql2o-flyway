package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.service.SessaoService;
import java.util.List;
import org.apache.click.control.Column;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;

/**
 * Cockpit da home: dois quadros com os últimos registros de cada fluxo.
 * A navegação fica na barra de menu (que aponta para as listagens de ambos
 * os papéis) — a home não duplica links:
 * <ul>
 * <li><b>Agendamento</b> (recepção): fila de sessões ainda não atendidas
 * (AGENDADA/CONFIRMADA), as recém-criadas primeiro → Agenda (SessaoViewPage).</li>
 * <li><b>Atendimento</b> (profissional): últimos atendimentos com registro
 * iniciado — EM_ANDAMENTO em curso e REALIZADAS recentes → Meus Atendimentos
 * (SessaoProntuarioViewPage).</li>
 * </ul>
 *
 * Tables enxutas: sem filtro e sem ordenação (nada a encaminhar ao service);
 * o recorte de linhas é LIMIT na query, então nenhuma paginação acontece
 * (linhas &lt; rowsPerPage padrão do Table). Formulário não existe aqui —
 * quadros são leitura pura.
 */
public class HomePage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    /** Registros exibidos em cada quadro. */
    private static final int LIMITE_QUADRO = 5;

    private final Table tabelaFila = new Table("tabelaFila");
    private final Table tabelaAtendimento = new Table("tabelaAtendimento");

    private final SessaoService sessaoService = new SessaoService();

    public HomePage() {
        // Quadro Agendamento: a fila da recepção
        tabelaFila.setShowBanner(false);
        tabelaFila.setSortable(false);
        tabelaFila.addColumn(new Column("dataHoraInicio"));
        tabelaFila.addColumn(new Column("cliente.nome"));
        tabelaFila.addColumn(new Column("profissional.nome"));
        tabelaFila.addColumn(new Column("situacao"));
        tabelaFila.setDataProvider(new DataProvider<Sessao>() {
            @Override
            public List<Sessao> getData() {
                return sessaoService.getUltimasNaFila(LIMITE_QUADRO);
            }
        });

        // Quadro Atendimento: a sala
        tabelaAtendimento.setShowBanner(false);
        tabelaAtendimento.setSortable(false);
        tabelaAtendimento.addColumn(new Column("dataHoraInicio"));
        tabelaAtendimento.addColumn(new Column("cliente.nome"));
        tabelaAtendimento.addColumn(new Column("profissional.nome"));
        tabelaAtendimento.addColumn(new Column("situacao"));
        tabelaAtendimento.setDataProvider(new DataProvider<Sessao>() {
            @Override
            public List<Sessao> getData() {
                return sessaoService.getUltimosAtendimentos(LIMITE_QUADRO);
            }
        });

        addControl(tabelaFila);
        addControl(tabelaAtendimento);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Cockpit");
    }
}
