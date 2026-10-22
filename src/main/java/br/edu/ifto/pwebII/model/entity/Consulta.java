package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;

import br.edu.ifto.pwebII.validation.Edicao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entidade Consulta. Representa a tabela tb_consulta no banco de dados.
 *
 * Relacionamentos (conforme o diagrama de classes da atividade):
 * - Cada Consulta pertence obrigatoriamente a exatamente 1 Paciente
 *   (muitos para 1 -> @ManyToOne agindo como lado "dono").
 * - Cada Consulta é realizada obrigatoriamente por exatamente 1 Medico
 *   (muitos para 1 -> @ManyToOne agindo como lado "dono").
 */
@Entity
public class Consulta implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull(groups = Edicao.class, message = "O identificador deve ser informado para edição.")
    private Long id;

    @NotNull(message = "Informe a data e hora da consulta.")
    private LocalDateTime data;

    @NotNull(message = "Informe o valor da consulta.")
    @PositiveOrZero(message = "O valor da consulta deve ser maior ou igual a zero.")
    private Double valor;

    @Size(max = 500, message = "A observação deve ter no máximo {max} caracteres.")
    private String observacao;

    // Lado dono do relacionamento 0..* -> 1 (Paciente).
    // A FK "paciente_id" é persistida na tabela Consulta.
    // @OnDelete(CASCADE) gera a constraint com ON DELETE CASCADE no banco,
    // garantindo a exclusão em cascata mesmo fora do contexto JPA.
    // optional = false gera a coluna NOT NULL, impedindo consulta sem paciente.
    @NotNull(message = "Informe o paciente.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Paciente paciente;

    // Lado dono do relacionamento 0..* -> 1 (Medico).
    // A FK "medico_id" é persistida na tabela Consulta.
    // @OnDelete(CASCADE) gera a constraint com ON DELETE CASCADE no banco.
    // optional = false gera a coluna NOT NULL, impedindo consulta sem médico.
    @NotNull(message = "Informe o médico.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "medico_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Medico medico;


    public String dados() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "Consulta em " + (data != null ? data.format(fmt) : "sem data")
                + " | Valor: R$ " + valor
                + " | Obs: " + observacao
                + " | Paciente: " + (paciente != null ? paciente.getNome() : "-")
                + " | Médico: " + (medico != null ? medico.getNome() : "-");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }
}
