package org.apache.click.showcase.fisio.page;

import org.apache.click.control.*;
import org.apache.click.extras.control.DateField;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Convenio;
import org.apache.click.showcase.fisio.service.ClienteService;

import java.time.ZoneId;
import java.util.Date;

public class ClienteEditPage extends LayoutPage {
    private static final long serialVersionUID = 1L;

    // Track internal persistence scope state
    protected HiddenField fieldId = new HiddenField("id", Integer.class);

    protected Form form = new Form("form");
    protected TextField campoNome = new TextField("nome", "Nome Completo:", true);
    protected TextField campoCpf = new TextField("cpf", "CPF (Somente Números):", true);
    protected DateField campoDataNascimento = new DateField("dataNascimento", "Data de Nascimento:", true);
    protected TextField campoTelefone = new TextField("telefone", "Telefone/Celular:", true);
    protected Select selectConvenio = new Select("convenioId", "Plano de Saúde / Convênio:", false);
    protected Select selectStatus = new Select("statusClinico", "Situação Clínica:", true);
    
    protected Submit botaoSalvar = new Submit("salvar", "Salvar Registro", this, "onSalvarClick");
    protected PageLink linkCancelar = new PageLink("linkCancelar", "Voltar", ClienteViewPage.class);

    private ClienteService clienteService = new ClienteService();

    public ClienteEditPage() {
        form.setAttribute("class", "pure-form pure-form-stacked");
        form.add(fieldId);
        form.add(campoNome);
        form.add(campoCpf);
        form.add(campoDataNascimento);
        form.add(campoTelefone);
        form.add(selectConvenio);
        form.add(selectStatus);
        form.add(botaoSalvar);
        form.add(linkCancelar);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        campoDataNascimento.setFormatPattern("dd/MM/yyyy");

        // Populate dropdown lookup metrics
        selectConvenio.getOptionList().clear();
        selectConvenio.add(new Option("", "-- Particular (Sem Convênio) --"));
        for (Convenio conv : clienteService.getAllConvenios()) {
            selectConvenio.add(new Option(conv.getId().toString(), conv.getNome()));
        }

        selectStatus.getOptionList().clear();
        selectStatus.add(new Option("ATIVO", "Ativo / Em Atendimento"));
        selectStatus.add(new Option("INATIVO", "Inativo"));

        // DETECT EDITS: Intercept inbound parameter requests
        String idParam = getContext().getRequestParameter("id");
        if (idParam != null && !idParam.trim().isEmpty() && !form.isFormSubmission()) {
            Cliente target = clienteService.get(Integer.valueOf(idParam));
            if (target != null) {
                // Populate flat property layout parameters into form view
                fieldId.setValue(target.getId().toString());
                campoNome.setValue(target.getNome());
                campoCpf.setValue(target.getCpf());
                campoTelefone.setValue(target.getTelefone());
                selectStatus.setValue(target.getStatusClinico());
                
                if (target.getDataNascimento() != null) {
                    campoDataNascimento.setDate(Date.from(target.getDataNascimento()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
                if (target.getConvenio() != null) {
                    selectConvenio.setValue(target.getConvenio().getId().toString());
                }
            }
        }
    }

    public boolean onSalvarClick() {
        if (form.isValid()) {
            try {
                Cliente cliente = new Cliente();
                cliente.setNome(campoNome.getValue());
                cliente.setCpf(campoCpf.getValue());
                cliente.setTelefone(campoTelefone.getValue());
                cliente.setStatusClinico(selectStatus.getValue());

                Date dateVal = campoDataNascimento.getDate();
                if (dateVal != null) {
                    cliente.setDataNascimento(dateVal.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                }

                String chosenConvenio = selectConvenio.getValue();
                if (chosenConvenio != null && !chosenConvenio.trim().isEmpty()) {
                    Convenio c = new Convenio();
                    c.setId(Integer.valueOf(chosenConvenio));
                    cliente.setConvenio(c);
                }

                // Branch execution path: UPDATE if hidden ID field is present, else CREATE fresh
                String activeId = fieldId.getValue();
                if (activeId != null && !activeId.trim().isEmpty()) {
                    cliente.setId(Integer.valueOf(activeId));
                    clienteService.update(cliente);
                } else {
                    clienteService.create(cliente);
                }

                setRedirect(ClienteViewPage.class);
                return false;
            } catch (Exception ex) {
                form.setError("Erro ao processar persistência: " + ex.getMessage());
            }
        }
        return true;
    }
}
