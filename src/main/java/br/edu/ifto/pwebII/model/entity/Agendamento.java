package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Representa a reserva de um horário disponível da agenda por um paciente.
 *
 * Usa @ManyToOne com Agenda para permitir que, após um cancelamento, o mesmo
 * horário liberado possa ser agendado novamente por outro paciente sem perder
 * o histórico do agendamento cancelado.
 */
@Entity
@Table(name = "agendamento")
public class Agendamento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe o horário da agenda.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "agenda_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Agenda agenda;

    @NotNull(message = "Informe o paciente.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    // Valor congelado no momento da reserva (snapshot do valorConsulta do médico).
    // Reajustes futuros no valor do médico não alteram agendamentos já marcados.
    @Column(nullable = false)
    private Double valor = 0.0;

    @Column(name = "data_agendamento", nullable = false)
    private LocalDateTime dataAgendamento = LocalDateTime.now();

    @OneToOne(mappedBy = "agendamento")
    private Consulta consulta;

    public boolean isAgendado() {
        return status == StatusAgendamento.AGENDADO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Agenda getAgenda() {
        return agenda;
    }

    public void setAgenda(Agenda agenda) {
        this.agenda = agenda;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataAgendamento() {
        return dataAgendamento;
    }

    public void setDataAgendamento(LocalDateTime dataAgendamento) {
        this.dataAgendamento = dataAgendamento;
    }

    public Consulta getConsulta() {
        return consulta;
    }

    public void setConsulta(Consulta consulta) {
        this.consulta = consulta;
    }
}
