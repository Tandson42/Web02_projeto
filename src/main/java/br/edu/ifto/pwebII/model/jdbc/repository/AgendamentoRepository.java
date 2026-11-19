package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Agendamento;
import br.edu.ifto.pwebII.model.entity.StatusAgendamento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Responsável pelo acesso aos dados da entidade Agendamento.
 * Segue o padrão da disciplina: EntityManager injetado via @PersistenceContext.
 */
@Repository
public class AgendamentoRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Agendamento agendamento) {
        em.persist(agendamento);
    }

    public void update(Agendamento agendamento) {
        em.merge(agendamento);
    }

    public Agendamento agendamento(Long id) {
        List<Agendamento> resultado = em.createQuery(
                        "select ag from Agendamento ag " +
                                "join fetch ag.agenda a " +
                                "join fetch a.medico " +
                                "join fetch ag.paciente " +
                                "left join fetch ag.consulta " +
                                "where ag.id = :id",
                        Agendamento.class)
                .setParameter("id", id)
                .getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<Agendamento> agendamentos() {
        return em.createQuery(
                        "select ag from Agendamento ag " +
                                "join fetch ag.agenda a " +
                                "join fetch a.medico " +
                                "join fetch ag.paciente " +
                                "left join fetch ag.consulta " +
                                "order by a.data desc, a.horario desc, ag.id desc",
                        Agendamento.class)
                .getResultList();
    }

    public List<Agendamento> agendamentosPorPaciente(Long idPaciente) {
        return em.createQuery(
                        "select ag from Agendamento ag " +
                                "join fetch ag.agenda a " +
                                "join fetch a.medico " +
                                "join fetch ag.paciente " +
                                "left join fetch ag.consulta " +
                                "where ag.paciente.id = :idPaciente " +
                                "order by a.data desc, a.horario desc, ag.id desc",
                        Agendamento.class)
                .setParameter("idPaciente", idPaciente)
                .getResultList();
    }

    public List<Agendamento> agendamentosPorMedico(Long idMedico) {
        return em.createQuery(
                        "select ag from Agendamento ag " +
                                "join fetch ag.agenda a " +
                                "join fetch a.medico " +
                                "join fetch ag.paciente " +
                                "left join fetch ag.consulta " +
                                "where a.medico.id = :idMedico " +
                                "order by a.data desc, a.horario desc, ag.id desc",
                        Agendamento.class)
                .setParameter("idMedico", idMedico)
                .getResultList();
    }

    public boolean existeAgendamentoAtivoParaAgenda(Long idAgenda) {
        Long total = em.createQuery(
                        "select count(ag) from Agendamento ag where ag.agenda.id = :idAgenda and ag.status = :status",
                        Long.class)
                .setParameter("idAgenda", idAgenda)
                .setParameter("status", StatusAgendamento.AGENDADO)
                .getSingleResult();
        return total > 0;
    }

    public boolean existeHistoricoParaAgenda(Long idAgenda) {
        Long total = em.createQuery(
                        "select count(ag) from Agendamento ag where ag.agenda.id = :idAgenda",
                        Long.class)
                .setParameter("idAgenda", idAgenda)
                .getSingleResult();
        return total > 0;
    }
}
