package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.model.enums.ProcedimentoSituacao;
import com.gilbertoca.fisio.service.ModalidadeService;
import com.gilbertoca.fisio.service.ProcedimentoService;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.util.Bindable;

/**
 * Cadastro do catálogo de procedimentos (recursos, aparelhos, técnicas).
 * A modalidade é OPCIONAL — procedimento de uso geral fica sem amarra e
 * serve a qualquer modalidade; os amarrados filtram as sugestões de conduta
 * na sala. Populate manual (como SessaoEditPage): os selects exigem setValue
 * por id/name, o copyFrom casaria o campo "modalidade" com a propriedade do
 * grafo e produziria toString no select.
 */
public class ProcedimentoEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    // Track internal persistence scope state
    private final HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable protected Integer id;

    final Form form = new Form("form");
    final TextField nome = new TextField("nome", "Nome:", true);
    final TextField codigoTuss = new TextField("codigoTuss", "Código TUSS (opcional):", false);
    final Select selectModalidade = new Select("selectModalidade", "Modalidade (opcional):", false);
    final Select selectSituacao = new Select("selectSituacao", "Situação:", true);

    private final ProcedimentoService procedimentoService = new ProcedimentoService();
    private final ModalidadeService modalidadeService = new ModalidadeService();

    public ProcedimentoEditPage() {
        form.setNoLayout(true);

        form.add(idField);
        form.add(nome);
        form.add(codigoTuss);
        form.add(selectModalidade);
        form.add(selectSituacao);
        form.add(new Submit("save", "Salvar", this, "onSalvarClick"));
        form.add(new Submit("cancel", "Cancelar", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Procedimento Edit");

        selectModalidade.getOptionList().clear();
        selectModalidade.add(Option.EMPTY_OPTION);
        for (Modalidade modalidade : modalidadeService.getAll()) {
            selectModalidade.add(new Option(modalidade.getId().toString(), modalidade.getNome()));
        }

        selectSituacao.getOptionList().clear();
        selectSituacao.add(Option.EMPTY_OPTION);
        for (ProcedimentoSituacao situacao : ProcedimentoSituacao.values()) {
            selectSituacao.add(new Option(situacao.name(), situacao.toString()));
        }
    }

    @Override
    public void onGet() {
        if (id != null) {
            Procedimento procedimento = procedimentoService.get(id);
            if (procedimento != null) {
                idField.setValueObject(procedimento.getId());
                nome.setValue(procedimento.getNome());
                codigoTuss.setValue(procedimento.getCodigoTuss());
                if (procedimento.getModalidade() != null) {
                    selectModalidade.setValue(procedimento.getModalidade().getId().toString());
                }
                if (procedimento.getSituacao() != null) {
                    selectSituacao.setValue(procedimento.getSituacao().name());
                }
            }
        }
    }

    public boolean onSalvarClick() {
        if (!form.isValid()) {
            return true;
        }

        Integer procedimentoId = id;
        Procedimento procedimento = (procedimentoId != null)
                ? procedimentoService.get(procedimentoId) : new Procedimento();
        if (procedimento == null) {
            form.setError("Procedimento informado não existe mais.");
            return true;
        }

        procedimento.setNome(nome.getValue().trim());
        procedimento.setCodigoTuss(codigoTuss.getValue());

        Integer modalidadeId = parseId(selectModalidade.getValue());
        if (modalidadeId != null) {
            Modalidade modalidade = new Modalidade();
            modalidade.setId(modalidadeId);
            procedimento.setModalidade(modalidade);
        } else {
            procedimento.setModalidade(null);
        }

        String situacao = selectSituacao.getValue();
        if (situacao == null || situacao.isEmpty()) {
            form.setError("Informe a situação do procedimento.");
            return true;
        }
        procedimento.setSituacao(ProcedimentoSituacao.valueOf(situacao));

        if (procedimentoId != null) {
            procedimentoService.update(procedimento);
        } else {
            procedimentoService.create(procedimento);
        }

        setRedirect(ProcedimentoViewPage.class);
        return false;
    }

    public boolean onCancelClick() {
        setRedirect(ProcedimentoViewPage.class);
        return false;
    }

    private Integer parseId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
