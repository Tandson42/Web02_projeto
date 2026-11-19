package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.*;
import br.edu.ifto.pwebII.model.jdbc.repository.*;
import br.edu.ifto.pwebII.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller MVC responsável pela gestão de disponibilidade de horários (Agenda)
 * e pela visualização unificada de calendário (horários disponíveis, agendamentos e consultas).
 */
@Controller
@RequestMapping("agenda")
public class AgendaController {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AgendaRepository agendaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ConsultaRepository consultaRepository;
    private final MedicoRepository medicoRepository;
    private final MedicoSecretarioRepository medicoSecretarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendaService agendaService;

    public AgendaController(AgendaRepository agendaRepository,
                            AgendamentoRepository agendamentoRepository,
                            ConsultaRepository consultaRepository,
                            MedicoRepository medicoRepository,
                            MedicoSecretarioRepository medicoSecretarioRepository,
                            UsuarioRepository usuarioRepository,
                            AgendaService agendaService) {
        this.agendaRepository = agendaRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.consultaRepository = consultaRepository;
        this.medicoRepository = medicoRepository;
        this.medicoSecretarioRepository = medicoSecretarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendaService = agendaService;
    }

    /**
     * Exibe a Agenda de Consultas (calendário estilo Google Agenda + visão em lista).
     * - ADMIN: todos os horários e consultas
     * - SECRETARIO: todos os horários (com alteração apenas onde o médico permitiu)
     * - MEDICO: somente os seus próprios horários, agendamentos e consultas
     */
    @GetMapping("/list")
    public ModelAndView list(Authentication authentication, ModelMap model) {
        if (isAdmin(authentication)) {
            model.addAttribute("agendas", agendaRepository.agendas());
            model.addAttribute("agendamentosClinica", agendamentoRepository.agendamentos());
            model.addAttribute("historicoConsultas", consultaRepository.consultas());
            model.addAttribute("medicosModal", medicoRepository.medicos());
            model.addAttribute("ehClinica", true);
        } else if (isSecretario(authentication)) {
            model.addAttribute("agendas", agendaRepository.agendas());
            model.addAttribute("agendamentosClinica", agendamentoRepository.agendamentos());
            model.addAttribute("historicoConsultas", consultaRepository.consultas());
            Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
            Set<Long> idsPermitidos = usuarioLogado != null
                    ? medicoSecretarioRepository.idsMedicosComPermissaoAgenda(usuarioLogado.getId())
                    : Collections.emptySet();
            List<Medico> medicosPermitidos = usuarioLogado != null
                    ? medicoSecretarioRepository.medicosComPermissaoAgenda(usuarioLogado.getId())
                    : Collections.emptyList();
            Set<Long> idsAssociados = usuarioLogado != null
                    ? medicoSecretarioRepository.idsMedicosAssociados(usuarioLogado.getId())
                    : Collections.emptySet();
            model.addAttribute("idsMedicosComPermissaoAgenda", idsPermitidos);
            model.addAttribute("idsMedicosAssociados", idsAssociados);
            model.addAttribute("medicosModal", medicosPermitidos);
            model.addAttribute("ehClinica", true);
        } else {
            Medico medico = medicoRepository.porUsuario(authentication.getName());
            model.addAttribute("medicoLogado", medico);
            model.addAttribute("ehClinica", false);
            if (medico != null) {
                model.addAttribute("agendas", agendaRepository.agendasPorMedico(medico.getId()));
                model.addAttribute("historicoConsultas", medicoRepository.consultasDoMedico(medico.getId()));
            } else {
                model.addAttribute("agendas", Collections.emptyList());
                model.addAttribute("historicoConsultas", Collections.emptyList());
            }
        }
        return new ModelAndView("agenda/list", model);
    }

