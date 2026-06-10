package org.apache.click.showcase.fisio.page;

import org.apache.click.control.*;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Faturamento;
import org.apache.click.showcase.fisio.model.RecebimentoParcela;
import org.apache.click.showcase.fisio.service.FaturamentoService;

import java.math.BigDecimal;

public class FaturamentoEditPage extends LayoutPage {
    private static final long serialVersionUID = 1L;

    protected HiddenField fieldId = new HiddenField("id", Integer.class);
    
    // Core Layout Components
    protected Form form = new Form("form");
    protected Select selectCliente = new Select("clienteId", "Paciente:", true);
    protected Select selectTipo = new Select("tipoFaturamento", "Origem:", true);
    protected TextField campoValor = new TextField("valorTotalFaturado", "Valor do Pacote / Total:", true);
    protected Select selectParcelas = new Select("quantidadeParcelas", "Parcelar em:", true);
    protected TextField campoObservacoes = new TextField("observacoes", "Observações:");
    protected Submit botaoSalvar = new Submit("salvar", "Consolidar Faturamento", this, "onSalvarClick");
    protected PageLink linkCancelar = new PageLink("linkCancelar", "Voltar", FaturamentoViewPage.class);

    // Sub-Installment Grid Component (Rendered only during READ/EDIT mode)
    protected Table tabelaParcelas = new Table("tabelaParcelas");

    private FaturamentoService faturamentoService;

    public FaturamentoEditPage() {
        form.add(fieldId);
        form.add(selectCliente);
        form.add(selectTipo);
        form.add(campoValor);
        form.add(selectParcelas);
        form.add(campoObservacoes);
        form.add(botaoSalvar);
        form.add(linkCancelar);
        addControl(form);

        // Build the sub-installment inner metadata table columns
        tabelaParcelas.addColumn(new Column("numeroParcela", "Nº Parcela"));
        
        Column colVenc = new Column("dataVencimento", "Vencimento");
        colVenc.setDecorator((row, context) -> {
            RecebimentoParcela p = (RecebimentoParcela) row;
            return p.getDataVencimento() != null ? p.getDataVencimento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        });
        tabelaParcelas.addColumn(colVenc);

        Column colValPart = new Column("valorParcela", "Valor Parcela");
        colValPart.setDecorator((row, context) -> "R$ " + ((RecebimentoParcela) row).getValorParcela().toString());
        tabelaParcelas.addColumn(colValPart);
        
        tabelaParcelas.addColumn(new Column("statusPagamento", "Status Liquidação"));
        addControl(tabelaParcelas);
    }

    @Override
    public void onInit() {
        super.onInit();

        if (faturamentoService == null) return;

        // Populate independent dropdown data fields
        selectCliente.getOptionList().clear();
        selectCliente.add(new Option("", "-- Selecione o Paciente Beneficiário --"));
        for (Cliente c : faturamentoService.getAllClientes()) {
            selectCliente.add(new Option(c.getId().toString(), c.getNome()));
        }

        selectTipo.getOptionList().clear();
        selectTipo.add(new Option("PARTICULAR", "Particular (Recibo Direto)"));
        selectTipo.add(new Option("CONVENIO", "Faturamento p/ Lote de Convênio"));

        selectParcelas.getOptionList().clear();
        selectParcelas.add(new Option("1", "1x à vista"));
        selectParcelas.add(new Option("2", "2x s/ juros"));
        selectParcelas.add(new Option("3", "3x s/ juros"));
        selectParcelas.add(new Option("4", "4x s/ juros"));

        // INTERCEPT READ STATE: Preload installments if invoice ID exists
        String idParam = getContext().getRequestParameter("id");
        if (idParam != null && !idParam.trim().isEmpty() && !form.isFormSubmission()) {
            Faturamento target = faturamentoService.get(Integer.valueOf(idParam));
            if (target != null) {
                fieldId.setValue(target.getId().toString());
                selectCliente.setValue(target.getIdCliente().toString());
                selectTipo.setValue(target.getTipoFaturamento());
                campoValor.setValue(target.getValorTotalFaturado().toString());
                campoObservacoes.setValue(target.getObservacoes());
                
                // Read-only locks to protect consolidated historical entries
                selectCliente.setDisabled(true);
                selectTipo.setDisabled(true);
                campoValor.setDisabled(true);
                selectParcelas.setDisabled(true);
                botaoSalvar.setDisabled(true);

                // Fetch child rows linked to the parent transaction root
                tabelaParcelas.setRowList(faturamentoService.getParcelasByFaturamentoId(target.getId()));
            }
        }
    }

    public boolean onSalvarClick() {
        if (form.isValid()) {
            try {
                Faturamento fatura = new Faturamento();
                fatura.setTipoFaturamento(selectTipo.getValue());
                fatura.setObservacoes(campoObservacoes.getValue());
                
                Cliente c = new Cliente();
                c.setId(Integer.valueOf(selectCliente.getValue()));
                
                fatura.setIdCliente(Integer.valueOf(selectCliente.getValue()));

                BigDecimal montante = new BigDecimal(campoValor.getValue());
                int parcelas = Integer.parseInt(selectParcelas.getValue());

                // SOM Rich Pattern execution encapsulates automated installment generation
                fatura.gerarParcelasParticionadas(montante, parcelas);

                // Persist through the single unified atomic service boundary
                faturamentoService.create(fatura);

                setRedirect(FaturamentoViewPage.class);
                return false;
            } catch (Exception ex) {
                form.setError("Falha transacional: " + ex.getMessage());
            }
        }
        return true;
    }
}
