package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.SessaoProntuario;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import com.gilbertoca.fisio.service.SessaoProntuarioService;
import com.gilbertoca.fisio.service.SessaoService;
import java.util.HashMap;
import java.util.Map;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
import org.apache.click.util.Bindable;

/**
 * Sala de atendimento: registra as anotações clínicas do paciente da sessão.
 *
 * Tudo na tela pertence à mesma entidade {@link Sessao}: a definição inicial
 * do agendamento (paciente, profissional, modalidade, tipo, horário, situação)
 * entra como campos readonly do Form — atribuída pela recepção, não editável
 * aqui — e o profissional preenche apenas as anotações do prontuário (1:1),
 * cujo paciente é o cliente_id do agendamento. Atendimento individualizado;
 * modalidades de grupo dividem o mesmo slot com N sessões individuais
 * (capacidade_simultanea), cada uma com linha própria na listagem.
 *
 * A tela segue a cadeia de transições da situação da sessão:
 * <ul>
 * <li><b>AGENDADA</b>: botões Iniciar Atendimento + Cancelar; anotações
 * readonly (o atendimento sequer iniciou).</li>
 * <li><b>EM_ANDAMENTO</b>: botão Salvar Anotações + Cancelar; anotações
 * editáveis. Finalizar Atendimento só aparece quando o registro clínico
 * 1:1 da sessão tem conduta registrada — mesmo critério do guard de
 * SessaoService.finalizarAtendimento.</li>
 * <li><b>REALIZADA / FALTOU / CANCELADA</b>: histórico — sem botões de
 * transição, somente Cancelar; anotações readonly.</li>
 * </ul>
 *
 * Ciclo de vida Click canônico:
 * <ul>
 * <li><b>Construtor</b>: monta o Form completo — contexto readonly,
 * anotações e os 4 botões como campos final (Iniciar, Salvar, Finalizar,
 * Cancelar). Nenhum acesso a banco.</li>
 * <li><b>onInit()</b>: prepara o Form e suas dependências (título) —
 * NÃO carrega o modelo a ser editado.</li>
 * <li><b>onGet()</b> (GET): carrega a Sessao e o prontuário persistido e
 * popula o Form (contexto readonly + form.copyFrom).</li>
 * <li><b>onRender()</b>: aplica o estado de exibição comum a GET e POST —
 * carrega a Sessao quando o fluxo não passou pelo onGet (ciclo POST) e
 * remove/re-insere os botões conforme a situação. No POST o onRender roda
 * DEPOIS dos handlers do Form (performOnPostOrGet → performOnRender no
 * ClickServlet), então o re-render pós-ação reflete a situação atual (ex.:
 * Salvar com conduta faz o Finalizar aparecer na mesma tela).</li>
 * </ul>
 *
 * Retorno dos handlers: quem redireciona retorna false (interrompe o
 * processamento — Iniciar, Finalizar, Cancelar); quem re-renderiza a mesma
 * tela retorna true, pois performOnProcess false pula também o onRender
 * (ClickServlet.processPageEvents) e a tela sairia sem o estado aplicado.
 *
 * O estado da tela é aplicado por remoção/re-inserção dos botões já
 * existentes ({@link Form#remove} / {@link Form#add}) e readonly das
 * textareas — nunca pela criação de controles no onInit. Defesa em
 * profundidade: como todos os botões existem no processamento do Form, um
 * POST forjado despacharia o handler, mas o guard do service rejeitaria a
 * transição ilegal.
 */