    /**
     * Fornece os eventos JSON para o calendário (FullCalendar):
     * - Horários DISPONIVEL (verde)
     * - Agendamentos AGENDADO (âmbar)
     * - Consultas registradas / realizadas (azul/índigo)
     */
    @GetMapping("/eventos")
    @ResponseBody
    public List<Map<String, Object>> eventos(Authentication authentication) {
        List<Map<String, Object>> eventos = new ArrayList<>();
        boolean admin = isAdmin(authentication);
        boolean secretario = isSecretario(authentication);
        boolean mostrarNomeMedico = admin || secretario;

        List<Agenda> agendas;
        List<Agendamento> agendamentos;
        List<Consulta> consultas;
        Set<Long> idsPermitidosAgenda = null;
        Set<Long> idsAssociados = null;

        if (admin) {
            agendas = agendaRepository.agendas();
            agendamentos = agendamentoRepository.agendamentos();
            consultas = consultaRepository.consultas();
        } else if (secretario) {
            agendas = agendaRepository.agendas();
            agendamentos = agendamentoRepository.agendamentos();
            consultas = consultaRepository.consultas();
            Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
            idsPermitidosAgenda = usuarioLogado != null
                    ? medicoSecretarioRepository.idsMedicosComPermissaoAgenda(usuarioLogado.getId())
                    : Collections.emptySet();
            idsAssociados = usuarioLogado != null
                    ? medicoSecretarioRepository.idsMedicosAssociados(usuarioLogado.getId())
                    : Collections.emptySet();
        } else {
            Medico medico = medicoRepository.porUsuario(authentication.getName());
            if (medico == null) {
                return eventos;
            }
            agendas = agendaRepository.agendasPorMedico(medico.getId());
            agendamentos = agendamentoRepository.agendamentosPorMedico(medico.getId());
            consultas = medicoRepository.consultasDoMedico(medico.getId());
        }

        // 1. Horários disponíveis na agenda (DISPONIVEL)
        for (Agenda a : agendas) {
            if (!a.isDisponivel() || a.getDataHora() == null) {
                continue;
            }
            LocalDateTime inicio = a.getDataHora();
            LocalDateTime fim = inicio.plusMinutes(30);

            boolean podeRemover = admin || !secretario
                    || (idsPermitidosAgenda != null && idsPermitidosAgenda.contains(a.getMedico().getId()));

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("tipo", "DISPONIVEL");
            props.put("statusLabel", "Disponível");
            props.put("agendaId", a.getId());
            props.put("medico", a.getMedico().getNome());
            props.put("crm", a.getMedico().getCrm());
            props.put("valor", a.getMedico().getValorConsulta());
            props.put("podeRemover", podeRemover);
            props.put("podeAgendar", admin || secretario);

            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id", "agenda-" + a.getId());
            ev.put("title", mostrarNomeMedico ? "Livre · " + a.getMedico().getNome() : "Horário Disponível");
            ev.put("start", inicio.format(ISO_LOCAL));
            ev.put("end", fim.format(ISO_LOCAL));
            ev.put("backgroundColor", "#10b981");
            ev.put("borderColor", "#059669");
            ev.put("textColor", "#ffffff");
            ev.put("extendedProps", props);
            eventos.add(ev);
        }

        // 2. Agendamentos pendentes de confirmação (AGENDADO)
        for (Agendamento ag : agendamentos) {
            if (ag.getStatus() != StatusAgendamento.AGENDADO || ag.getAgenda().getDataHora() == null) {
                continue;
            }
            LocalDateTime inicio = ag.getAgenda().getDataHora();
            LocalDateTime fim = inicio.plusMinutes(30);

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("tipo", "AGENDADO");
            props.put("statusLabel", "Agendado (aguardando presença)");
            props.put("agendamentoId", ag.getId());
            props.put("medico", ag.getAgenda().getMedico().getNome());
            props.put("crm", ag.getAgenda().getMedico().getCrm());
            props.put("paciente", ag.getPaciente().getNome());
            props.put("valor", ag.getValor());
            props.put("podeConfirmar", admin || secretario);
            props.put("podeCancelar", admin || secretario);

            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id", "agendamento-" + ag.getId());
            ev.put("title", ag.getPaciente().getNome()
                    + (mostrarNomeMedico ? " · " + ag.getAgenda().getMedico().getNome() : ""));
            ev.put("start", inicio.format(ISO_LOCAL));
            ev.put("end", fim.format(ISO_LOCAL));
            ev.put("backgroundColor", "#f59e0b");
            ev.put("borderColor", "#d97706");
            ev.put("textColor", "#ffffff");
            ev.put("extendedProps", props);
            eventos.add(ev);
        }

        // 3. Consultas efetivamente realizadas/registradas
        for (Consulta c : consultas) {
            if (c.getData() == null) {
                continue;
            }
            LocalDateTime inicio = c.getData();
            LocalDateTime fim = inicio.plusMinutes(30);

            boolean podeEditarObs = !admin && !secretario;
            boolean podeVerObs = admin || podeEditarObs
                    || (c.getMedico() != null && idsAssociados != null && idsAssociados.contains(c.getMedico().getId()));

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("tipo", "CONSULTA");
            props.put("statusLabel", "Consulta Realizada");
            props.put("consultaId", c.getId());
            props.put("medico", c.getMedico() != null ? c.getMedico().getNome() : "-");
            props.put("crm", c.getMedico() != null ? c.getMedico().getCrm() : "-");
            props.put("paciente", c.getPaciente() != null ? c.getPaciente().getNome() : "-");
            props.put("valor", c.getValor());
            props.put("podeEditarObs", podeEditarObs);
            props.put("podeVerObs", podeVerObs);
            if (podeVerObs) {
                props.put("observacao", c.getObservacao() != null && !c.getObservacao().isBlank()
                        ? c.getObservacao()
                        : "Nenhuma observação registrada.");
            }

            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id", "consulta-" + c.getId());
            ev.put("title", (c.getPaciente() != null ? c.getPaciente().getNome() : "Consulta")
                    + (mostrarNomeMedico && c.getMedico() != null ? " · " + c.getMedico().getNome() : " (Realizada)"));
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

    /**
     * Exibe apenas os horários com status DISPONIVEL para agendamento.
     */
    @GetMapping("/disponiveis")
    public ModelAndView disponiveis(ModelMap model) {
        model.addAttribute("agendas", agendaRepository.disponiveis());
        return new ModelAndView("agenda/disponiveis", model);
    }

    /**
     * Abre o formulário de cadastro de novo horário disponível.
     */
    @GetMapping("/form")
    public ModelAndView form(@ModelAttribute("agenda") Agenda agenda,
                             Authentication authentication,
                             ModelMap model) {
        prepararFormulario(agenda, authentication, model);
        return new ModelAndView("agenda/form", model);
    }

    /**
     * Salva um novo horário disponível na agenda, impedindo duplicidades
     * e validando permissão do secretário quando aplicável.
     */
    @PostMapping("/save")
    public ModelAndView save(@Valid @ModelAttribute("agenda") Agenda agenda,
                             BindingResult result,
                             Authentication authentication,
                             ModelMap model,
                             RedirectAttributes attributes) {
        if (!isAdmin(authentication) && !isSecretario(authentication)) {
            Medico medicoLogado = medicoRepository.porUsuario(authentication.getName());
            if (medicoLogado != null) {
                agenda.setMedico(medicoLogado);
            }
        } else if (isSecretario(authentication) && !isAdmin(authentication)) {
            Long idMedico = agenda.getMedico() != null ? agenda.getMedico().getId() : null;
            Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
            if (idMedico != null && (usuarioLogado == null
                    || !medicoSecretarioRepository.podeAlterarAgenda(idMedico, usuarioLogado.getId()))) {
                result.rejectValue("medico", "agenda.semPermissao",
                        "Você não tem permissão deste médico para alterar a agenda dele.");
            }
        }

        agendaService.cadastrarHorario(agenda, result);
        if (result.hasErrors()) {
            prepararFormulario(agenda, authentication, model);
            return new ModelAndView("agenda/form", model);
        }

        attributes.addFlashAttribute("mensagem", "Horário disponível cadastrado com sucesso na agenda!");
        return new ModelAndView("redirect:/agenda/list");
    }

    /**
     * Remove um horário da agenda somente se ainda estiver disponível, sem agendamentos
     * e com permissão válida do usuário autenticado.
     */
    @PostMapping("/remove/{id}")
    public ModelAndView remove(@PathVariable("id") Long id,
                               Authentication authentication,
                               RedirectAttributes attributes) {
        try {
            if (isSecretario(authentication) && !isAdmin(authentication)) {
                Agenda agenda = agendaRepository.agenda(id);
                Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
                if (agenda != null && (usuarioLogado == null
                        || !medicoSecretarioRepository.podeAlterarAgenda(agenda.getMedico().getId(), usuarioLogado.getId()))) {
                    throw new IllegalStateException(
                            "Você não tem permissão deste médico para alterar a agenda dele.");
                }
            }

            Medico medicoRestrito = (isAdmin(authentication) || isSecretario(authentication))
                    ? null
                    : medicoRepository.porUsuario(authentication.getName());
            agendaService.removerHorario(id, medicoRestrito);
            attributes.addFlashAttribute("mensagem", "Horário removido da agenda com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
        }
        return new ModelAndView("redirect:/agenda/list");
    }

    private void prepararFormulario(Agenda agenda, Authentication authentication, ModelMap model) {
        if (isAdmin(authentication)) {
            model.addAttribute("medicos", medicoRepository.medicos());
        } else if (isSecretario(authentication)) {
            Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
            List<Medico> permitidos = usuarioLogado != null
                    ? medicoSecretarioRepository.medicosComPermissaoAgenda(usuarioLogado.getId())
                    : Collections.emptyList();
            model.addAttribute("medicos", permitidos);
            model.addAttribute("secretarioSemPermissao", permitidos.isEmpty());
        } else {
            Medico medicoLogado = medicoRepository.porUsuario(authentication.getName());
            if (medicoLogado != null) {
                agenda.setMedico(medicoLogado);
                model.addAttribute("medicoLogado", medicoLogado);
            }
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    private boolean isSecretario(Authentication authentication) {
        return hasRole(authentication, "ROLE_SECRETARIO");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
