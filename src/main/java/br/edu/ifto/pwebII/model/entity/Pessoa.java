package br.edu.ifto.pwebII.model.entity;

import br.edu.ifto.pwebII.validation.Cadastro;
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

    /**
     * Conta de acesso vinculada à pessoa.
     * O cascade ALL faz com que o usuário seja persistido/removido junto com a pessoa.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    // Campos usados somente pelos formulários de cadastro/edição.
    @Transient
    @NotBlank(message = "Informe o nome de usuário para acesso ao sistema.")
    @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre {min} e {max} caracteres.")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "O nome de usuário deve conter apenas letras, números, ponto, hífen ou sublinhado.")
    private String login;

    @Transient
    @NotBlank(groups = Cadastro.class, message = "Informe a senha de acesso.")
    @Size(min = 6, max = 72, groups = Cadastro.class,
            message = "A senha deve ter entre {min} e {max} caracteres.")
    private String senha;

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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * Ao editar, o login já cadastrado é exibido no formulário.
     */
    public String getLogin() {
        if (login != null) {
            return login;
        }
        return usuario != null ? usuario.getUsuario() : null;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
