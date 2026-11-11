package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Profissional;
import com.gilbertoca.fisio.service.ProfissionalService;
import org.apache.click.control.*;
import org.apache.click.extras.control.LocalDateField;
import org.apache.click.util.Bindable;

public class ProfissionalEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private final HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable protected Integer id;
    private final Form form = new Form("form");
    private final TextField nome;
    private final TextField cpf;
    private final LocalDateField dtNascimento;
    private final TextField email;
    private final TextField telefone;
    private final TextField registroNumero;
    private final TextField registroOrgao;
    private final ProfissionalService profissionalService = new ProfissionalService();

    public ProfissionalEditPage() {
        addModel("title", "Profissional Edit");

        form.setNoLayout(true);

        nome = new TextField("nome", "Nome Completo:", true);
        cpf = new TextField("cpf", "CPF (Somente Números):", true);
        dtNascimento = new LocalDateField("dtNascimento", "Data de Nascimento:", true);
        email = new TextField("email", "Email:", false);
        telefone = new TextField("telefone", "Telefone/Celular:", true);
        registroNumero = new TextField("registroNumero", "Número do Registro:", true);
        registroOrgao = new TextField("registroOrgao", "Órgão do Registro:", true);

        form.add(idField);
        form.add(nome);
        form.add(cpf);
        form.add(dtNascimento);
        form.add(email);
        form.add(telefone);
        form.add(registroNumero);
        form.add(registroOrgao);
        form.add(new Submit("save", "Salvar", this, "onSaveClick"));
        form.add(new Submit("cancel", "Cancelar", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onGet() {
        if (id != null) {
            Profissional profissional = profissionalService.get(id);
            if (profissional != null) {
                form.copyFrom(profissional);
            }
        }
    }

    public boolean onSaveClick() {
        if (form.isValid()) {
            Integer profissionalId = (Integer) idField.getValueObject();
            if (profissionalId != null) {
                Profissional profissional = profissionalService.get(profissionalId);
                form.copyTo(profissional);
                profissionalService.update(profissional);
            } else {
                Profissional profissional = new Profissional();
                form.copyTo(profissional);
                profissionalService.create(profissional);
            }
            setRedirect(ProfissionalViewPage.class);
            return false;
        }
        return true;
    }

    public boolean onCancelClick() {
        setRedirect(ProfissionalViewPage.class);
        return false;
    }
}
