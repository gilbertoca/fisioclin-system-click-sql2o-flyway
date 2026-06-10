package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.time.LocalDate; // Alterado para LocalDate (Apenas Data)
import java.util.Objects;

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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.id);
        hash = 13 * hash + Objects.hashCode(this.nome);
        hash = 13 * hash + Objects.hashCode(this.cpf);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cliente other = (Cliente) obj;
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        if (!Objects.equals(this.cpf, other.cpf)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cliente{");
        sb.append("id=").append(id);
        sb.append(", nome=").append(nome);
        sb.append(", cpf=").append(cpf);
        sb.append(", dataNascimento=").append(dataNascimento);
        sb.append(", telefone=").append(telefone);
        sb.append(", statusClinico=").append(statusClinico);
        sb.append(", convenio=").append(convenio);
        sb.append('}');
        return sb.toString();
    }
    
}
