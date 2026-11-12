package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa um papel (role) de acesso do sistema.
 *
 * O nome é armazenado sem o prefixo "ROLE_" (por exemplo, ADMIN ou USER).
 * O prefixo exigido pelo Spring Security é acrescentado em getAuthority().
 */
@Entity
@Table(name = "perfil")
public class Perfil implements GrantedAuthority, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o nome do perfil.")
    @Size(max = 30, message = "O nome do perfil deve ter no máximo {max} caracteres.")
    @Column(nullable = false, unique = true, length = 30)
    private String nome;

    // Lado inverso do relacionamento muitos-para-muitos.
    // A tabela de junção é definida em Usuario (@JoinTable).
    @ManyToMany(mappedBy = "perfis")
    private Set<Usuario> usuarios = new HashSet<>();

    /**
     * Formato esperado pelo Spring Security: ROLE_ADMIN, ROLE_USER etc.
     */
    @Override
    public String getAuthority() {
        return "ROLE_" + nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Set<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(Set<Usuario> usuarios) {
        this.usuarios = usuarios;
    }
}