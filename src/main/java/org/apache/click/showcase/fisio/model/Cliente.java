package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.time.LocalDate; // Alterado para LocalDate (Apenas Data)

public class Cliente implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    private String cpf;
    private LocalDate dataNascimento;
    private String telefone;
    private String statusClinico;
    private Convenio convenio;

    public Cliente() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getStatusClinico() { return statusClinico; }
    public void setStatusClinico(String statusClinico) { this.statusClinico = statusClinico; }
    public Convenio getConvenio() { return convenio; }
    public void setConvenio(Convenio convenio) { this.convenio = convenio; }
    public Integer getConvenioId() { return this.convenio != null ? this.convenio.getId() : null; }
    
}
