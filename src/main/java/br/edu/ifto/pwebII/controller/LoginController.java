package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Paciente;
import br.edu.ifto.pwebII.model.jdbc.repository.PacienteRepository;
import br.edu.ifto.pwebII.service.UsuarioService;
import br.edu.ifto.pwebII.validation.Cadastro;
import jakarta.transaction.Transactional;
import jakarta.validation.groups.Default;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    private final PacienteRepository pacienteRepository;
    private final UsuarioService usuarioService;

    public LoginController(PacienteRepository pacienteRepository, UsuarioService usuarioService) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastroForm(@ModelAttribute("paciente") Paciente paciente) {
        return "cadastro";
    }

    @Transactional
    @PostMapping("/cadastro")
    public String cadastrarPaciente(@Validated({Default.class, Cadastro.class}) @ModelAttribute("paciente") Paciente paciente,
                                    BindingResult result) {
        // Sempre atribui o perfil PACIENTE no auto-cadastro público
        usuarioService.criarCredenciais(paciente, UsuarioService.PERFIL_PACIENTE, result);
        if (result.hasErrors()) {
            return "cadastro";
        }

        pacienteRepository.save(paciente);
        return "redirect:/login?cadastrado";
    }
}
