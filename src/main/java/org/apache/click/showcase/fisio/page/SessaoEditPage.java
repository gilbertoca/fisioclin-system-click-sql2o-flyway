package org.apache.click.showcase.fisio.page;

import org.apache.click.control.*;
import org.apache.click.extras.control.DateField;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Modalidade;
import org.apache.click.showcase.fisio.model.Profissional;
import org.apache.click.showcase.fisio.model.Sessao;
import org.apache.click.showcase.fisio.service.SessaoService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.apache.click.showcase.fisio.model.enums.PagamentoOrigem;
import org.apache.click.showcase.fisio.model.enums.SessaoStatus;
import org.apache.click.showcase.fisio.model.enums.SessaoTipo;

public class SessaoEditPage extends LayoutPage {
    private static final long serialVersionUID = 1L;

    protected HiddenField fieldId = new HiddenField("id", Integer.class);
    
    protected Form form = new Form("form");
    protected Select selectCliente = new Select("clienteId", "Cliente:", true);
    protected Select selectProfissional = new Select("profissionalId", "Fisioterapeuta:", true);
    protected Select selectModalidade = new Select("modalidadeId", "Modalidade/Serviço:", true);
    protected DateField campoData = new DateField("dataSessao", "Data do Atendimento:", true);
    protected Select selectHorario = new Select("horarioSessao", "Horário:", true);
    protected Select selectStatus = new Select("sessaoStatus", "Status:", true);
    protected TextField campoObservacoes = new TextField("observacoesRecepcao", "Observações:");

    protected Submit botaoSalvar = new Submit("salvar", "Confirmar Agendamento", this, "onSalvarClick");
    protected PageLink linkCancelar = new PageLink("linkCancelar", "Voltar", SessaoViewPage.class);

    private SessaoService sessaoService;

    public SessaoEditPage() {
        form.add(fieldId);
        form.add(selectCliente);
        form.add(selectProfissional);
        form.add(selectModalidade);
        form.add(campoData);
        form.add(selectHorario);
        form.add(selectStatus);
        form.add(campoObservacoes);
        form.add(botaoSalvar);
        form.add(linkCancelar);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        campoData.setFormatPattern("dd/MM/yyyy");

        if (sessaoService == null) return;

        // Carga dinâmica dos seletores via helpers do modelo rico
        selectCliente.getOptionList().clear();
        selectCliente.add(new Option("", "-- Selecione o Paciente --"));
        for (Cliente c : sessaoService.getAllClientes()) {
            selectCliente.add(new Option(c.getId().toString(), c.getNome()));
        }

        selectProfissional.getOptionList().clear();
        selectProfissional.add(new Option("", "-- Selecione o Profissional --"));
        for (Profissional p : sessaoService.getAllProfissionais()) {
            selectProfissional.add(new Option(p.getId().toString(), p.getNome()));
        }

        selectModalidade.getOptionList().clear();
        selectModalidade.add(new Option("", "-- Selecione a Modalidade --"));
        for (Modalidade m : sessaoService.getAllModalidades()) {
            selectModalidade.add(new Option(m.getId().toString(), m.getNome()));
        }

        selectHorario.getOptionList().clear();
        selectHorario.add(new Option("08:00", "08:00 AM"));
        selectHorario.add(new Option("09:00", "09:00 AM"));
        selectHorario.add(new Option("14:00", "02:00 PM"));
        selectHorario.add(new Option("15:00", "03:00 PM"));

        selectStatus.getOptionList().clear();
        selectStatus.add(new Option("AGENDADA", "Agendada"));
        selectStatus.add(new Option("CONFIRMADA", "Confirmada"));
        selectStatus.add(new Option("REALIZADA", "Realizada"));
        selectStatus.add(new Option("FALTOU", "Falta"));
        selectStatus.add(new Option("CANCELADA", "Cancelada"));

        // Preenchimento em caso de Edição
        String idParam = getContext().getRequestParameter("id");
        if (idParam != null && !idParam.trim().isEmpty() && !form.isFormSubmission()) {
            Sessao target = sessaoService.get(Integer.valueOf(idParam));
            if (target != null) {
                fieldId.setValue(target.getId().toString());
                selectCliente.setValue(target.getCliente().getId().toString());
                selectProfissional.setValue(target.getProfissional().getId().toString());
                selectModalidade.setValue(target.getModalidade().getId().toString());
                selectStatus.setValue(target.getSessaoStatus().toString());
                campoObservacoes.setValue(target.getObservacoesRecepcao());

                if (target.getDataHoraInicio() != null) {
                    campoData.setDate(Date.from(target.getDataHoraInicio().atZone(ZoneId.systemDefault()).toInstant()));
                    String horaMin = target.getDataHoraInicio().toLocalTime().toString().substring(0, 5);
                    selectHorario.setValue(horaMin);
                }
            }
        }
    }

    public boolean onSalvarClick() {
        if (form.isValid()) {
            try {
                Date dataSelecionada = campoData.getDate();
                String[] hm = selectHorario.getValue().split(":");
                LocalDateTime inicio = dataSelecionada.toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                        .atTime(Integer.parseInt(hm[0]), Integer.parseInt(hm[1]));

                // Constrói o Grafo de Objetos Rico
                Sessao s = new Sessao();
                s.setDataHoraInicio(inicio);
                s.setDataHoraFim(inicio.plusMinutes(50));
                s.setSessaoStatus(SessaoStatus.valueOf(selectStatus.getValue()));
                s.setObservacoesRecepcao(campoObservacoes.getValue());
                s.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
                s.setSessaoTipo(SessaoTipo.TRATAMENTO_ROTINA);

                Cliente c = new Cliente(); c.setId(Integer.valueOf(selectCliente.getValue())); s.setCliente(c);
                Profissional p = new Profissional(); p.setId(Integer.valueOf(selectProfissional.getValue())); s.setProfissional(p);
                Modalidade m = new Modalidade(); m.setId(Integer.valueOf(selectModalidade.getValue())); s.setModalidade(m);

                String activeId = fieldId.getValue();
                if (activeId != null && !activeId.trim().isEmpty()) {
                    s.setId(Integer.valueOf(activeId));
                    sessaoService.update(s);
                } else {
                    sessaoService.create(s);
                }

                setRedirect(SessaoViewPage.class);
                return false;
            } catch (Exception ex) {
                form.setError("Conflito detectado: " + ex.getMessage());
            }
        }
        return true;
    }
}
