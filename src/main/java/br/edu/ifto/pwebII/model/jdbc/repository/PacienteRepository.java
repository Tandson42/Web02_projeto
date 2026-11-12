package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Consulta;
import br.edu.ifto.pwebII.model.entity.Paciente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Responsável pelo acesso aos dados da entidade Paciente.
 * Segue o padrão da disciplina: EntityManager injetado via @PersistenceContext.
 */
@Repository
public class PacienteRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Paciente paciente) {
        em.persist(paciente);
    }

    public Paciente paciente(Long id) {
        return em.find(Paciente.class, id);
    }

    public List<Paciente> pacientes() {
        Query query = em.createQuery("from Paciente"); //HQL
        return query.getResultList();
    }

    public void remove(Long id) {
        Paciente p = em.find(Paciente.class, id);
        em.remove(p);
    }

    public void update(Paciente paciente) {
        em.merge(paciente);
    }

    /**
     * Retorna todas as consultas de um determinado paciente.
     * Diferença de implementação: além do atributo de coleção do Paciente
     * (que também funciona), este método usa uma query explícita para
     * garantir o carregamento das consultas de forma direta.
     *
     * @param id identificador do paciente
     * @return lista de consultas do paciente
     */
    public List<Consulta> consultasDoPaciente(Long id) {
        Query query = em.createQuery("from Consulta c where c.paciente.id = :id");
        query.setParameter("id", id);
        return query.getResultList();
    }

    public Paciente porUsuario(String login) {
        List<Paciente> list = em.createQuery(
                "select p from Paciente p where lower(p.usuario.usuario) = lower(:login)", Paciente.class)
                .setParameter("login", login)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Paciente> pacientesDoMedico(Long idMedico) {
        if (idMedico == null) {
            return List.of();
        }
        return em.createQuery(
                "select distinct c.paciente from Consulta c where c.medico.id = :idMedico", Paciente.class)
                .setParameter("idMedico", idMedico)
                .getResultList();
    }
}
