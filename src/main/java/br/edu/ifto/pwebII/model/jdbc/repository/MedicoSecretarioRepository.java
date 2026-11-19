package br.edu.ifto.pwebII.model.jdbc.repository;

import br.edu.ifto.pwebII.model.entity.Medico;
import br.edu.ifto.pwebII.model.entity.MedicoSecretario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repositório das permissões concedidas por um médico aos seus secretários associados.
 * Segue o padrão da disciplina: EntityManager injetado via @PersistenceContext.
 */
@Repository
public class MedicoSecretarioRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(MedicoSecretario vinculo) {
        em.persist(vinculo);
    }

    public void update(MedicoSecretario vinculo) {
        em.merge(vinculo);
    }

    public void remove(Long id) {
        MedicoSecretario ms = em.find(MedicoSecretario.class, id);
        if (ms != null) {
            em.remove(ms);
        }
    }

    public MedicoSecretario porId(Long id) {
        List<MedicoSecretario> lista = em.createQuery(
                        "select ms from MedicoSecretario ms " +
                                "join fetch ms.medico " +
                                "join fetch ms.secretario " +
                                "where ms.id = :id",
                        MedicoSecretario.class)
                .setParameter("id", id)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<MedicoSecretario> porMedico(Long idMedico) {
        return em.createQuery(
                        "select ms from MedicoSecretario ms " +
                                "join fetch ms.secretario " +
                                "where ms.medico.id = :idMedico " +
                                "order by ms.secretario.usuario asc",
                        MedicoSecretario.class)
                .setParameter("idMedico", idMedico)
                .getResultList();
    }

    public MedicoSecretario porMedicoESecretario(Long idMedico, Long idSecretario) {
        List<MedicoSecretario> lista = em.createQuery(
                        "select ms from MedicoSecretario ms " +
                                "join fetch ms.medico " +
                                "join fetch ms.secretario " +
                                "where ms.medico.id = :idMedico and ms.secretario.id = :idSecretario",
                        MedicoSecretario.class)
                .setParameter("idMedico", idMedico)
                .setParameter("idSecretario", idSecretario)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<Medico> medicosComPermissaoAgenda(Long idSecretario) {
        return em.createQuery(
                        "select ms.medico from MedicoSecretario ms " +
                                "where ms.secretario.id = :idSecretario and ms.podeAlterarAgenda = true " +
                                "order by ms.medico.nome asc",
                        Medico.class)
                .setParameter("idSecretario", idSecretario)
                .getResultList();
    }

    public Set<Long> idsMedicosComPermissaoAgenda(Long idSecretario) {
        return em.createQuery(
                        "select ms.medico.id from MedicoSecretario ms " +
                                "where ms.secretario.id = :idSecretario and ms.podeAlterarAgenda = true",
                        Long.class)
                .setParameter("idSecretario", idSecretario)
                .getResultList()
                .stream()
                .collect(Collectors.toSet());
    }

    public Set<Long> idsMedicosComPermissaoPreco(Long idSecretario) {
        return em.createQuery(
                        "select ms.medico.id from MedicoSecretario ms " +
                                "where ms.secretario.id = :idSecretario and ms.podeAlterarPreco = true",
                        Long.class)
                .setParameter("idSecretario", idSecretario)
                .getResultList()
                .stream()
                .collect(Collectors.toSet());
    }

    public boolean podeAlterarAgenda(Long idMedico, Long idSecretario) {
        Long total = em.createQuery(
                        "select count(ms) from MedicoSecretario ms " +
                                "where ms.medico.id = :idMedico and ms.secretario.id = :idSecretario and ms.podeAlterarAgenda = true",
                        Long.class)
                .setParameter("idMedico", idMedico)
                .setParameter("idSecretario", idSecretario)
                .getSingleResult();
        return total > 0;
    }

    public boolean podeAlterarPreco(Long idMedico, Long idSecretario) {
        Long total = em.createQuery(
                        "select count(ms) from MedicoSecretario ms " +
                                "where ms.medico.id = :idMedico and ms.secretario.id = :idSecretario and ms.podeAlterarPreco = true",
                        Long.class)
                .setParameter("idMedico", idMedico)
                .setParameter("idSecretario", idSecretario)
                .getSingleResult();
        return total > 0;
    }

    public boolean existeVinculo(Long idMedico, Long idSecretario) {
        Long total = em.createQuery(
                        "select count(ms) from MedicoSecretario ms " +
                                "where ms.medico.id = :idMedico and ms.secretario.id = :idSecretario",
                        Long.class)
                .setParameter("idMedico", idMedico)
                .setParameter("idSecretario", idSecretario)
                .getSingleResult();
        return total > 0;
    }

    public Set<Long> idsMedicosAssociados(Long idSecretario) {
        return em.createQuery(
                        "select ms.medico.id from MedicoSecretario ms where ms.secretario.id = :idSecretario",
                        Long.class)
                .setParameter("idSecretario", idSecretario)
                .getResultList()
                .stream()
                .collect(Collectors.toSet());
    }
}
