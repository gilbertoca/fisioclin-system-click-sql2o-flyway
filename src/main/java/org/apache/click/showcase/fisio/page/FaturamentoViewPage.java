package org.apache.click.showcase.fisio.page;

import org.apache.click.control.Column;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.showcase.fisio.model.Faturamento;
import org.apache.click.showcase.fisio.service.FaturamentoService;

import java.time.format.DateTimeFormatter;

public class FaturamentoViewPage extends LayoutPage {
    private static final long serialVersionUID = 1L;

    protected Table table = new Table("table");
    protected PageLink linkNew = new PageLink("linkNew", "Emitir Novo Faturamento", FaturamentoEditPage.class);

    private FaturamentoService faturamentoService;

    public FaturamentoViewPage() {
        this.table.setWidth("100%");

        // Column 1: Format LocalDateTime explicitly for the view
        Column colData = new Column("dataEmissao", "Data de Emissão");
        colData.setDecorator((row, context) -> {
            Faturamento f = (Faturamento) row;
            return f.getDataEmissao() != null ? f.getDataEmissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        });
        table.addColumn(colData);

        // Column 2: Resolve Client descriptor navigating the rich object graph
        Column colCliente = new Column("clienteId", "Paciente");
        table.addColumn(colCliente);

        table.addColumn(new Column("tipoFaturamento", "Tipo"));
        
        // Column 4: Total Monitored Invoice Currency
        Column colValor = new Column("valorTotalFaturado", "Valor Total");
        colValor.setDecorator((row, context) -> {
            Faturamento f = (Faturamento) row;
            return f.getValorTotalFaturado() != null ? "R$ " + f.getValorTotalFaturado().toString() : "R$ 0.00";
        });
        table.addColumn(colValor);
        
        table.addColumn(new Column("statusFaturamento", "Situação"));

        // Column 6: Inline View Action Link routing
        Column colAcoes = new Column("acoes", "Ações");
        colAcoes.setDecorator((row, context) -> {
            Faturamento f = (Faturamento) row;
            String editUrl = context.getPagePath(FaturamentoEditPage.class) + "?id=" + f.getId();
            return "<a href='" + editUrl + "'>Ver Detalhes / Parcelas</a>";
        });
        table.addColumn(colAcoes);

        addControl(table);
        addControl(linkNew);
    }

    @Override
    public void onRender() {
        super.onRender();
        if (faturamentoService != null) {
            table.setRowList(faturamentoService.getAll());
        }
    }
}
