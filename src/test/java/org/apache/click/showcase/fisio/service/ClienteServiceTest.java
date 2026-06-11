package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Convenio;
import org.junit.Test;
import org.sql2o.Connection;

import java.time.LocalDate;
import java.util.List;
import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.BeforeClass;

public class ClienteServiceTest {

    private static DataSourceManager dsManager;
    private static ClienteService clienteService;
    
    // IDs de planos de saúde criados para o cenário de teste
    private static Integer convenioIdUnimed;

    @BeforeClass
    public static void setUp() {
        // Inicialização do banco em memória isolado para os testes de cliente
        String jdbcUrl = "jdbc:h2:mem:fisio_cliente_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        dsManager = new DataSourceManager(jdbcUrl, "sa", "", "org.h2.Driver");
        
        QueryLoader queryLoader = new QueryLoader("queries.properties");
        
        // Instanciação direta do Serviço Unificado (Sem camadas intermediárias de Repository)
        clienteService = new ClienteService(dsManager.getSql2o(), queryLoader);

    
       //Insere os dados básicos de convênio para testar a associação do grafo rico.


        try (Connection conn = dsManager.getSql2o().beginTransaction()) {
            convenioIdUnimed = conn.createQuery("INSERT INTO convenio (nome, cnpj) VALUES ('Unimed Teresina', '12345678000199')", true)
                    .executeUpdate()
                    .getKey(Integer.class);
            conn.commit();
        }
    }

    @Test
    public void deveExecutarCicloCompletoDeCrudDoClienteComGrafoRico() {
        // ------------------------------------------------------------------------
        // 1. TESTE DO OPERAÇÃO: CREATE (Com Convênio atrelado)
        // ------------------------------------------------------------------------
        Convenio planoUnimed = new Convenio();
        planoUnimed.setId(convenioIdUnimed);

        Cliente novoCliente = new Cliente();
        novoCliente.setNome("Mariana Costa Lima");
        novoCliente.setCpf("55566677788");
        novoCliente.setDataNascimento(LocalDate.of(1993, 9, 22));
        novoCliente.setTelefone("8699994455");
        novoCliente.setStatusClinico("ATIVO");
        novoCliente.setConvenio(planoUnimed); // Vincula o objeto rico de domínio

        clienteService.create(novoCliente);

        // ------------------------------------------------------------------------
        // 2. TESTE DA OPERAÇÃO: GETALL & GET (Verifica mapeamento dot-notation)
        // ------------------------------------------------------------------------
        List<Cliente> todosClientes = clienteService.getAll();
        assertNotNull("A listagem não deve ser nula", todosClientes);
        assertEquals("Deve conter exatamente 1 cliente salvo", 1, todosClientes.size());

        // Captura o ID gerado pelo banco para fazer a busca direta
        Integer idGerado = todosClientes.get(0).getId();
        
        Cliente clientePersistido = clienteService.get(idGerado);
        assertNotNull("O cliente deveria ter sido encontrado", clientePersistido);
        assertEquals("Mariana Costa Lima", clientePersistido.getNome());
        assertEquals("55566677788", clientePersistido.getCpf());
        assertEquals(LocalDate.of(1993, 9, 22), clientePersistido.getDataNascimento());

        // PROVA DO GRAFO ANINHADO: O Sql2o preencheu o convênio interno do cliente de forma transparente
        assertNotNull("O objeto Convenio interno não deveria estar nulo", clientePersistido.getConvenio());
        assertEquals(convenioIdUnimed, clientePersistido.getConvenio().getId());
        assertEquals("Unimed Teresina", clientePersistido.getConvenio().getNome());

        // ------------------------------------------------------------------------
        // 3. TESTE DA OPERAÇÃO: UPDATE (Modificação de dados básicos)
        // ------------------------------------------------------------------------
        clientePersistido.setNome("Mariana Costa Lima Refatorada");
        clientePersistido.setStatusClinico("INATIVO");
        
        clienteService.update(clientePersistido);
        
        Cliente clienteModificado = clienteService.get(idGerado);
        assertEquals("Mariana Costa Lima Refatorada", clienteModificado.getNome());
        assertEquals("INATIVO", clienteModificado.getStatusClinico());

        // ------------------------------------------------------------------------
        // 4. TESTE DA OPERAÇÃO: DELETE (Remoção física)
        // ------------------------------------------------------------------------
        clienteService.delete(idGerado);
        
        Cliente clienteDeletado = clienteService.get(idGerado);
        assertNull("O cliente deveria ter sido completamente apagado do banco", clienteDeletado);
    }

    @Test
    public void deveBuscarClientesFiltandoPeloPadraoLikeNoNome() {
        // Insere 2 clientes distintos para testar a busca textual
        try (Connection conn = dsManager.getSql2o().open()) {
            conn.createQuery("INSERT INTO cliente (nome, cpf, data_nascimento, telefone, status_clinico) VALUES ('Carlos Silva', '111', '1985-04-12', '99', 'ATIVO')").executeUpdate();
            conn.createQuery("INSERT INTO cliente (nome, cpf, data_nascimento, telefone, status_clinico) VALUES ('Ana Beatriz', '222', '1978-11-05', '88', 'ATIVO')").executeUpdate();
        }

        // Executa a busca enviando apenas um fragmento do nome ("silva") em letras minúsculas
        List<Cliente> resultadoFiltro = clienteService.getAllLikeNome("silva");
        
        assertEquals(1, resultadoFiltro.size());
        assertEquals("Carlos Silva", resultadoFiltro.get(0).getNome());
    }

    @Test
    public void deveValidarOsMetodosAuxiliaresDeCargaDeDropdowns() {
        // Valida se o método helper do serviço consegue ler a tabela de convênios para alimentar os combos
        List<Convenio> listaParaCombos = clienteService.getAllConvenios();
        System.out.println(listaParaCombos);
        assertEquals(1, listaParaCombos.size());
        assertEquals("Unimed Teresina", listaParaCombos.get(0).getNome());

        Convenio buscaIndividual = clienteService.getConvenioById(convenioIdUnimed);
        assertNotNull(buscaIndividual);
        assertEquals("Unimed Teresina", buscaIndividual.getNome());
    }

    @AfterClass
    public static void tearDown() {
        if (dsManager != null) {
            dsManager.close();
        }
    }
}
