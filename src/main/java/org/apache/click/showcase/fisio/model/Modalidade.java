package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Modalidade implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    private int duracaoMinutos;
    private BigDecimal valorBase;

    public Modalidade() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public int getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(int duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public BigDecimal getValorBase() { return valorBase; }
    public void setValorBase(BigDecimal valorBase) { this.valorBase = valorBase; }
}
