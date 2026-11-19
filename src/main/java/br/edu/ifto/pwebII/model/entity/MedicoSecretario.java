package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

/**
 * Associação entre um Médico e um Secretário (Usuario com perfil SECRETARIO),
 * controlando individualmente as permissões concedidas pelo médico.
 */
@Entity
@Table(
        name = "medico_secretario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_medico_secretario",
                columnNames = {"medico_id", "secretario_id"}
        )
)
public class MedicoSecretario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Medico medico;

    @ManyToOne(optional = false)
    @JoinColumn(name = "secretario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario secretario;

    @Column(name = "pode_alterar_agenda", nullable = false)
    private boolean podeAlterarAgenda = false;

    @Column(name = "pode_alterar_preco", nullable = false)
    private boolean podeAlterarPreco = false;

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

    public Usuario getSecretario() {
        return secretario;
    }

    public void setSecretario(Usuario secretario) {
        this.secretario = secretario;
    }

    public boolean isPodeAlterarAgenda() {
        return podeAlterarAgenda;
    }

    public void setPodeAlterarAgenda(boolean podeAlterarAgenda) {
        this.podeAlterarAgenda = podeAlterarAgenda;
    }

    public boolean isPodeAlterarPreco() {
        return podeAlterarPreco;
    }

    public void setPodeAlterarPreco(boolean podeAlterarPreco) {
        this.podeAlterarPreco = podeAlterarPreco;
    }
}
