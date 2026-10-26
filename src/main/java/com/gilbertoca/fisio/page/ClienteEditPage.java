package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.service.ClienteService;
import org.apache.click.control.*;
import org.apache.click.extras.control.LocalDateField;
import org.apache.click.util.Bindable;

public class ClienteEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    // Track internal persistence scope state
    private final HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable protected Integer id;
    private final Form form = new Form("form");
    private final TextField nome;
    private final TextField cpf;
    private final LocalDateField dtNascimento;
    private final TextField telefone;
    private final TextField email;
    //private final Select selectConvenio;
    private final Select selectSituacao;
    private final ClienteService clienteService = new ClienteService();

    public ClienteEditPage() {
        addModel("title", "Cliente Edit");

        form.setNoLayout(true);
        
        nome = new TextField("nome", "Nome Completo:", true);
        cpf = new TextField("cpf", "CPF (Somente Números):", false);
        dtNascimento = new LocalDateField("dtNascimento", "Data de Nascimento:", true);
        telefone = new TextField("telefone", "Telefone/Celular:", false);
        email = new TextField("email", "Email:", false);
        //selectConvenio = new Select("convenio", "Plano de Saúde / Convênio:", false);
        selectSituacao = new Select("situacao", "Situação:", true);

        form.add(idField);
        form.add(nome);
        form.add(cpf);
        form.add(dtNascimento);
        form.add(telefone);
        form.add(email);
        //form.add(selectConvenio);
        form.add(selectSituacao);
        form.add(new Submit("save", "Salvar", this, "onSaveClick"));
        form.add(new Submit("cancel", "Cancelar", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        // Populate dropdown lookup metrics
//        selectConvenio.getOptionList().clear();
//        selectConvenio.add(new Option("", "-- Particular (Sem Convênio) --"));
//        for (Convenio conv : clienteService.getAllConvenios()) {
//            selectConvenio.add(new Option(conv.getId().toString(), conv.getNome()));
//        }

        selectSituacao.getOptionList().clear();
        selectSituacao.add(Option.EMPTY_OPTION);
        selectSituacao.add(new Option("ATIVO", "Ativo / Em Atendimento"));
        selectSituacao.add(new Option("INATIVO", "Inativo"));
    }

    @Override
    public void onGet() {
        if (id != null) {
            Cliente cliente = clienteService.get(id);
            if (cliente != null) {
                form.copyFrom(cliente);
            }
        }
    }

    public boolean onSaveClick() {
        if (form.isValid()) {
            Integer clienteId = (Integer) idField.getValueObject();
            if (clienteId != null) {
                Cliente cliente = clienteService.get(clienteId);
                form.copyTo(cliente);
                clienteService.update(cliente);                
            } else {
                Cliente cliente = new Cliente();
                form.copyTo(cliente);
                clienteService.create(cliente);
            }
            setRedirect(ClienteViewPage.class);
            return false;
        }
        return true;
    }

    public boolean onCancelClick() {
        setRedirect(ClienteViewPage.class);
        return false;
    }
}
