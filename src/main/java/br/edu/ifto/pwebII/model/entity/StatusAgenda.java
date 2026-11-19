package br.edu.ifto.pwebII.model.entity;

/**
 * Situação de um horário cadastrado na agenda do médico.
 */
public enum StatusAgenda {
    DISPONIVEL("Disponível"),
    AGENDADO("Agendado");

    private final String descricao;

    StatusAgenda(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
