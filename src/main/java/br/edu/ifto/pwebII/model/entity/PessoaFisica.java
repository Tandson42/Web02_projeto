package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@PrimaryKeyJoinColumn(name = "id_pessoa")
public class PessoaFisica extends Pessoa {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 100, message = "O nome deve ter no máximo {max} caracteres.")
    private String nome;

    @NotBlank(message = "Informe o CPF.")
    @Pattern(regexp = "([0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}\\-[0-9]{2})",
            message = "O CPF deve conter 11 dígitos.")
    private String cpf;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
}