public class SessaoProntuarioEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    /**
     * ID da sessão em atendimento. Ligado automaticamente pelo Click em
     * qualquer request: query string no GET e o HiddenField abaixo no POST.
     */
    @Bindable protected Integer id;

    /** Carrega o id da sessão no POST (padrão Click para tela de edição). */
    private final HiddenField idField = new HiddenField("id", Integer.class);
    private final Form form = new Form("form");

    // Definição inicial do agendamento: campos da mesma entidade Sessao
    private final TextField pacienteField = new TextField("paciente", "Paciente:");
    private final TextField profissionalField = new TextField("profissional", "Profissional:");
    private final TextField modalidadeField = new TextField("modalidade", "Modalidade:");
    private final TextField tipoField = new TextField("tipo", "Tipo:");
    private final TextField horarioField = new TextField("horario", "Horário:");
    private final TextField situacaoField = new TextField("situacao", "Situação:");

    private final TextArea queixaSubjetiva = new TextArea("queixaSubjetiva", "Queixa Subjetiva (relato do cliente):", false);
    private final TextArea avaliacaoObjetiva = new TextArea("avaliacaoObjetiva", "Avaliação Objetiva (testes, palpação):", false);
    private final TextArea condutaProcedimento = new TextArea("condutaProcedimento", "Conduta / Procedimento Realizado:", false);
    private final TextArea planoOrientacoes = new TextArea("planoOrientacoes", "Plano e Orientações para Casa:", false);

    // Botões de transição e o permanente: criados uma única vez no construtor;
    // a situação da sessão define quais permanecem no Form (prepararEstadoDaTela)
    private final Submit iniciarSubmit = new Submit("iniciar", "Iniciar Atendimento", this, "onIniciarClick");
    private final Submit salvarSubmit = new Submit("salvar", "Salvar Anotações", this, "onSalvarClick");
    private final Submit finalizarSubmit = new Submit("finalizar", "Finalizar Atendimento", this, "onFinalizarClick");
    private final Submit cancelarSubmit = new Submit("cancelar", "Cancelar", this, "onCancelClick");

    private final SessaoService sessaoService = new SessaoService();
    private final SessaoProntuarioService prontuarioService = new SessaoProntuarioService();

    /** Sessao carregada no onGet/onPost: base do estado da tela. */
    private Sessao sessao;

    public SessaoProntuarioEditPage() {
        form.setNoLayout(true);

        form.add(idField);
        form.add(pacienteField);
        form.add(profissionalField);
        form.add(modalidadeField);
        form.add(tipoField);
        form.add(horarioField);
        form.add(situacaoField);
        form.add(queixaSubjetiva);
        form.add(avaliacaoObjetiva);
        form.add(condutaProcedimento);
        form.add(planoOrientacoes);

        // Contexto do agendamento: exibição fixa, valor vindo do servidor
        pacienteField.setReadonly(true);
        profissionalField.setReadonly(true);
        modalidadeField.setReadonly(true);
        tipoField.setReadonly(true);
        horarioField.setReadonly(true);
        situacaoField.setReadonly(true);

        queixaSubjetiva.setCols(60);
        queixaSubjetiva.setRows(4);
        avaliacaoObjetiva.setCols(60);
        avaliacaoObjetiva.setRows(4);
        condutaProcedimento.setCols(60);
        condutaProcedimento.setRows(5);
        planoOrientacoes.setCols(60);
        planoOrientacoes.setRows(3);

        // Todos os botões existem desde a montagem do Form; o estado da tela
        // decide quais permanecem (remoção/re-inserção em prepararEstadoDaTela)
        form.add(iniciarSubmit);
        form.add(salvarSubmit);
        form.add(finalizarSubmit);
        form.add(cancelarSubmit);
        addControl(form);
    }

    /**
     * Preparação do Form e suas dependências — sem carga de modelo (a Sessao
     * é carregada no onGet/onPost, fases de processamento do request).
     */
    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Atendimento");
    }

    /**
     * Abertura da sala (GET): carrega a Sessao e popula o Form — contexto
     * readonly da definição do agendamento e prontuário persistido via
     * form.copyFrom (padrão do javadoc de Form.copyFrom).
     */
    @Override
    public void onGet() {
        carregarSessao();

        if (sessao != null) {
            // Round-trip do id no POST: o Form só processa os filhos na
            // submissão, então o HiddenField precisa receber o valor aqui no GET.
            idField.setValueObject(sessao.getId());

            // Definição inicial do agendamento: contexto fixo, não editável
            // (texto(Object) usa toString: os enums exibem o rótulo legível)
            pacienteField.setValue(texto(sessao.getCliente() != null ? sessao.getCliente().getNome() : null));
            profissionalField.setValue(texto(sessao.getProfissional() != null ? sessao.getProfissional().getNome() : null));
            modalidadeField.setValue(texto(sessao.getModalidade() != null ? sessao.getModalidade().getNome() : null));
            tipoField.setValue(texto(sessao.getSessaoTipo()));
            horarioField.setValue(texto(sessao.getDataHoraInicio() != null ? sessao.getDataHoraInicio().toString() : null));
            situacaoField.setValue(texto(sessao.getSituacao()));

            // Registro clínico 1:1 da sessão (o paciente é o do agendamento)
            SessaoProntuario prontuario = prontuarioService.getBySessao(id);
            if (prontuario != null) {
                form.copyFrom(prontuario);
            }
        }
    }

    /**
     * Estado de exibição comum a GET e POST (javadoc de Page.onRender: "if
     * you have code used in both onGet() and onPost(), use onRender()").
     * No ciclo POST o onGet não roda: a Sessao é carregada aqui quando o
     * processamento ainda não a trouxe — operação de exibição, sancionada
     * pelo papel do onRender. Remove e re-insere os botões de transição já
     * existentes no Form e ajusta o readonly das anotações, conforme a
     * situação da Sessao carregada.
     */
    @Override
    public void onRender() {
        super.onRender();

        if (sessao == null) {
            carregarSessao();
        }
        aplicarEstadoDaTela();
    }

    /** Carga do modelo: a Sessao atendida (ou erro na sala, sem modelo). */
    private void carregarSessao() {
        sessao = null;
        if (id == null) {
            form.setError("Sessão não informada. Use a lista de atendimentos para selecionar uma sessão.");
            return;
        }
        sessao = sessaoService.get(id);
        if (sessao == null) {
            form.setError("Sessão não encontrada.");
        }
    }

    /**
     * Aplica o estado de exibição dirigido pela situação da Sessao carregada:
     * remove e re-insere os botões de transição já existentes no Form e
     * ajusta o readonly das anotações. Ordem final dos botões: transições
     * aplicáveis primeiro, Cancelar por último.
     */
    private void aplicarEstadoDaTela() {
        form.remove(iniciarSubmit);
        form.remove(salvarSubmit);
        form.remove(finalizarSubmit);
        form.remove(cancelarSubmit);

        boolean emAndamento = atendimentoEmAndamento();

        // Anotações clínicas: editáveis somente com o atendimento em andamento
        queixaSubjetiva.setReadonly(!emAndamento);
        avaliacaoObjetiva.setReadonly(!emAndamento);
        condutaProcedimento.setReadonly(!emAndamento);
        planoOrientacoes.setReadonly(!emAndamento);

        if (podeIniciar()) {
            form.add(iniciarSubmit);
        } else if (emAndamento) {
            form.add(salvarSubmit);
            if (podeFinalizar()) {
                form.add(finalizarSubmit);
            }
        }
        // REALIZADA / FALTOU / CANCELADA: histórico — sem transições
        form.add(cancelarSubmit);
    }

    /** A sala pode iniciar o atendimento? Só a partir de AGENDADA. */
    private boolean podeIniciar() {
        return sessao != null && sessao.getSituacao() == SessaoSituacao.AGENDADA;
    }

    /** Anotações clínicas editáveis: atendimento em andamento. */
    private boolean atendimentoEmAndamento() {
        return sessao != null && sessao.getSituacao() == SessaoSituacao.EM_ANDAMENTO;
    }

    /**
     * Finalizar exige o registro clínico 1:1 da sessão com conduta
     * registrada — mesmo critério do guard do service
     * (SessaoService.finalizarAtendimento).
     */
    private boolean podeFinalizar() {
        return atendimentoEmAndamento() && sessaoService.podeFinalizar(sessao.getId());
    }

    /** Value textual padrão: "-" quando o dado não veio do banco. */
    private String texto(Object valor) {
        return (valor != null) ? valor.toString() : "-";
    }

    private SessaoProntuario montarProntuarioDaTela() {
        SessaoProntuario prontuario = new SessaoProntuario();
        form.copyTo(prontuario);
        return prontuario;
    }

    public boolean onIniciarClick() {
        if (id == null) {
            form.setError("Sessão não informada.");
            return true;
        }
        try {
            sessaoService.iniciarAtendimento(id);
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
        // Post Redirect: reabre a sala, que recarrega o contexto (situação nova)
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        setRedirect(SessaoProntuarioEditPage.class, params);
        return false;
    }

    public boolean onSalvarClick() {
        if (id == null) {
            form.setError("Sessão não informada.");
            return true;
        }
        try {
            prontuarioService.salvarAnotacoes(id, montarProntuarioDaTela());
            addModel("mensagem", "Anotações salvas.");
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
        // Re-render da mesma tela: retorna true para o servlet seguir para
        // onPost/onRender — retornar false pularia o onRender e a tela sairia
        // sem o estado aplicado (botões e readonly das anotações). O onRender
        // reavalia podeFinalizar: com a conduta salva, o Finalizar aparece.
        return true;
    }

    public boolean onFinalizarClick() {
        if (id == null) {
            form.setError("Sessão não informada.");
            return true;
        }
        try {
            Sessao atualizada = sessaoService.finalizarAtendimento(id);
            addModel("mensagem", "Atendimento finalizado: " + atualizada.getSituacao() + ".");
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
        setRedirect(SessaoProntuarioViewPage.class);
        return false;
    }

    /** Único botão permanente: volta para Meus Atendimentos. */
    public boolean onCancelClick() {
        setRedirect(SessaoProntuarioViewPage.class);
        return false;
    }
}
