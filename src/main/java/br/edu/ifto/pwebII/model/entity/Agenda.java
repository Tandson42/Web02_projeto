package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Representa a disponibilidade de um horário na agenda de um médico da clínica.
 */
@Entity
@Table(
        name = "agenda",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agenda_medico_data_horario",
                columnNames = {"medico_id", "data", "horario"}
        )
)
public class Agenda implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe o médico.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Medico medico;

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate data;

    @NotNull(message = "Informe o horário.")
    @DateTimeFormat(pattern = "HH:mm")
    @Column(nullable = false)
    private LocalTime horario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgenda status = StatusAgenda.DISPONIVEL;

    // Lock otimista: evita que dois agendamentos simultâneos reservem o mesmo horário.
    @Version
    private Long version = 0L;

    public LocalDateTime getDataHora() {
        if (data == null || horario == null) {
            return null;
        }
        return LocalDateTime.of(data, horario);
    }

    public boolean isDisponivel() {
        return status == StatusAgenda.DISPONIVEL;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public StatusAgenda getStatus() {
        return status;
    }

    public void setStatus(StatusAgenda status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
