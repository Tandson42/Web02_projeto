package br.edu.ifto.pwebII.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Conta de acesso ao sistema.
 *
 * Implementa UserDetails para que o próprio usuário carregado do banco seja
 * usado pelo Spring Security como principal autenticado.
 */
@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o nome de usuário.")
    @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre {min} e {max} caracteres.")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "O nome de usuário deve conter apenas letras, números, ponto, hífen ou sublinhado.")
    @Column(nullable = false, unique = true, length = 50)
    private String usuario;

    // Sempre armazenada codificada com BCrypt.
    @Column(nullable = false, length = 100)
    private String senha;

    // EAGER para que as autoridades estejam disponíveis durante a autenticação.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_perfil",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_perfil"))
    private Set<Perfil> perfis = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new HashSet<>(perfis);
    }

    /**
     * O Spring Security chama getPassword(), mas o atributo do modelo é "senha".
     */
    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return usuario;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean possuiPerfil(Long idPerfil) {
        return perfis.stream().anyMatch(p -> p.getId() != null && p.getId().equals(idPerfil));
    }

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

    public Set<Perfil> getPerfis() {
        return perfis;
    }

    public void setPerfis(Set<Perfil> perfis) {
        this.perfis = perfis;
    }
}