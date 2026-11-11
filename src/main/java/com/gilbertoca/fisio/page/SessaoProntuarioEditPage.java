package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.SessaoProntuario;
import com.gilbertoca.fisio.service.SessaoProntuarioService;
import com.gilbertoca.fisio.service.SessaoService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.util.Bindable;

/**
 * Tela da sala de atendimento (multi-paciente): o profissional registra as
 * anotações clínicas de CADA paciente da sessão (1 prontuário por
 * sessão/paciente) e finaliza a sessão, que exige a conduta de todos.
 * Fluxo: iniciar atendimento -> anotações por paciente (salvar) ->
 * [grupo] adicionar pacientes -> finalizar atendimento.
 */
public class SessaoProntuarioEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    // ID da sessão em atendimento
    @Bindable protected Integer id;
    // Paciente selecionado na sala (da sessão, para preencher/editar prontuário)
    @Bindable protected Integer paciente;

    private final HiddenField idField = new HiddenField("id", Integer.class);
    private final Form form = new Form("form");
    private final Select selectPaciente = new Select("selectPaciente", "Paciente da Sessão:", true);
    private final Select selectNovoPaciente = new Select("selectNovoPaciente", "Adicionar Paciente (grupo):", false);
    private final TextArea queixaSubjetiva = new TextArea("queixaSubjetiva", "Queixa Subjetiva (relato do cliente):", false);
    private final TextArea avaliacaoObjetiva = new TextArea("avaliacaoObjetiva", "Avaliação Objetiva (testes, palpação):", false);
    private final TextArea condutaProcedimento = new TextArea("condutaProcedimento", "Conduta / Procedimento Realizado:", false);
    private final TextArea planoOrientacoes = new TextArea("planoOrientacoes", "Plano e Orientações para Casa:", false);

    private final SessaoService sessaoService = new SessaoService();
    private final SessaoProntuarioService prontuarioService = new SessaoProntuarioService();

    public SessaoProntuarioEditPage() {
        addModel("title", "Atendimento");

        form.setNoLayout(true);

        form.add(idField);
        form.add(selectPaciente);
        form.add(selectNovoPaciente);
        form.add(queixaSubjetiva);
        form.add(avaliacaoObjetiva);
        form.add(condutaProcedimento);
        form.add(planoOrientacoes);

        queixaSubjetiva.setCols(60);
        queixaSubjetiva.setRows(4);
        avaliacaoObjetiva.setCols(60);
        avaliacaoObjetiva.setRows(4);
        condutaProcedimento.setCols(60);
        condutaProcedimento.setRows(5);
        planoOrientacoes.setCols(60);
        planoOrientacoes.setRows(3);

        form.add(new Submit("iniciar", "Iniciar Atendimento", this, "onIniciarClick"));
        form.add(new Submit("salvar", "Salvar Anotações do Paciente", this, "onSalvarClick"));
        form.add(new Submit("adicionar", "Adicionar Paciente", this, "onAdicionarClick"));
        form.add(new Submit("finalizar", "Finalizar Atendimento", this, "onFinalizarClick"));
        form.add(new Submit("cancel", "Voltar aos Atendimentos", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onGet() {
        if (id == null) {
            form.setError("Sessão não informada. Use a lista de atendimentos para selecionar uma sessão.");
            return;
        }

        Sessao sessao = sessaoService.get(id);
        if (sessao == null) {
            form.setError("Sessão não encontrada.");
            return;
        }

        // Contexto do atendimento exibido no template ($clienteNome etc.)
        addModel("sessaoId", sessao.getId());
        addModel("profissionalNome", sessao.getProfissional() != null ? sessao.getProfissional().getNome() : "-");
        addModel("modalidadeNome", sessao.getModalidade() != null ? sessao.getModalidade().getNome() : "-");
        addModel("sessaoTipo", sessao.getSessaoTipo() != null ? sessao.getSessaoTipo().name() : "-");
        addModel("dataHoraInicio", sessao.getDataHoraInicio() != null ? sessao.getDataHoraInicio().toString() : "-");
        addModel("situacao", sessao.getSituacao() != null ? sessao.getSituacao().name() : "-");

        // Pacientes da sessão (titular + grupo), com estado do registro clínico
        List<SessaoProntuario> pacientes = prontuarioService.listarPacientesDaSessao(id);
        addModel("pacientes", pacientes);

        selectPaciente.getOptionList().clear();
        for (SessaoProntuario registro : pacientes) {
            selectPaciente.add(new Option(registro.getClienteId().toString(), registro.getClienteNome()));
        }

        selectNovoPaciente.getOptionList().clear();
        selectNovoPaciente.add(Option.EMPTY_OPTION);
        for (Cliente cliente : sessaoService.getAllClientes()) {
            selectNovoPaciente.add(new Option(cliente.getId().toString(), cliente.getNome()));
        }

        // Paciente selecionado: query param "paciente" ou o primeiro da lista
        Integer pacienteSelecionado = (paciente != null) ? paciente
                : (!pacientes.isEmpty() ? pacientes.get(0).getClienteId() : null);
        if (pacienteSelecionado != null) {
            selectPaciente.setValue(pacienteSelecionado.toString());
            SessaoProntuario prontuario = prontuarioService.getBySessaoCliente(id, pacienteSelecionado);
            if (prontuario != null) {
                form.copyFrom(prontuario);
            }
        }
    }

    private Integer sessaoIdAtiva() {
        return (id != null) ? id : (Integer) idField.getValueObject();
    }

    private Integer pacienteSelecionadoId() {
        return parseId(selectPaciente.getValue());
    }

    private SessaoProntuario montarProntuarioDaTela() {
        SessaoProntuario prontuario = new SessaoProntuario();
        form.copyTo(prontuario);
        return prontuario;
    }

    public boolean onIniciarClick() {
        try {
            prontuarioService.iniciarAtendimento(sessaoIdAtiva());
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
        redirectParaMesmaTela();
        return false;
    }

    public boolean onSalvarClick() {
        Integer pacienteId = pacienteSelecionadoId();
        if (pacienteId == null) {
            form.setError("Selecione um paciente da sessão para salvar as anotações.");
            return true;
        }
        try {
            prontuarioService.salvarAnotacoes(sessaoIdAtiva(), pacienteId, montarProntuarioDaTela());
            addModel("mensagem", "Anotações salvas para o paciente selecionado.");
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
        redirectParaMesmaTela(pacienteId);
        return false;
    }

    public boolean onAdicionarClick() {
        Integer novoPacienteId = parseId(selectNovoPaciente.getValue());
        if (novoPacienteId == null) {
            form.setError("Selecione o cliente a adicionar na sessão de grupo.");
            return true;
        }
        try {
            prontuarioService.adicionarPaciente(sessaoIdAtiva(), novoPacienteId);
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
        redirectParaMesmaTela(novoPacienteId);
        return false;
    }

    public boolean onFinalizarClick() {
        try {
            Sessao atualizada = prontuarioService.finalizarAtendimento(sessaoIdAtiva());
            addModel("mensagem", "Atendimento finalizado: " + atualizada.getSituacao() + ".");
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
        setRedirect(SessaoProntuarioViewPage.class);
        return false;
    }

    public boolean onCancelClick() {
        setRedirect(SessaoProntuarioViewPage.class);
        return false;
    }

    /** Recarrega a sala mantendo sessão e paciente em atendimento. */
    private void redirectParaMesmaTela() {
        redirectParaMesmaTela(pacienteSelecionadoId());
    }

    private void redirectParaMesmaTela(Integer pacienteId) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", sessaoIdAtiva());
        if (pacienteId != null) {
            params.put("paciente", pacienteId);
        }
        setRedirect(SessaoProntuarioEditPage.class, params);
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
