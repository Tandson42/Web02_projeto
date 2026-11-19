package br.edu.ifto.pwebII.service;

import br.edu.ifto.pwebII.model.entity.*;
import br.edu.ifto.pwebII.model.jdbc.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;

/**
 * Centraliza as regras de negócio e transações de Agenda, Agendamento
 * e registro de Consulta após confirmação de presença.
 */
@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ConsultaRepository consultaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;

    public AgendaService(AgendaRepository agendaRepository,
                         AgendamentoRepository agendamentoRepository,
                         ConsultaRepository consultaRepository,
                         MedicoRepository medicoRepository,
                         PacienteRepository pacienteRepository) {
        this.agendaRepository = agendaRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.consultaRepository = consultaRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    /**
     * Cadastra um novo horário disponível na agenda do médico, impedindo duplicidades.
     */
    @Transactional
    public void cadastrarHorario(Agenda agenda, BindingResult result) {
        Long idMedico = agenda.getMedico() != null ? agenda.getMedico().getId() : null;
        if (idMedico == null) {
            result.rejectValue("medico", "medico.obrigatorio", "Selecione um médico.");
        }
        if (result.hasErrors()) {
            return;
        }

        Medico medico = medicoRepository.medico(idMedico);
        if (medico == null) {
            result.rejectValue("medico", "medico.naoEncontrado", "Médico não encontrado.");
            return;
        }

        if (agenda.getDataHora() != null && agenda.getDataHora().isBefore(LocalDateTime.now())) {
            result.rejectValue("data", "agenda.passado",
                    "Não é possível cadastrar um horário em data ou hora no passado.");
            return;
        }

        if (agendaRepository.existeHorarioDuplicado(medico.getId(), agenda.getData(), agenda.getHorario())) {
            result.rejectValue("horario", "agenda.duplicada",
                    "Este médico já possui um horário cadastrado para esta data e horário.");
            return;
        }

        agenda.setMedico(medico);
        agenda.setStatus(StatusAgenda.DISPONIVEL);
        agendaRepository.save(agenda);
    }

    /**
     * Remove um horário da agenda somente se estiver disponível e sem histórico de agendamentos.
     */
    @Transactional
    public void removerHorario(Long idAgenda, Medico medicoRestrito) {
        Agenda agenda = agendaRepository.agenda(idAgenda);
        if (agenda == null) {
            throw new IllegalArgumentException("Horário não encontrado na agenda.");
        }
        if (medicoRestrito != null && !agenda.getMedico().getId().equals(medicoRestrito.getId())) {
            throw new IllegalStateException("Você só pode remover horários da sua própria agenda.");
        }
        if (!agenda.isDisponivel()) {
            throw new IllegalStateException("Não é possível remover um horário que já está agendado.");
        }
        if (agendamentoRepository.existeHistoricoParaAgenda(idAgenda)) {
            throw new IllegalStateException("Este horário possui histórico de agendamentos e não pode ser removido.");
        }
        agendaRepository.remove(idAgenda);
    }

    /**
     * Reserva um horário disponível da agenda para um paciente e atualiza a agenda para AGENDADO.
     * Não cria Consulta — a consulta só é registrada após confirmação de presença pela clínica.
     */
    @Transactional
    public Agendamento agendar(Long idAgenda, Long idPaciente) {
        Agenda agenda = agendaRepository.agenda(idAgenda);
        if (agenda == null) {
            throw new IllegalArgumentException("Horário da agenda não encontrado.");
        }
        if (!agenda.isDisponivel() || agendamentoRepository.existeAgendamentoAtivoParaAgenda(idAgenda)) {
            throw new IllegalStateException("Este horário já está ocupado ou indisponível para agendamento.");
        }
        if (agenda.getDataHora() != null && agenda.getDataHora().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Não é possível agendar um horário que já passou.");
        }

        Paciente paciente = pacienteRepository.paciente(idPaciente);
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente não encontrado.");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setAgenda(agenda);
        agendamento.setPaciente(paciente);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setDataAgendamento(LocalDateTime.now());
        // Congela o valor atual da consulta do médico no momento do agendamento
        Double valorConsultaMedico = agenda.getMedico().getValorConsulta();
        agendamento.setValor(valorConsultaMedico != null ? valorConsultaMedico : 0.0);

        agenda.setStatus(StatusAgenda.AGENDADO);

        agendaRepository.update(agenda);
        agendamentoRepository.save(agendamento);
        return agendamento;
    }

    /**
     * Confirma a presença do paciente na clínica, registra a Consulta vinculada ao
     * Agendamento usando o valor congelado na reserva e atualiza o status para REALIZADO.
     */
    @Transactional
    public Consulta confirmarPresenca(Long idAgendamento, String observacao) {
        Agendamento agendamento = agendamentoRepository.agendamento(idAgendamento);
        if (agendamento == null) {
            throw new IllegalArgumentException("Agendamento não encontrado.");
        }
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new IllegalStateException("Somente agendamentos com situação AGENDADO podem ter a presença confirmada.");
        }
        if (agendamento.getConsulta() != null) {
            throw new IllegalStateException("Já existe uma consulta registrada para este agendamento.");
        }

        Consulta consulta = new Consulta();
        consulta.setAgendamento(agendamento);
        consulta.setPaciente(agendamento.getPaciente());
        consulta.setMedico(agendamento.getAgenda().getMedico());
        consulta.setData(agendamento.getAgenda().getDataHora());
        consulta.setValor(agendamento.getValor() != null ? agendamento.getValor() : 0.0);
        consulta.setObservacao(observacao != null && !observacao.isBlank()
                ? observacao.trim()
                : "Consulta registrada após confirmação de presença.");

        agendamento.setStatus(StatusAgendamento.REALIZADO);

        consultaRepository.save(consulta);
        agendamentoRepository.update(agendamento);
        return consulta;
    }

    /**
     * Cancela um agendamento existente, atualiza seu status para CANCELADO e
     * libera o horário na Agenda (status DISPONIVEL) para que possa ser reservado novamente.
     */
    @Transactional
    public void cancelarAgendamento(Long idAgendamento, Paciente pacienteRestrito) {
        Agendamento agendamento = agendamentoRepository.agendamento(idAgendamento);
        if (agendamento == null) {
            throw new IllegalArgumentException("Agendamento não encontrado.");
        }
        if (pacienteRestrito != null && !agendamento.getPaciente().getId().equals(pacienteRestrito.getId())) {
            throw new IllegalStateException("Você só pode cancelar os seus próprios agendamentos.");
        }
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new IllegalStateException("Somente agendamentos com situação AGENDADO podem ser cancelados.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        Agenda agenda = agendamento.getAgenda();
        agenda.setStatus(StatusAgenda.DISPONIVEL);

        agendamentoRepository.update(agendamento);
        agendaRepository.update(agenda);
    }
}
