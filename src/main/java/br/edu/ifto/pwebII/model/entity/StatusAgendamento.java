package br.edu.ifto.pwebII.model.entity;

/**
 * Situação de um agendamento realizado por um paciente.
 */
public enum StatusAgendamento {
    AGENDADO("Agendado"),
    REALIZADO("Realizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
