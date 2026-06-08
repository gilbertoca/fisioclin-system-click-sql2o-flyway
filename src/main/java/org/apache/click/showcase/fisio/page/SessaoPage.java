package org.apache.click.showcase.fisio.page;

import org.apache.click.control.*;
import org.apache.click.extras.control.DateField;
import org.apache.click.showcase.fisio.model.Sessao;
import org.apache.click.showcase.fisio.service.SessaoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Modalidade;
import org.apache.click.showcase.fisio.model.Profissional;

public class SessaoPage extends LayoutPage {
    private static final long serialVersionUID = 1L;

    // Componentes de Controle de Estado (Modo Visão vs Modo Edição)
    public boolean modoFormulario = false;

    // Componentes da Grade (Grid)
    protected Table tabelaAgenda = new Table("tabelaAgenda");
    protected ActionLink linkNovo = new ActionLink("linkNovo", "Novo Agendamento", this, "onNovoClick");

    // Componentes do Formulário (Form)
    protected Form formAgendamento = new Form("formAgendamento");
    protected Select selectCliente = new Select("idCliente", "Cliente:", true);
    protected Select selectProfissional = new Select("idProfissional", "Fisioterapeuta:", true);
    protected Select selectModalidade = new Select("idModalidade", "Modalidade:", true);
    protected DateField campoData = new DateField("dataSessao", "Data:", true);
    protected Select selectHorario = new Select("horarioSessao", "Horário:", true);
    protected Submit botaoSalvar = new Submit("salvar", "Confirmar", this, "onSalvarClick");
    protected ActionLink linkCancelar = new ActionLink("linkCancelar", "Cancelar", this, "onCancelarClick");

    private SessaoService sessaoService;

    public SessaoPage() {
        // Configura Tabela
        tabelaAgenda.setAttribute("class", "pure-table pure-table-horizontal");
        tabelaAgenda.addColumn(new Column("statusSessao", "Status"));
        addControl(tabelaAgenda);
        addControl(linkNovo);

        // Configura Formulário unificado
        formAgendamento.setAttribute("class", "pure-form pure-form-stacked");
        formAgendamento.add(selectCliente);
        formAgendamento.add(selectProfissional);
        formAgendamento.add(selectModalidade);
        formAgendamento.add(campoData);
        formAgendamento.add(selectHorario);
        formAgendamento.add(botaoSalvar);
        formAgendamento.add(linkCancelar);
        addControl(formAgendamento);
    }

    @Override
    public void onInit() {
        super.onInit();
        
        // Apply Pure.css style decorations to layout controls
        linkNovo.setAttribute("class", "pure-button pure-button-primary");
        linkCancelar.setAttribute("class", "pure-button");
        botaoSalvar.setAttribute("class", "pure-button pure-button-primary");
        campoData.setFormatPattern("dd/MM/yyyy");

        // Guard clause to prevent rendering errors if service context is missing
        if (sessaoService == null) {
            return;
        }

        // 1. Populate Clientes Combobox
        //selectCliente.removeAllOptions();
        selectCliente.add(new Option("", "-- Select Patient --"));
        for (Cliente c : sessaoService.getAllClientes()) {
            selectCliente.add(new Option(c.getId().toString(), c.getNome()));
        }

        // 2. Populate Profissionais Combobox
        //selectProfissional.removeAllOptions();
        selectProfissional.add(new Option("", "-- Select Therapist --"));
        for (Profissional p : sessaoService.getAllProfissionais()) {
            selectProfissional.add(new Option(p.getId().toString(), p.getNome()));
        }

        // 3. Populate Modalidades Combobox
        //selectModalidade.removeAllOptions();
        selectModalidade.add(new Option("", "-- Select Service --"));
        for (Modalidade m : sessaoService.getAllModalidades()) {
            selectModalidade.add(new Option(m.getId().toString(), m.getNome()));
        }

        // 4. Populate Static Standard Time Slots
        //selectHorario.removeAllOptions();
        selectHorario.add(new Option("08:00", "08:00 AM"));
        selectHorario.add(new Option("09:00", "09:00 AM"));
        selectHorario.add(new Option("10:00", "10:00 AM"));
        selectHorario.add(new Option("14:00", "02:00 PM"));
        selectHorario.add(new Option("15:00", "03:00 PM"));
    }


    public boolean onNovoClick() {
        this.modoFormulario = true;
        return true;
    }

    public boolean onCancelarClick() {
        this.modoFormulario = false;
        return true;
    }

    public boolean onSalvarClick() {
        if (formAgendamento.isValid()) {
            try {
                Date dataSel = campoData.getDate();
                String[] hm = selectHorario.getValue().split(":");
                LocalDateTime inicio = dataSel.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .atTime(Integer.parseInt(hm[0]), Integer.parseInt(hm[1]));

                Sessao s = new Sessao();
                //s.setCliente(selectCliente.getValue());
                //s.setProfissional(selectProfissional.getValue());
                //s.setModalidade(selectModalidade.getValue());
                s.setDataHoraInicio(inicio);
                s.setDataHoraFim(inicio.plusMinutes(50));
                s.setTipoSessao("AVALIACAO_INICIAL");
                s.setTipoPagamento("PARTICULAR");

                sessaoService.create(s);
                
                this.modoFormulario = false; // Retorna para a grade
                return false;
            } catch (IllegalStateException ex) {
                formAgendamento.setError(ex.getMessage());
            }
        }
        return true;
    }

    @Override
    public void onRender() {
        super.onRender();
        if (!modoFormulario && sessaoService != null) {
            tabelaAgenda.setRowList(sessaoService.listarAgendaDoDia(LocalDate.now()));
        }
    }
}
