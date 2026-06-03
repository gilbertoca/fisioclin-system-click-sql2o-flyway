package org.apache.click.showcase.fisio.model;

import java.io.Serializable;

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
}
