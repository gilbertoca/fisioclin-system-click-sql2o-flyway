package org.apache.click.showcase.fisio.page;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.showcase.fisio.model.Sessao;
import org.apache.click.showcase.fisio.service.SessaoService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SessaoViewPage extends LayoutPage {
    private static final long serialVersionUID = 1L;

    protected Table table = new Table("table");
    protected PageLink linkNew = new PageLink("linkNew", "Novo Agendamento", SessaoEditPage.class);
    protected ActionLink linkDelete = new ActionLink("linkDelete", "Excluir", this, "onDeleteClick");

    private final SessaoService sessaoService = new SessaoService();

    public SessaoViewPage() {
        Column colHorario = new Column("dataHoraInicio", "Horário");
        colHorario.setDecorator((row, context) -> {
            Sessao s = (Sessao) row;
            return s.getDataHoraInicio() != null ? s.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        });
        table.addColumn(colHorario);

        Column colPaciente = new Column("cliente", "Paciente");
        colPaciente.setDecorator((row, context) -> {
            Sessao s = (Sessao) row;
            return s.getCliente() != null ? s.getCliente().getNome() : "";
        });
        table.addColumn(colPaciente);

        Column colProf = new Column("profissional", "Fisioterapeuta");
        colProf.setDecorator((row, context) -> {
            Sessao s = (Sessao) row;
            return s.getProfissional() != null ? s.getProfissional().getNome() : "";
        });
        table.addColumn(colProf);

        Column colMod = new Column("modalidade", "Serviço");
        colMod.setDecorator((row, context) -> {
            Sessao s = (Sessao) row;
            return s.getModalidade() != null ? s.getModalidade().getNome() : "";
        });
        table.addColumn(colMod);

        table.addColumn(new Column("sessaoTipo", "Objetivo"));
        table.addColumn(new Column("sessaoStatus", "Status"));

        Column colActions = new Column("acoes", "Ações");
        colActions.setDecorator((row, context) -> {
            Sessao s = (Sessao) row;
            String idStr = s.getId().toString();
            String editUrl = context.getPagePath(SessaoEditPage.class) + "?id=" + idStr;
            linkDelete.setValue(idStr);
            return "<a href='" + editUrl + "' style='margin-right: 10px;'>Editar</a>" + linkDelete.toString();
        });
        table.addColumn(colActions);

        addControl(table);
        addControl(linkNew);
        addControl(linkDelete);
    }

    public boolean onDeleteClick() {
        Integer targetId = linkDelete.getValueInteger();
        if (targetId != null) {
            sessaoService.delete(targetId);
        }
        return true; 
    }

    @Override
    public void onRender() {
        super.onRender();
        table.setRowList(sessaoService.listarAgendaDoDia(LocalDate.now()));
    }
}
