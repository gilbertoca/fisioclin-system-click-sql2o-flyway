package org.apache.click.showcase.fisio.page;

import org.apache.click.control.*;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.service.ClienteService;

import org.apache.click.extras.control.LocalDateField;
import org.apache.click.util.Bindable;

public class ClienteEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    // Track internal persistence scope state
    private HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable protected Integer id;
    private Form form = new Form("form");
    private TextField nome;
    private TextField cpf;
    private LocalDateField dtNascimento;
    private TextField telefone;
    private Select selectConvenio;
    private Select selectStatus;
    private ClienteService clienteService = new ClienteService();

    public ClienteEditPage() {
        addModel("title", "Login");
        addControl(form);

        form.setLayout(Form.LAYOUT_DIV);
        
        nome = new TextField("nome", "Nome Completo:", true);
        cpf = new TextField("cpf", "CPF (Somente Números):", true);
        dtNascimento = new LocalDateField("dtNascimento", "Data de Nascimento:", true);
        telefone = new TextField("telefone", "Telefone/Celular:", true);
        selectConvenio = new Select("convenio", "Plano de Saúde / Convênio:", false);
        selectStatus = new Select("status", "Situação Clínica:", true);

        form.add(idField);
        form.add(nome);
        form.add(cpf);
        form.add(dtNascimento);
        form.add(telefone);
        //form.add(selectConvenio);
        form.add(selectStatus);
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

        selectStatus.getOptionList().clear();
        selectStatus.add(new Option("U", ""));
        selectStatus.add(new Option("ATIVO", "Ativo / Em Atendimento"));
        selectStatus.add(new Option("INATIVO", "Inativo"));
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
