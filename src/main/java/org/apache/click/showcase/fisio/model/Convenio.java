package org.apache.click.showcase.fisio.model;

import java.io.Serializable;

public class Convenio implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    private String cnpj;

    public Convenio() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    @Override
    public String toString() {
        return "Convenio{" + "id=" + id + ", nome=" + nome + ", cnpj=" + cnpj + '}';
    }  
    
}
