package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Paciente. Representa a tabela tb_paciente no banco de dados.
 *
 * Relacionamento (conforme o diagrama de classes da atividade):
 * Um {@code Paciente} pode estar associado a zero ou várias (1 -> 0..*)
 * instâncias de {@code Consulta}. O lado "donos" do relacionamento é a
 * própria Consulta (que guarda a FK), por isso usamos @OneToMany(mappedBy).
 */
@Entity
@PrimaryKeyJoinColumn(name = "id_pessoa_fisica")
public class Paciente extends PessoaFisica implements Serializable {

    // Lado fraco (não dono) do relacionamento 1 -> 0..*
    // O mappedBy indica que a FK está no atributo "paciente" de Consulta.
    // cascade=REMOVE + orphanRemoval: ao excluir o paciente, o JPA também
    // exclui as consultas associadas (evita violação de FK).
    @OneToMany(mappedBy = "paciente", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Consulta> consultas = new ArrayList<>();

    /**
     * Retorna uma String com a representação textual (dados básicos) do paciente.
     * Diferença de implementação: no material o método retornava apenas o nome;
     * aqui concatenamos nome e telefone conforme os atributos do diagrama.
     *
     * @return String com nome e telefone do paciente
     */
    public String dados() {
        return "Paciente: " + getNome() + " | Telefone: " + getTelefone();
    }

    /**
     * Retorna uma String com a lista textual de todas as consultas do paciente.
     * (Atende ao requisito: "Visualizar todas as consultas de um paciente")
     *
     * @return String com as consultas do paciente
     */
    public String consultas() {
        String texto = "Consultas de " + getNome() + ":\n";
        if (consultas == null || consultas.isEmpty()) {
            texto += "Nenhuma consulta cadastrada.";
        } else {
            for (Consulta c : consultas) {
                texto += " - " + c.dados() + "\n";
            }
        }
        return texto;
    }

    public List<Consulta> getConsultas() {
        return consultas;
    }

    public void setConsultas(List<Consulta> consultas) {
        this.consultas = consultas;
    }
}
