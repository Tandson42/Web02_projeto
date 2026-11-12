package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de contas de acesso.
 * Mantém o padrão do projeto: EntityManager injetado via @PersistenceContext.
 */
@Repository
public class UsuarioRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Usuario usuario) {
        em.persist(usuario);
    }

    public void update(Usuario usuario) {
        em.merge(usuario);
    }

    public Usuario usuario(Long id) {
        return em.find(Usuario.class, id);
    }

    /**
     * Carrega o usuário e seus perfis em uma única consulta.
     * Usado pelo UserDetailsService durante a autenticação.
     */
    public Usuario porUsuario(String usuario) {
        TypedQuery<Usuario> query = em.createQuery(
                "select distinct u from Usuario u left join fetch u.perfis where u.usuario = :usuario",
                Usuario.class);
        query.setParameter("usuario", usuario);
        List<Usuario> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<Usuario> usuarios() {
        TypedQuery<Usuario> query = em.createQuery(
                "select distinct u from Usuario u left join fetch u.perfis order by u.usuario",
                Usuario.class);
        return query.getResultList();
    }

    public boolean existePorUsuario(String usuario) {
        Long total = em.createQuery(
                        "select count(u) from Usuario u where lower(u.usuario) = lower(:usuario)", Long.class)
                .setParameter("usuario", usuario)
                .getSingleResult();
        return total > 0;
    }

    /**
     * Verifica duplicidade ignorando o próprio usuário que está sendo editado.
     */
    public boolean existeOutroUsuario(String usuario, Long idUsuario) {
        Long total = em.createQuery(
                        "select count(u) from Usuario u where lower(u.usuario) = lower(:usuario) and u.id <> :id", Long.class)
                .setParameter("usuario", usuario)
                .setParameter("id", idUsuario)
                .getSingleResult();
        return total > 0;
    }

    public long contarSecretarios() {
        return em.createQuery(
                        "select count(distinct u) from Usuario u join u.perfis p where p.nome in ('SECRETARIO', 'ADMIN')", Long.class)
                .getSingleResult();
    }
}