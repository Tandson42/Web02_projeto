package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Consulta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Responsável pelo acesso aos dados da entidade Consulta.
 * Segue o padrão da disciplina: EntityManager injetado via @PersistenceContext.
 */
@Repository
public class ConsultaRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Consulta consulta) {
        em.persist(consulta);
    }

    public Consulta consulta(Long id) {
        return em.find(Consulta.class, id);
    }

    public List<Consulta> consultas() {
        return em.createQuery(
                "select c from Consulta c " +
                        "join fetch c.paciente " +
                        "join fetch c.medico " +
                        "left join fetch c.agendamento " +
                        "order by c.data desc",
                Consulta.class).getResultList();
    }

    public void remove(Long id) {
        Consulta c = em.find(Consulta.class, id);
        em.remove(c);
    }

    public void update(Consulta consulta) {
        em.merge(consulta);
    }
}
