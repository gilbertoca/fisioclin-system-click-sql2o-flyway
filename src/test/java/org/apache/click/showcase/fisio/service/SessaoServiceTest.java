package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Modalidade;
import org.apache.click.showcase.fisio.model.Profissional;
import org.apache.click.showcase.fisio.model.Sessao;
import org.junit.Test;
import org.sql2o.Connection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.click.showcase.fisio.model.enums.PagamentoOrigem;
import org.apache.click.showcase.fisio.model.enums.SessaoStatus;
import org.apache.click.showcase.fisio.model.enums.SessaoTipo;
import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.BeforeClass;

public class SessaoServiceTest {

    private static SessaoService sessaoService;

    // IDs de referência persistidos no cenário base
    private static Integer clienteId;
    private static Integer idProfissional;
    private static Integer idModalidade;

    @BeforeClass
    public static void setUp() {
        // Inicializa banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_sessao_service_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();                
        // Instancia o Serviço passando diretamente o Sql2o (Sem a camada Repository)
        sessaoService = new SessaoService();

        seedCenarioOperacionalBase();
    }

    /**
     * Alimenta o banco com os Atores e Contextos obrigatórios para as FKs.
     */
    private static void seedCenarioOperacionalBase() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            
            // 1. Cadastra Cliente Ator
            clienteId = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, data_nascimento, telefone, status_clinico) " +
                    "VALUES ('Roberto Miranda', '98765432100', '1988-10-05', '869994455', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            // 2. Cadastra Profissional Ator
            idProfissional = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, crefito_ou_registro, telefone) VALUES ('Dr. Marcos Vinícius', 'CREFITO-999-F', '868882211')", true)
                    .executeUpdate().getKey(Integer.class);

            // 3. Cadastra Modalidade Descrição
            idModalidade = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, valor_base) VALUES ('RPG (Reeducação Postural)', 130.00)", true)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    @Test
    public void deveAgendarComSucessoERecuperarGrafoRicoDoBanco() {
        // Define um horário fixo livre de oscilações de milissegundos (Hoje às 14:00)
        LocalDateTime horarioDesejado = LocalDate.of(1993, 9, 22).atTime(14, 0);

        // Instancia os modelos ricos de domínio com seus respectivos IDs
        Cliente cliente = new Cliente(); cliente.setId(clienteId);
        Profissional profissional = new Profissional(); profissional.setId(idProfissional);
        Modalidade modalidade = new Modalidade(); modalidade.setId(idModalidade);

        // Monta o Grafo completo da Sessão
        Sessao sessao = new Sessao();
        sessao.setCliente(cliente);
        sessao.setProfissional(profissional);
        sessao.setModalidade(modalidade);
        sessao.setDataHoraInicio(horarioDesejado);
        sessao.setDataHoraFim(horarioDesejado.plusMinutes(50));
        sessao.setSessaoTipo(SessaoTipo.AVALIACAO_INICIAL);
        sessao.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessao.setSessaoStatus(SessaoStatus.AGENDADA);
        sessao.setObservacoesRecepcao("Teste de serviço unificado.");

        // 1. Executa a gravação direta pela camada Service
        sessaoService.create(sessao);

        // 2. Recupera a grade diária para validação
        List<Sessao> agenda = sessaoService.listarAgendaDoDia(LocalDate.of(1993, 9, 22));
        System.out.println(agenda);
        // 3. Asserts de Integridade
        assertEquals(1, agenda.size());
        Sessao sGravada = agenda.get(0);
        assertEquals(SessaoTipo.AVALIACAO_INICIAL, sGravada.getSessaoTipo());
        assertEquals(horarioDesejado, sGravada.getDataHoraInicio());

        // 4. Prova do Mapeamento de Grafo Rico (Múltiplos Níveis via dot-notation)
        assertNotNull(sGravada.getCliente());
        assertEquals("Roberto Miranda", sGravada.getCliente().getNome());

        assertNotNull(sGravada.getProfissional());
        assertEquals("Dr. Marcos Vinícius", sGravada.getProfissional().getNome());

        assertNotNull(sGravada.getModalidade());
        assertEquals("RPG (Reeducação Postural)", sGravada.getModalidade().getNome());
    }

    @Test
    public void deveBloquearAgendamentoSeHouverConflitoDeHorarioDoTerapeuta() {
        LocalDateTime horarioBase = LocalDate.of(1993, 9, 23).atTime(16, 0);

        Cliente c = new Cliente(); c.setId(clienteId);
        Profissional p = new Profissional(); p.setId(idProfissional);
        Modalidade m = new Modalidade(); m.setId(idModalidade);

        // Primeiro agendamento legítimo (16:00 às 16:50)
        Sessao sessao1 = new Sessao();
        sessao1.setCliente(c); sessao1.setProfissional(p); sessao1.setModalidade(m);
        sessao1.setDataHoraInicio(horarioBase);
        sessao1.setDataHoraFim(horarioBase.plusMinutes(50));
        sessao1.setSessaoTipo(SessaoTipo.TRATAMENTO_ROTINA);
        sessao1.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessao1.setSessaoStatus(SessaoStatus.AGENDADA);
        sessaoService.create(sessao1);

        // Segundo agendamento em conflito para o mesmo profissional (Interseção às 16:30)
        Sessao sessaoConflitante = new Sessao();
        sessaoConflitante.setCliente(c); sessaoConflitante.setProfissional(p); sessaoConflitante.setModalidade(m);
        sessaoConflitante.setDataHoraInicio(horarioBase.plusMinutes(30)); 
        sessaoConflitante.setDataHoraFim(horarioBase.plusMinutes(80));
        sessaoConflitante.setSessaoTipo(SessaoTipo.TRATAMENTO_ROTINA);
        sessaoConflitante.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessaoConflitante.setSessaoStatus(SessaoStatus.AGENDADA);

        try {
            // A camada service deve interceptar o conflito ativamente
            sessaoService.create(sessaoConflitante);
            fail("Deveria ter lançado IllegalStateException devido à colisão de horários.");
        } catch (IllegalStateException ex) {
            assertEquals("O profissional já possui atendimento neste horário.", ex.getMessage());
        }
    }

    @Test
    public void deveRejeitarAgendamentoSeOClienteNaoExistirNoBanco() {
        Cliente clienteInexistente = new Cliente();
        clienteInexistente.setId(999); // ID fantasma

        Profissional p = new Profissional(); p.setId(idProfissional);
        Modalidade m = new Modalidade(); m.setId(idModalidade);

        Sessao sessaoInvalida = new Sessao();
        sessaoInvalida.setCliente(clienteInexistente);
        sessaoInvalida.setProfissional(p);
        sessaoInvalida.setModalidade(m);
        sessaoInvalida.setDataHoraInicio(LocalDateTime.now());
        sessaoInvalida.setDataHoraFim(LocalDateTime.now().plusMinutes(50));
        sessaoInvalida.setSessaoTipo(SessaoTipo.TRATAMENTO_ROTINA);
        sessaoInvalida.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessaoInvalida.setSessaoStatus(SessaoStatus.AGENDADA);

        try {
            sessaoService.create(sessaoInvalida);
            fail("Deveria ter falhado devido à ausência do cliente no banco.");
        } catch (IllegalArgumentException ex) {
            assertEquals("Operação cancelada: O cliente informado não existe.", ex.getMessage());
        }
    }

    @AfterClass
    public static void tearDown() {
        DataSourceManager.shutdown();
    }
}
