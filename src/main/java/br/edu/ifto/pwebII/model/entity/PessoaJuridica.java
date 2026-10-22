package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

@Entity
@PrimaryKeyJoinColumn(name = "id_pessoa")
public class PessoaJuridica extends Pessoa implements Serializable {

    @NotBlank(message = "Informe a razão social.")
    @Size(max = 100, message = "A razão social deve ter no máximo {max} caracteres.")
    private String razaoSocial;

    @NotBlank(message = "Informe o CNPJ.")
    @Pattern(regexp = "([0-9]{14}|[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}\\/[0-9]{4}\\-[0-9]{2})",
            message = "O CNPJ deve conter 14 dígitos.")
    private String cnpj;

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }
}