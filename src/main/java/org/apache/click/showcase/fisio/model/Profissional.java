package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.util.Objects;

public class Profissional implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    private String crefitoOuRegistro;
    private String telefone;

    public Profissional() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCrefitoOuRegistro() { return crefitoOuRegistro; }
    public void setCrefitoOuRegistro(String crefitoOuRegistro) { this.crefitoOuRegistro = crefitoOuRegistro; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.id);
        hash = 71 * hash + Objects.hashCode(this.nome);
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
        final Profissional other = (Profissional) obj;
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Profissional{");
        sb.append("id=").append(id);
        sb.append(", nome=").append(nome);
        sb.append(", crefitoOuRegistro=").append(crefitoOuRegistro);
        sb.append(", telefone=").append(telefone);
        sb.append('}');
        return sb.toString();
    }
    
}
