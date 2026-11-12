package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.service.ModalidadeService;
import org.apache.click.control.*;
import org.apache.click.util.Bindable;

public class ModalidadeEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private final HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable protected Integer id;
    private final Form form = new Form("form");
    private final TextField nome;
    private final TextField duracaoMinutos;
    private final TextField valorBase;
    private final TextField capacidadeSimultanea;
    private final ModalidadeService modalidadeService = new ModalidadeService();

    public ModalidadeEditPage() {
        form.setNoLayout(true);

        nome = new TextField("nome", "Nome da Modalidade:", true);
        duracaoMinutos = new TextField("duracaoMinutos", "Duração (Minutos):", true);
        valorBase = new TextField("valorBase", "Valor Base (R$):", true);
        capacidadeSimultanea = new TextField("capacidadeSimultanea", "Capacidade Simultânea (fisio: 1, pilates: 4):", true);

        form.add(idField);
        form.add(nome);
        form.add(duracaoMinutos);
        form.add(valorBase);
        form.add(capacidadeSimultanea);
        form.add(new Submit("save", "Salvar", this, "onSaveClick"));
        form.add(new Submit("cancel", "Cancelar", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Modalidade Edit");
    }

    @Override
    public void onGet() {
        if (id != null) {
            Modalidade modalidade = modalidadeService.get(id);
            if (modalidade != null) {
                form.copyFrom(modalidade);
            }
        }
    }

    public boolean onSaveClick() {
        if (form.isValid()) {
            if (id != null) {
                Modalidade modalidade = modalidadeService.get(id);
                if (modalidade == null) {
                    form.setError("Modalidade informada não existe mais.");
                    return true;
                }
                form.copyTo(modalidade);
                modalidadeService.update(modalidade);
            } else {
                Modalidade modalidade = new Modalidade();
                form.copyTo(modalidade);
                modalidadeService.create(modalidade);
            }
            setRedirect(ModalidadeViewPage.class);
            return false;
        }
        return true;
    }

    public boolean onCancelClick() {
        setRedirect(ModalidadeViewPage.class);
        return false;
    }
}
