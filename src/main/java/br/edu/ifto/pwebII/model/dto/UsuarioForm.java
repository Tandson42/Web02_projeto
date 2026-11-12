package br.edu.ifto.pwebII.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Formulário de edição de uma conta de acesso na área administrativa.
 * Não é uma entidade: evita que o formulário altere diretamente o Usuario/Perfis.
 */
public class UsuarioForm {

    private Long id;

    @NotBlank(message = "Informe o nome de usuário.")
    @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre {min} e {max} caracteres.")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "O nome de usuário deve conter apenas letras, números, ponto, hífen ou sublinhado.")
    private String usuario;

    /**
     * Em branco significa "manter a senha atual".
     */
    private String senha;

    @NotEmpty(message = "Selecione ao menos um papel.")
    private List<Long> perfis = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public List<Long> getPerfis() {
        return perfis;
    }

    public void setPerfis(List<Long> perfis) {
        this.perfis = perfis;
    }
}