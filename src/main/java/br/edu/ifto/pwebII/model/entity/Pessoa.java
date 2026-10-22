package br.edu.ifto.pwebII.model.entity;

import br.edu.ifto.pwebII.validation.Edicao;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pessoa implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull(groups = Edicao.class, message = "O identificador deve ser informado para edição.")
    private Long id;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 100, message = "O e-mail deve ter no máximo {max} caracteres.")
    private String email;

    @NotBlank(message = "Informe o telefone.")
    @Pattern(regexp = "[0-9()\\s+\\-]{8,20}", message = "O telefone deve conter de 8 a 20 caracteres, usando apenas números, espaços e os símbolos ( ) + -.")
    private String telefone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
