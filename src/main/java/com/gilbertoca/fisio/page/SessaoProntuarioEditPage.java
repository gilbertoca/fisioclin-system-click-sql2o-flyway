package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.SessaoProcedimento;
import com.gilbertoca.fisio.model.enums.ProcedimentoSituacao;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import com.gilbertoca.fisio.service.ProcedimentoService;
import com.gilbertoca.fisio.service.SessaoService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.PickList;
import org.apache.click.util.Bindable;

/**
 * Sala de atendimento: registra o registro do atendimento do paciente da
 * sessão.
 *
 * Tudo na tela pertence à MESMA entidade {@link Sessao}: a definição inicial
 * do agendamento (paciente, profissional, modalidade, tipo, horário, situação)
 * entra como QUADRO somente leitura no topo, desenhado no template (table
 * semântica direto da $sessao publicada em onRender) — atribuída pela
 * recepção, não editável aqui — e o profissional preenche apenas o registro
 * do atendimento: os
 * textos clínicos (registro_relato/avaliacao/conduta/orientacoes) e o
 * PICKLIST de condutas (roteiro da sessão, sessao_procedimento) — colunas e
 * itens da própria sessão. O picklist é um <select multiple> nativo do Click
 * com size fixado — ZERO JavaScript. Sugestões vêm do
 * catálogo ATIVO da modalidade (+ uso geral).
 * Atendimento individualizado; modalidades de grupo dividem
 * o mesmo slot com N sessões individuais (capacidade_simultanea), cada uma
 * com linha própria na listagem.
 *
 * A tela segue a cadeia de transições da situação da sessão. Estado é efeito
 * de GRAVAÇÃO real, não de navegação: abrir o formulário não persiste nada.
 * <ul>
 * <li><b>AGENDADA / CONFIRMADA</b>: painel de conferência do agendamento
 * (Iniciar Atendimento + Cancelar). Iniciar é NAVEGAÇÃO — recarrega a sala
 * com o formulário de registro (?editar=true) sem tocar no banco.</li>
 * <li><b>Primeira gravação do registro</b>: é o Salvar Anotações que inicia
 * o atendimento — o service avança AGENDADA/CONFIRMADA → EM_ANDAMENTO e
 * marca registro_dt_inicio na mesma transação. Se o profissional desistir
 * (Cancelar), nada foi alterado — não há transição a desfazer.</li>
 * <li><b>EM_ANDAMENTO</b>: botão Salvar Anotações + Cancelar; anotações
 * editáveis. Finalizar Atendimento só aparece quando o registro da sessão
 * tem conduta registrada — mesmo critério do guard de
 * SessaoService.finalizarAtendimento.</li>
 * <li><b>REALIZADA / FALTOU / CANCELADA</b>: histórico — sem botões de
 * transição, somente Cancelar; anotações readonly (REALIZADA ainda permite
 * consolidação do texto via service, a tela não oferece edição).</li>
 * </ul>
 *
 * Ciclo de vida Click canônico:
 * <ul>
 * <li><b>Construtor</b>: monta o Form completo — anotações, picklist de
 * condutas e os 4 botões como campos final (Iniciar, Salvar, Finalizar,
 * Cancelar). Nenhum acesso a banco.</li>
 * <li><b>onInit()</b>: prepara o Form e suas dependências (título) —
 * NÃO carrega o modelo a ser editado.</li>
 * <li><b>onGet()</b> (GET): carrega a Sessao (query única: agenda + registro)
 * e popula o Form (HiddenField do id + textos clínicos).</li>
 * <li><b>onRender()</b>: aplica o estado de exibição comum a GET e POST —
 * carrega a Sessao quando o fluxo não passou pelo onGet (ciclo POST),
 * publica o modelo de exibição ($sessao + $horario) e remove/re-insere os
 * botões conforme a situação. No POST o onRender roda DEPOIS dos handlers
 * do Form (performOnPostOrGet → performOnRender no ClickServlet), então o
 * re-render pós-ação reflete a situação atual (ex.: Salvar com conduta faz
 * o Finalizar aparecer na mesma tela).</li>
 * </ul>
 *
 * Transição visual no template (sessao-prontuario-edit.htm): na fila da sala
 * (AGENDADA/CONFIRMADA) sem ?editar=true o template exibe o painel de
 * conferência do agendamento (dados direto da $sessao + Iniciar/Cancelar
 * entre form.startTag()/endTag()); nos demais casos exibe o quadro de
 * contexto (table compacta, somente leitura) + o Form completo.
 * O Iniciar redireciona para a mesma sala com editar=true — o formulário
 * entra e a primeira gravação efetiva EM_ANDAMENTO. O sinal editar chega ao
 * template pelo próprio @Bindable (o ClickServlet publica campos bindables
 * não-Control no modelo em createTemplateModel — nada de addModel manual).
 *
 * População manual (sem form.copyFrom): campos como o HiddenField do id e as
 * textareas do registro recebem o valor explicitamente da Sessao carregada;
 * o contexto do agendamento nem é Form — é conteúdo do template, direto da
 * $sessao publicada em onRender.
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

    /**
     * Sinal de navegação do painel de conferência: ?editar=true abre o
     * formulário do registro sem persistir nada — a transição para
     * EM_ANDAMENTO só acontece na primeira gravação (salvarRegistro).
     * O ClickServlet publica este campo no modelo do template
     * (createTemplateModel), o que o torna acessível como $editar.
     */
    @Bindable protected Boolean editar;

    /** Carrega o id da sessão no POST (padrão Click para tela de edição). */
    private final HiddenField idField = new HiddenField("id", Integer.class);
    private final Form form = new Form("form");
    private final FieldSet prontuario = new FieldSet("Registro");

    // Registro do atendimento: colunas registro_* da própria Sessao
    private final TextArea registroRelato = new TextArea("registroRelato", "Queixa Subjetiva (relato do cliente):", false);
    private final TextArea registroAvaliacao = new TextArea("registroAvaliacao", "Avaliação Objetiva (testes, palpação):", false);
    private final TextArea registroConduta = new TextArea("registroConduta", "Conduta / Procedimento Realizado:", false);
    private final TextArea registroOrientacoes = new TextArea("registroOrientacoes", "Plano e Orientações para Casa:", false);

    /**
     * Picklist de condutas (roteiro da sessão): PickList do click-extras —
     * duas listbox lado a lado (disponíveis / no roteiro) com botões de
     * transferência do próprio framework (extras-control.js). Opções =
     * sugestões ATIVO da modalidade da sessão (mais os de uso geral) e os
     * itens já no roteiro; seleção (lado direito) = roteiro gravado,
     * sincronizado a cada Salvar Anotações.
     */
    private final PickList pickCondutas = new PickList("condutas", "Condutas (roteiro da sessão):");

    // Botões de transição e o permanente: criados uma única vez no construtor;
    // a situação da sessão define quais permanecem no Form (aplicarEstadoDaTela)
    private final Submit iniciarSubmit = new Submit("iniciar", "Iniciar Atendimento", this, "onIniciarClick");
    private final Submit salvarSubmit = new Submit("salvar", "Salvar Anotações", this, "onSalvarClick");
    private final Submit finalizarSubmit = new Submit("finalizar", "Finalizar Atendimento", this, "onFinalizarClick");
    private final Submit cancelarSubmit = new Submit("cancelar", "Cancelar", this, "onCancelClick");

    private final SessaoService sessaoService = new SessaoService();
    private final ProcedimentoService procedimentoService = new ProcedimentoService();

    /** Sessao carregada no onGet/onRender: base do estado da tela. */
    private Sessao sessao;

    public SessaoProntuarioEditPage() {
        form.setNoLayout(true);

        
        form.add(idField);
        form.add(prontuario);
        
        prontuario.add(registroRelato);
        prontuario.add(registroAvaliacao);
        prontuario.add(registroConduta);
        prontuario.add(pickCondutas);
        prontuario.add(registroOrientacoes);

        // Contexto do agendamento: exibição fixa, valor vindo do servidor
        registroRelato.setCols(60);
        registroRelato.setRows(4);
        registroAvaliacao.setCols(60);
        registroAvaliacao.setRows(4);
        registroConduta.setCols(60);
        registroConduta.setRows(5);
        registroOrientacoes.setCols(60);
        registroOrientacoes.setRows(3);

        // Picklist de condutas: cabeçalhos das duas listas em português
        // (esquerda = catálogo sugerido, direita = roteiro da sessão).
        pickCondutas.setHeaderLabel("Disponíveis:", "No roteiro:");

        // Opções via DataProvider: o PickList as busca quando as precisa
        // (bind do POST acontece ANTES do onRender; o render também).
        pickCondutas.setDataProvider(new DataProvider<Option>() {
            @Override
            public List<Option> getData() {
                return montarOpcoesDeCondutas();
            }
        });

        // Todos os botões existem desde a montagem do Form; o estado da tela
        // decide quais permanecem (remoção/re-inserção em aplicarEstadoDaTela)
        form.add(iniciarSubmit);
        form.add(salvarSubmit);
        form.add(finalizarSubmit);
        form.add(cancelarSubmit);
        addControl(form);
    }

    /**
     * Preparação do Form e suas dependências — sem carga de modelo (a Sessao
     * é carregada no onGet/onRender, fases de processamento do request).
     */
    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Atendimento");
    }

    /**
     * Abertura da sala (GET): carrega a Sessao — query única com a agenda E
     * o registro do atendimento — e popula o Form (HiddenField do id + textos
     * clínicos). O contexto do agendamento é exibido no quadro do template,
     * direto da $sessao publicada em onRender.
     */
    @Override
    public void onGet() {
        carregarSessao();

        if (sessao != null) {
            // Round-trip do id no POST: o Form só processa os filhos na
            // submissão, então o HiddenField precisa receber o valor aqui no GET.
            idField.setValueObject(sessao.getId());

            // Registro do atendimento: colunas da própria sessão
            registroRelato.setValue(sessao.getRegistroRelato());
            registroAvaliacao.setValue(sessao.getRegistroAvaliacao());
            registroConduta.setValue(sessao.getRegistroConduta());
            registroOrientacoes.setValue(sessao.getRegistroOrientacoes());
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
        // Modelos de exibição do template: o painel de conferência (AGENDADA)
        // desenha os dados direto da Sessao, com o horário planejado já
        // formatado pela página (formatação de dados, não lógica de negócio).
        if (sessao != null) {
            addModel("sessao", sessao);
            addModel("horario", formatarHorario(sessao.getDataHoraInicio()));
            addModel("horarioIso", formatarHorarioIso(sessao.getDataHoraInicio()));
        }
        popularCondutas();
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

        // Painel de conferência: sessão na fila e formulário ainda não aberto
        boolean painel = naFilaDaSala() && !exibirFormulario();
        // Anotações editáveis: formulário aberto com a sessão ativa
        boolean editavel = !painel && registroEditavel();

        registroRelato.setReadonly(!editavel);
        registroAvaliacao.setReadonly(!editavel);
        registroConduta.setReadonly(!editavel);
        registroOrientacoes.setReadonly(!editavel);
        pickCondutas.setReadonly(!editavel);

        if (painel) {
            form.add(iniciarSubmit);
        } else if (editavel) {
            form.add(salvarSubmit);
            if (podeFinalizar()) {
                form.add(finalizarSubmit);
            }
        }
        // Terminais (REALIZADA/FALTOU/CANCELADA): histórico — sem transições
        form.add(cancelarSubmit);
    }

    /** Sessão na fila da sala, ainda não atendida: AGENDADA ou CONFIRMADA. */
    private boolean naFilaDaSala() {
        return sessao != null
                && (sessao.getSituacao() == SessaoSituacao.AGENDADA
                    || sessao.getSituacao() == SessaoSituacao.CONFIRMADA);
    }

    /** O formulário do registro foi aberto? Navegação (?editar=true): nada persiste. */
    private boolean exibirFormulario() {
        return Boolean.TRUE.equals(editar);
    }

    /** Registro do atendimento editável: sessão na fila com o formulário
     * aberto, ou atendimento em andamento. */
    private boolean registroEditavel() {
        return naFilaDaSala() || atendimentoEmAndamento();
    }

    /** Anotações clínicas editáveis: atendimento em andamento. */
    private boolean atendimentoEmAndamento() {
        return sessao != null && sessao.getSituacao() == SessaoSituacao.EM_ANDAMENTO;
    }

    /**
     * Finalizar exige o registro da sessão com conduta registrada — mesmo
     * critério do guard do service (SessaoService.finalizarAtendimento).
     */
    private boolean podeFinalizar() {
        return atendimentoEmAndamento() && sessaoService.podeFinalizar(sessao.getId());
    }

    /** Formato do horário planejado no painel de conferência do template. */
    private static final DateTimeFormatter FORMATO_HORARIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Horário planejado formatado ("-" quando o dado não veio do banco). */
    private String formatarHorario(LocalDateTime dataHora) {
        return (dataHora != null) ? dataHora.format(FORMATO_HORARIO) : "-";
    }

    /** Formato ISO-8601 do horário, para o atributo datetime do elemento time. */
    private static final DateTimeFormatter FORMATO_HORARIO_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /** Horário planejado em ISO-8601 (atributo datetime do time no template). */
    private String formatarHorarioIso(LocalDateTime dataHora) {
        return (dataHora != null) ? dataHora.format(FORMATO_HORARIO_ISO) : null;
    }

    /**
     * Monta o delta do registro a partir da tela: somente as colunas
     * registro_* preenchidas pelo profissional; a transição marca os
     * registro_dt_* e o service preserva o restante da Sessao.
     */
    private Sessao montarRegistroDaTela() {
        Sessao registro = new Sessao();
        registro.setRegistroRelato(registroRelato.getValue());
        registro.setRegistroAvaliacao(registroAvaliacao.getValue());
        registro.setRegistroConduta(registroConduta.getValue());
        registro.setRegistroOrientacoes(registroOrientacoes.getValue());
        return registro;
    }

    /**
     * Seleção do picklist (estado de exibição, onRender): o roteiro
     * gravado. As OPÇÕES não são manipuladas aqui — o PickList as busca via
     * DataProvider (montarOpcoesDeCondutas) no momento em que as requer
     * (bind no POST, render). No ciclo POST o bind já aconteceu e o Salvar
     * sincronizou o roteiro: a tela volta a exibir o estado GRAVADO, não o
     * POSTado.
     */
    private void popularCondutas() {
        List<String> noRoteiro = new ArrayList<>();
        if (sessao != null) {
            for (SessaoProcedimento item : procedimentoService.getRoteiroDaSessao(sessao.getId())) {
                noRoteiro.add(item.getProcedimentoId().toString());
            }
        }
        pickCondutas.setSelectedValues(noRoteiro);
    }

    /**
     * Opções do picklist, buscadas pelo DataProvider do PickList no momento do
     * uso — bind do POST acontece ANTES do onRender e o render também precisa
     * delas, então não podem viver apenas no estado de exibição. São elas:
     * sugestões ATIVO da modalidade da sessão (mais as de uso geral) e os
     * itens já no roteiro que não estão nas sugestões — inclusive
     * procedimento INATIVO, que sai do catálogo ativo mas permanece no
     * roteiro gravado.
     */
    private List<Option> montarOpcoesDeCondutas() {
        List<Option> opcoes = new ArrayList<>();
        if (id == null) {
            return opcoes;
        }
        // O provider roda durante o processamento (bind/render) — fases em
        // que a carga da Sessao é sancionada. Em onGet ela já veio; aqui só
        // entra se o ciclo ainda não a carregou (ciclo POST).
        if (sessao == null) {
            carregarSessao();
        }
        if (sessao == null) {
            return opcoes; // erro de carga: nada a sugerir
        }

        Set<Integer> incluidos = new HashSet<>();
        Integer modalidadeId = (sessao.getModalidade() != null) ? sessao.getModalidade().getId() : null;
        for (Procedimento sugestao : procedimentoService.getSugestoesPorModalidade(modalidadeId)) {
            opcoes.add(new Option(sugestao.getId().toString(), sugestao.getNome()));
            incluidos.add(sugestao.getId());
        }

        // Itens do roteiro fora das sugestões: histórico do catálogo — entram
        // nas opções para não serem descartados pelo sync do Salvar.
        for (SessaoProcedimento item : procedimentoService.getRoteiroDaSessao(sessao.getId())) {
            if (!incluidos.contains(item.getProcedimentoId())) {
                boolean inativo = item.getProcedimento() != null
                        && item.getProcedimento().getSituacao() == ProcedimentoSituacao.INATIVO;
                opcoes.add(new Option(item.getProcedimentoId().toString(),
                        item.getProcedimento().getNome() + (inativo ? " (inativo)" : "")));
            }
        }
        return opcoes;
    }

    /** Condutas marcadas no picklist, como ids para o sync do service. */
    private List<Integer> condutasEscolhidas() {
        List<Integer> escolhidos = new ArrayList<>();
        for (String valor : pickCondutas.getSelectedValues()) {
            try {
                escolhidos.add(Integer.valueOf(valor));
            } catch (NumberFormatException ex) {
                // valor não numérico no request: não entra no roteiro
            }
        }
        return escolhidos;
    }

    /**
     * Iniciar Atendimento é NAVEGAÇÃO, não transição: nada é persistido —
     * abrir o formulário do registro não pode deixar resíduo se o
     * profissional desistir (Cancelar volta sem nada a desfazer). Redireciona
     * para a própria sala com editar=true; a transição para EM_ANDAMENTO
     * acontece na primeira gravação (SessaoService.salvarRegistro).
     */
    public boolean onIniciarClick() {
        if (id == null) {
            form.setError("Sessão não informada.");
            return true;
        }
        // Post Redirect: reabre a sala com o formulário do registro (?editar=true)
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("editar", Boolean.TRUE);
        setRedirect(SessaoProntuarioEditPage.class, params);
        return false;
    }

    /**
     * Salvar Anotações é a gravação real — e, na primeira gravação de uma
     * sessão da fila, é ELA que inicia o atendimento (service avança para
     * EM_ANDAMENTO e marca registro_dt_inicio). Grava também o roteiro: a
     * seleção do picklist de condutas vira sessao_procedimento (sync do
     * service). Re-render da mesma tela:
     * retorna true para o servlet seguir para onRender — retornar false
     * pularia o onRender e a tela sairia sem o estado aplicado (botões e
     * readonly das anotações). A Sessao em memória é recarregada: o onRender
     * aplica o estado da situação GRAVADA — com a conduta registrada, o
     * Finalizar aparece na mesma tela.
     */
    public boolean onSalvarClick() {
        if (id == null) {
            form.setError("Sessão não informada.");
            return true;
        }
        try {
            sessao = sessaoService.salvarRegistro(id, montarRegistroDaTela());
            // Roteiro (condutas) sincronizado com a seleção do picklist: o
            // que está marcado é o que ficou gravado.
            procedimentoService.sincronizarRoteiroDaSessao(id, condutasEscolhidas());
            addModel("mensagem", "Anotações salvas.");
        } catch (RuntimeException ex) {
            form.setError(ex.getMessage());
            return true;
        }
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
