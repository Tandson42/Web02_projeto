package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Agenda;
import br.edu.ifto.pwebII.model.entity.Agendamento;
import br.edu.ifto.pwebII.model.entity.Medico;
import br.edu.ifto.pwebII.model.entity.Paciente;
import br.edu.ifto.pwebII.model.jdbc.repository.AgendaRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.AgendamentoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.PacienteRepository;
import br.edu.ifto.pwebII.service.AgendaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

/**
 * Controller MVC responsável pelos agendamentos de horários, confirmação de
 * presença (com registro de Consulta) e cancelamento com liberação de horário.
 */
@Controller
@RequestMapping("agendamento")
public class AgendamentoController {

    private final AgendamentoRepository agendamentoRepository;
    private final AgendaRepository agendaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final AgendaService agendaService;

    public AgendamentoController(AgendamentoRepository agendamentoRepository,
                                 AgendaRepository agendaRepository,
                                 PacienteRepository pacienteRepository,
                                 MedicoRepository medicoRepository,
                                 AgendaService agendaService) {
        this.agendamentoRepository = agendamentoRepository;
        this.agendaRepository = agendaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.agendaService = agendaService;
    }

    /**
     * Lista os agendamentos conforme o perfil autenticado:
     * - ADMIN/SECRETARIO: todos os agendamentos da clínica
     * - MEDICO: agendamentos dos seus horários
     * - PACIENTE: apenas os seus próprios agendamentos
     */
    @GetMapping("/list")
    public ModelAndView list(Authentication authentication, ModelMap model) {
        if (isClinica(authentication)) {
            model.addAttribute("agendamentos", agendamentoRepository.agendamentos());
        } else if (hasRole(authentication, "ROLE_MEDICO")) {
            Medico medico = medicoRepository.porUsuario(authentication.getName());
            model.addAttribute("agendamentos", medico != null
                    ? agendamentoRepository.agendamentosPorMedico(medico.getId())
                    : Collections.emptyList());
        } else {
            Paciente paciente = pacienteRepository.porUsuario(authentication.getName());
            model.addAttribute("agendamentos", paciente != null
                    ? agendamentoRepository.agendamentosPorPaciente(paciente.getId())
                    : Collections.emptyList());
        }
        return new ModelAndView("agendamento/list", model);
    }

    /**
     * Exibe a tela para confirmar o agendamento de um horário disponível da agenda.
     */
    @GetMapping("/novo/{idAgenda}")
    public ModelAndView novo(@PathVariable("idAgenda") Long idAgenda,
                             Authentication authentication,
                             ModelMap model,
                             RedirectAttributes attributes) {
        Agenda agenda = agendaRepository.agenda(idAgenda);
        if (agenda == null) {
            attributes.addFlashAttribute("erro", "Horário não encontrado na agenda.");
            return new ModelAndView("redirect:/agenda/disponiveis");
        }
        if (!agenda.isDisponivel()) {
            attributes.addFlashAttribute("erro", "Este horário já foi agendado e não está mais disponível.");
            return new ModelAndView("redirect:/agenda/disponiveis");
        }

        model.addAttribute("agenda", agenda);
        if (isClinica(authentication)) {
            model.addAttribute("pacientes", pacienteRepository.pacientes());
        } else {
            Paciente pacienteLogado = pacienteRepository.porUsuario(authentication.getName());
            if (pacienteLogado == null) {
                attributes.addFlashAttribute("erro", "Sua conta não está vinculada a um cadastro de paciente.");
                return new ModelAndView("redirect:/agenda/disponiveis");
            }
            model.addAttribute("pacienteLogado", pacienteLogado);
        }
        return new ModelAndView("agendamento/form", model);
    }

    /**
     * Registra o agendamento de um horário disponível para o paciente selecionado/logado.
     */
    @PostMapping("/save")
    public ModelAndView save(@RequestParam("agendaId") Long agendaId,
                             @RequestParam(value = "pacienteId", required = false) Long pacienteId,
                             Authentication authentication,
                             RedirectAttributes attributes) {
        Long idPacienteDestino = pacienteId;
        if (!isClinica(authentication)) {
            Paciente pacienteLogado = pacienteRepository.porUsuario(authentication.getName());
            if (pacienteLogado == null) {
                attributes.addFlashAttribute("erro", "Sua conta não está vinculada a um cadastro de paciente.");
                return new ModelAndView("redirect:/agenda/disponiveis");
            }
            idPacienteDestino = pacienteLogado.getId();
        }

        if (idPacienteDestino == null) {
            attributes.addFlashAttribute("erro", "Selecione um paciente para concluir o agendamento.");
            return new ModelAndView("redirect:/agendamento/novo/" + agendaId);
        }

        try {
            agendaService.agendar(agendaId, idPacienteDestino);
            attributes.addFlashAttribute("mensagem",
                    "Agendamento realizado com sucesso! O horário foi reservado.");
            return new ModelAndView(isClinica(authentication) ? "redirect:/agenda/list" : "redirect:/minhas-consultas");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return new ModelAndView("redirect:/agenda/disponiveis");
        }
    }

    /**
     * Exibe a tela de confirmação de presença pela clínica para registrar a consulta.
     */
    @GetMapping("/confirmar/{id}")
    public ModelAndView confirmarForm(@PathVariable("id") Long id,
                                      ModelMap model,
                                      RedirectAttributes attributes) {
        Agendamento agendamento = agendamentoRepository.agendamento(id);
        if (agendamento == null) {
            attributes.addFlashAttribute("erro", "Agendamento não encontrado.");
            return new ModelAndView("redirect:/agendamento/list");
        }
        if (!agendamento.isAgendado()) {
            attributes.addFlashAttribute("erro",
                    "Somente agendamentos com situação AGENDADO podem ter a presença confirmada.");
            return new ModelAndView("redirect:/agendamento/list");
        }
        model.addAttribute("agendamento", agendamento);
        return new ModelAndView("agendamento/confirmar", model);
    }

    /**
     * Confirma o comparecimento do paciente, cria a Consulta vinculada e marca o Agendamento como REALIZADO.
     */
    @PostMapping("/confirmar/{id}")
    public ModelAndView confirmar(@PathVariable("id") Long id,
                                  @RequestParam(value = "observacao", required = false) String observacao,
                                  RedirectAttributes attributes) {
        try {
            agendaService.confirmarPresenca(id, observacao);
            attributes.addFlashAttribute("mensagem",
                    "Presença confirmada! A consulta foi registrada com o valor garantido no agendamento e marcada como REALIZADO.");
            return new ModelAndView("redirect:/agenda/list");
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return new ModelAndView("redirect:/agendamento/confirmar/" + id);
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return new ModelAndView("redirect:/agenda/list");
        }
    }

    /**
     * Cancela um agendamento existente e libera o horário novamente na agenda (DISPONIVEL).
     */
    @PostMapping("/cancelar/{id}")
    public ModelAndView cancelar(@PathVariable("id") Long id,
                                 Authentication authentication,
                                 RedirectAttributes attributes) {
        try {
            Paciente pacienteRestrito = isClinica(authentication)
                    ? null
                    : pacienteRepository.porUsuario(authentication.getName());
            agendaService.cancelarAgendamento(id, pacienteRestrito);
            attributes.addFlashAttribute("mensagem",
                    "Agendamento cancelado com sucesso! O horário foi liberado novamente na agenda.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
        }
        return new ModelAndView(isClinica(authentication) ? "redirect:/agenda/list" : "redirect:/minhas-consultas");
    }

    private boolean isClinica(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_SECRETARIO");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
