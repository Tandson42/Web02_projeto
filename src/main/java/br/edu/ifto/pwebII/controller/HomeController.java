package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.*;
import br.edu.ifto.pwebII.model.jdbc.repository.AgendamentoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.PacienteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class HomeController {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public HomeController(PacienteRepository pacienteRepository,
                          MedicoRepository medicoRepository,
                          AgendamentoRepository agendamentoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    // carrega o menu principal na raiz da aplicação
    @GetMapping("/")
    public ModelAndView menu() {
        return new ModelAndView("menu");
    }

    // área do paciente: agenda das próprias consultas/agendamentos + histórico de consultas realizadas
    @GetMapping("/minhas-consultas")
    public ModelAndView minhasConsultas(Principal principal, ModelMap model) {
        String login = principal != null ? principal.getName() : null;
        Paciente paciente = login != null ? pacienteRepository.porUsuario(login) : null;
        if (paciente != null) {
            model.addAttribute("paciente", paciente);
            model.addAttribute("consultas", pacienteRepository.consultasDoPaciente(paciente.getId()));
            model.addAttribute("agendamentos", agendamentoRepository.agendamentosPorPaciente(paciente.getId()));
        } else {
            model.addAttribute("consultas", Collections.emptyList());
            model.addAttribute("agendamentos", Collections.emptyList());
        }
        return new ModelAndView("paciente/consultas", model);
    }

    // eventos JSON do calendário do paciente (apenas seus agendamentos e suas consultas realizadas)
    @GetMapping("/minhas-consultas/eventos")
    @ResponseBody
    public List<Map<String, Object>> eventosPaciente(Principal principal) {
        List<Map<String, Object>> eventos = new ArrayList<>();
        if (principal == null) {
            return eventos;
        }
        Paciente paciente = pacienteRepository.porUsuario(principal.getName());
        if (paciente == null) {
            return eventos;
        }

        // Agendamentos ativos do paciente (aguardando comparecimento)
        for (Agendamento ag : agendamentoRepository.agendamentosPorPaciente(paciente.getId())) {
            if (ag.getStatus() != StatusAgendamento.AGENDADO || ag.getAgenda().getDataHora() == null) {
                continue;
            }
            LocalDateTime inicio = ag.getAgenda().getDataHora();
            LocalDateTime fim = inicio.plusMinutes(30);

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("tipo", "AGENDADO");
            props.put("statusLabel", "Agendado");
            props.put("agendamentoId", ag.getId());
            props.put("medico", ag.getAgenda().getMedico().getNome());
            props.put("crm", ag.getAgenda().getMedico().getCrm());
            props.put("valor", ag.getValor());

            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id", "ag-" + ag.getId());
            ev.put("title", "Agendado · " + ag.getAgenda().getMedico().getNome());
            ev.put("start", inicio.format(ISO_LOCAL));
            ev.put("end", fim.format(ISO_LOCAL));
            ev.put("backgroundColor", "#f59e0b");
            ev.put("borderColor", "#d97706");
            ev.put("textColor", "#ffffff");
            ev.put("extendedProps", props);
            eventos.add(ev);
        }

        // Consultas realizadas do paciente
        for (Consulta c : pacienteRepository.consultasDoPaciente(paciente.getId())) {
            if (c.getData() == null) {
                continue;
            }
            LocalDateTime inicio = c.getData();
            LocalDateTime fim = inicio.plusMinutes(30);

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("tipo", "CONSULTA");
            props.put("statusLabel", "Consulta Realizada");
            props.put("consultaId", c.getId());
            props.put("medico", c.getMedico() != null ? c.getMedico().getNome() : "-");
            props.put("crm", c.getMedico() != null ? c.getMedico().getCrm() : "-");
            props.put("valor", c.getValor());
            props.put("observacao", c.getObservacao() != null && !c.getObservacao().isBlank()
                    ? c.getObservacao()
                    : "Sem observações.");

            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id", "cons-" + c.getId());
            ev.put("title", "Consulta · " + (c.getMedico() != null ? c.getMedico().getNome() : ""));
            ev.put("start", inicio.format(ISO_LOCAL));
            ev.put("end", fim.format(ISO_LOCAL));
            ev.put("backgroundColor", "#0284c7");
            ev.put("borderColor", "#0369a1");
            ev.put("textColor", "#ffffff");
            ev.put("extendedProps", props);
            eventos.add(ev);
        }

        return eventos;
    }

    // área do médico: lista apenas as consultas realizadas pelo médico autenticado
    @GetMapping("/minhas-consultas-medico")
    public ModelAndView minhasConsultasMedico(Principal principal, ModelMap model) {
        String login = principal != null ? principal.getName() : null;
        Medico medico = login != null ? medicoRepository.porUsuario(login) : null;
        if (medico != null) {
            model.addAttribute("medico", medico);
            model.addAttribute("consultas", medicoRepository.consultasDoMedico(medico.getId()));
        } else {
            model.addAttribute("consultas", Collections.emptyList());
        }
        return new ModelAndView("medico/consultas", model);
    }

    // área do médico: lista os pacientes atendidos pelo médico autenticado
    @GetMapping("/meus-pacientes")
    public ModelAndView meusPacientes(Principal principal, ModelMap model) {
        String login = principal != null ? principal.getName() : null;
        Medico medico = login != null ? medicoRepository.porUsuario(login) : null;
        if (medico != null) {
            model.addAttribute("medico", medico);
            model.addAttribute("pacientes", pacienteRepository.pacientesDoMedico(medico.getId()));
        } else {
            model.addAttribute("pacientes", Collections.emptyList());
        }
        return new ModelAndView("medico/pacientes", model);
    }
}
