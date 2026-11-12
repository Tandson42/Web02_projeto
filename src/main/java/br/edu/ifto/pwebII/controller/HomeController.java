package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Medico;
import br.edu.ifto.pwebII.model.entity.Paciente;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.PacienteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Collections;

@Controller
public class HomeController {

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;

    public HomeController(PacienteRepository pacienteRepository, MedicoRepository medicoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
    }

    // carrega o menu principal na raiz da aplicação
    @GetMapping("/")
    public ModelAndView menu() {
        return new ModelAndView("menu");
    }

    // área do paciente: lista apenas as consultas do próprio paciente autenticado
    @GetMapping("/minhas-consultas")
    public ModelAndView minhasConsultas(Principal principal, ModelMap model) {
        String login = principal != null ? principal.getName() : null;
        Paciente paciente = login != null ? pacienteRepository.porUsuario(login) : null;
        if (paciente != null) {
            model.addAttribute("paciente", paciente);
            model.addAttribute("consultas", pacienteRepository.consultasDoPaciente(paciente.getId()));
        } else {
            model.addAttribute("consultas", Collections.emptyList());
        }
        return new ModelAndView("paciente/consultas", model);
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