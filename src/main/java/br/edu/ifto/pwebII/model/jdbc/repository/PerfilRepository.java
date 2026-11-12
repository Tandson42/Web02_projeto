package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Perfil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório dos papéis (roles) de acesso.
 */
@Repository
public class PerfilRepository {

    @PersistenceContext
    private EntityManager em;

    public Perfil porId(Long id) {
        return em.find(Perfil.class, id);
    }

    public Perfil porNome(String nome) {
        TypedQuery<Perfil> query = em.createQuery(
                "from Perfil p where upper(p.nome) = upper(:nome)", Perfil.class);
        query.setParameter("nome", nome);
        List<Perfil> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<Perfil> perfis() {
        return em.createQuery("from Perfil p order by p.nome", Perfil.class).getResultList();
    }

    public void save(Perfil perfil) {
        em.persist(perfil);
    }
}