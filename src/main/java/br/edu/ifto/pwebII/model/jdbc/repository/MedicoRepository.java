package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Consulta;
import br.edu.ifto.pwebII.model.entity.Medico;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Responsável pelo acesso aos dados da entidade Medico.
 * Segue o padrão da disciplina: EntityManager injetado via @PersistenceContext.
 */
@Repository
public class MedicoRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Medico medico) {
        em.persist(medico);
    }

    public Medico medico(Long id) {
        return em.find(Medico.class, id);
    }

    public List<Medico> medicos() {
        Query query = em.createQuery("from Medico"); //HQL
        return query.getResultList();
    }

    public void remove(Long id) {
        Medico m = em.find(Medico.class, id);
        em.remove(m);
    }

    public void update(Medico medico) {
        em.merge(medico);
    }

    /**
     * Retorna todas as consultas realizadas por um determinado médico.
     * Diferença de implementação: usa uma query explícita para carregar
     * as consultas do médico de forma direta.
     *
     * @param id identificador do médico
     * @return lista de consultas do médico
     */
    public List<Consulta> consultasDoMedico(Long id) {
        Query query = em.createQuery("from Consulta c where c.medico.id = :id");
        query.setParameter("id", id);
        return query.getResultList();
    }

    public Medico porUsuario(String login) {
        List<Medico> list = em.createQuery(
                "select m from Medico m where lower(m.usuario.usuario) = lower(:login)", Medico.class)
                .setParameter("login", login)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }
}
