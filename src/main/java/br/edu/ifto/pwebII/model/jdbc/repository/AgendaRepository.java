package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Agenda;
import br.edu.ifto.pwebII.model.entity.StatusAgenda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Responsável pelo acesso aos dados da entidade Agenda.
 * Segue o padrão da disciplina: EntityManager injetado via @PersistenceContext.
 */
@Repository
public class AgendaRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Agenda agenda) {
        em.persist(agenda);
    }

    public void update(Agenda agenda) {
        em.merge(agenda);
    }

    public Agenda agenda(Long id) {
        List<Agenda> resultado = em.createQuery(
                        "select a from Agenda a join fetch a.medico where a.id = :id", Agenda.class)
                .setParameter("id", id)
                .getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public void remove(Long id) {
        Agenda agenda = em.find(Agenda.class, id);
        if (agenda != null) {
            em.remove(agenda);
        }
    }

    public List<Agenda> agendas() {
        return em.createQuery(
                        "select a from Agenda a join fetch a.medico order by a.data asc, a.horario asc",
                        Agenda.class)
                .getResultList();
    }

    public List<Agenda> agendasPorMedico(Long idMedico) {
        return em.createQuery(
                        "select a from Agenda a join fetch a.medico where a.medico.id = :idMedico order by a.data asc, a.horario asc",
                        Agenda.class)
                .setParameter("idMedico", idMedico)
                .getResultList();
    }

    public List<Agenda> disponiveis() {
        return em.createQuery(
                        "select a from Agenda a join fetch a.medico where a.status = :status order by a.data asc, a.horario asc",
                        Agenda.class)
                .setParameter("status", StatusAgenda.DISPONIVEL)
                .getResultList();
    }

    public boolean existeHorarioDuplicado(Long idMedico, LocalDate data, LocalTime horario) {
        Long total = em.createQuery(
                        "select count(a) from Agenda a where a.medico.id = :idMedico and a.data = :data and a.horario = :horario",
                        Long.class)
                .setParameter("idMedico", idMedico)
                .setParameter("data", data)
                .setParameter("horario", horario)
                .getSingleResult();
        return total > 0;
    }
}
