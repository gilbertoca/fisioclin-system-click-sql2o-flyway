package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Profissional;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.enums.PagamentoOrigem;
import com.gilbertoca.fisio.model.enums.SessaoTipo;
import com.gilbertoca.fisio.service.ModalidadeService;
import com.gilbertoca.fisio.service.SessaoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.DateField;
import org.apache.click.util.Bindable;

/**
 * Formulário de agendamento de sessões (fisioterapia, pilates, massagem).
 * Monta o grafo Sessao -> Cliente/Profissional/Modalidade e persiste via
 * SessaoService, que garante a verificação transacional de conflito de agenda.
 */
public class SessaoEditPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    // Track internal persistence scope state
    private final HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable protected Integer id;

    final Form form = new Form("form");
    final Select selectCliente = new Select("selectCliente", "Cliente:", true);
    final Select selectProfissional = new Select("selectProfissional", "Profissional:", true);
    final Select selectModalidade = new Select("selectModalidade", "Modalidade:", true);
    final DateField campoData = new DateField("campoData", "Data:", true);
    final TextField selectHorario = new TextField("selectHorario", "Horário (HH:mm):", true);
    final Select selectTipo = new Select("selectTipo", "Tipo de Sessão:", true);
    final Select selectPagamentoOrigem = new Select("selectPagamentoOrigem", "Origem do Pagamento:", true);

    private final SessaoService sessaoService = new SessaoService();
    private final ModalidadeService modalidadeService = new ModalidadeService();

    public SessaoEditPage() {
        form.setNoLayout(true);

        form.add(idField);
        form.add(selectCliente);
        form.add(selectProfissional);
        form.add(selectModalidade);
        form.add(campoData);
        form.add(selectHorario);
        form.add(selectTipo);
        form.add(selectPagamentoOrigem);
        form.add(new Submit("save", "Salvar", this, "onSalvarClick"));
        form.add(new Submit("cancel", "Cancelar", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Sessão Edit");

        selectCliente.getOptionList().clear();
        selectCliente.add(Option.EMPTY_OPTION);
        for (Cliente cliente : sessaoService.getAllClientes()) {
            selectCliente.add(new Option(cliente.getId().toString(), cliente.getNome()));
        }

        selectProfissional.getOptionList().clear();
        selectProfissional.add(Option.EMPTY_OPTION);
        for (Profissional profissional : sessaoService.getAllProfissionais()) {
            selectProfissional.add(new Option(profissional.getId().toString(), profissional.getNome()));
        }

        selectModalidade.getOptionList().clear();
        selectModalidade.add(Option.EMPTY_OPTION);
        for (Modalidade modalidade : sessaoService.getAllModalidades()) {
            selectModalidade.add(new Option(modalidade.getId().toString(), modalidade.getNome()));
        }

        selectTipo.getOptionList().clear();
        selectTipo.add(Option.EMPTY_OPTION);
        for (SessaoTipo tipo : SessaoTipo.values()) {
            selectTipo.add(new Option(tipo.name(), tipo.toString()));
        }

        selectPagamentoOrigem.getOptionList().clear();
        selectPagamentoOrigem.add(Option.EMPTY_OPTION);
        for (PagamentoOrigem origem : PagamentoOrigem.values()) {
            selectPagamentoOrigem.add(new Option(origem.name(), origem.toString()));
        }
    }

    @Override
    public void onGet() {
        if (id != null) {
            Sessao sessao = sessaoService.get(id);
            if (sessao != null) {
                idField.setValueObject(sessao.getId());
                if (sessao.getCliente() != null) {
                    selectCliente.setValue(sessao.getCliente().getId().toString());
                }
                if (sessao.getProfissional() != null) {
                    selectProfissional.setValue(sessao.getProfissional().getId().toString());
                }
                if (sessao.getModalidade() != null) {
                    selectModalidade.setValue(sessao.getModalidade().getId().toString());
                }
                if (sessao.getDataHoraInicio() != null) {
                    LocalDate data = sessao.getDataHoraInicio().toLocalDate();
                    campoData.setDate(Date.from(data.atStartOfDay(ZONE).toInstant()));
                    selectHorario.setValue(sessao.getDataHoraInicio().toLocalTime().toString());
                }
                if (sessao.getSessaoTipo() != null) {
                    selectTipo.setValue(sessao.getSessaoTipo().name());
                }
                if (sessao.getPagamentoOrigem() != null) {
                    selectPagamentoOrigem.setValue(sessao.getPagamentoOrigem().name());
                }
            }
        }
    }

    public boolean onSalvarClick() {
        if (!form.isValid()) {
            return true;
        }

        Integer sessaoId = id;
        Sessao sessao = (sessaoId != null) ? sessaoService.get(sessaoId) : new Sessao();
        if (sessao == null) {
            form.setError("Sessão informada não existe mais.");
            return true;
        }

        LocalDate data;
        LocalTime hora;
        try {
            data = campoData.getDate().toInstant().atZone(ZONE).toLocalDate();
            hora = LocalTime.parse(selectHorario.getValue().trim());
        } catch (RuntimeException ex) {
            form.setError("Data ou horário inválidos. Use o formato HH:mm.");
            return true;
        }

        Integer clienteId = parseId(selectCliente.getValue());
        Integer profissionalId = parseId(selectProfissional.getValue());
        Integer modalidadeId = parseId(selectModalidade.getValue());
        if (clienteId == null || profissionalId == null || modalidadeId == null) {
            form.setError("Informe cliente, profissional e modalidade.");
            return true;
        }

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        sessao.setCliente(cliente);

        Profissional profissional = new Profissional();
        profissional.setId(profissionalId);
        sessao.setProfissional(profissional);

        Modalidade modalidade = new Modalidade();
        modalidade.setId(modalidadeId);
        sessao.setModalidade(modalidade);

        int duracaoMinutos = 50; // fallback consistente com o default do banco
        Modalidade modalidadeCarregada = modalidadeService.get(modalidadeId);
        if (modalidadeCarregada != null && modalidadeCarregada.getDuracaoMinutos() > 0) {
            duracaoMinutos = modalidadeCarregada.getDuracaoMinutos();
        }

        LocalDateTime inicio = LocalDateTime.of(data, hora);
        sessao.setDataHoraInicio(inicio);
        sessao.setDataHoraFim(inicio.plusMinutes(duracaoMinutos));

        if (sessao.getSessaoTipo() == null) {
            String tipo = selectTipo.getValue();
            sessao.setSessaoTipo((tipo != null && !tipo.isEmpty())
                    ? SessaoTipo.valueOf(tipo) : SessaoTipo.AVALIACAO);
        }
        if (sessao.getPagamentoOrigem() == null) {
            String origem = selectPagamentoOrigem.getValue();
            sessao.setPagamentoOrigem((origem != null && !origem.isEmpty())
                    ? PagamentoOrigem.valueOf(origem) : PagamentoOrigem.PARTICULAR);
        }

        try {
            if (sessaoId != null) {
                sessaoService.update(sessao);
            } else {
                sessaoService.create(sessao);
            }
        } catch (IllegalStateException ex) {
            form.setError(ex.getMessage());
            return true;
        }

        setRedirect(SessaoViewPage.class);
        return false;
    }

    public boolean onCancelClick() {
        setRedirect(SessaoViewPage.class);
        return false;
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
