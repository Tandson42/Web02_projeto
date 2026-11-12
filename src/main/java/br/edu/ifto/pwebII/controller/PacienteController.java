package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Paciente;
import br.edu.ifto.pwebII.model.jdbc.repository.PacienteRepository;
import br.edu.ifto.pwebII.service.UsuarioService;
import br.edu.ifto.pwebII.validation.Cadastro;
import br.edu.ifto.pwebII.validation.Edicao;
import jakarta.transaction.Transactional;
import jakarta.validation.groups.Default;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("paciente")
public class PacienteController {

    private final PacienteRepository repository;
    private final UsuarioService usuarioService;

    public PacienteController(PacienteRepository repository, UsuarioService usuarioService) {
        this.repository = repository;
        this.usuarioService = usuarioService;
    }

    //carrega a página form.html do paciente (cadastro novo)
    @GetMapping("form")
    public ModelAndView form(@ModelAttribute("paciente") Paciente paciente) {
        return new ModelAndView("paciente/form");
    }

    //listagem dos pacientes
    @GetMapping("/list")
    public ModelAndView list(ModelMap model) {
        model.addAttribute("pacientes", repository.pacientes());
        return new ModelAndView("/paciente/list", model);
    }

    //cadastra um novo paciente
    @Transactional
    @PostMapping("save")
    public ModelAndView save(@Validated({Default.class, Cadastro.class}) @ModelAttribute("paciente") Paciente paciente,
                             BindingResult result,
                             RedirectAttributes attributes) {
        usuarioService.criarCredenciais(paciente, result);
        if (result.hasErrors()) {
            return new ModelAndView("paciente/form");
        }

        repository.save(paciente);
        attributes.addFlashAttribute("mensagem", "Paciente cadastrado com sucesso.");
        return new ModelAndView("redirect:/paciente/list");
    }

    //carrega o formulário preenchido para edição
    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable("id") Long id, ModelMap model) {
        model.addAttribute("paciente", repository.paciente(id));
        return new ModelAndView("/paciente/form", model);
    }

    //atualiza um paciente existente (a edição exige o identificador)
    @Transactional
    @PostMapping("update")
    public ModelAndView update(@Validated({Default.class, Edicao.class}) @ModelAttribute("paciente") Paciente paciente,
                               BindingResult result,
                               RedirectAttributes attributes) {
        if (result.hasErrors()) {
            return new ModelAndView("paciente/form");
        }

        Paciente existente = repository.paciente(paciente.getId());
        if (existente == null) {
            result.reject("paciente.naoEncontrado", "Paciente não encontrado.");
            return new ModelAndView("paciente/form");
        }

        usuarioService.atualizarCredenciais(paciente, existente, result);
        if (result.hasErrors()) {
            return new ModelAndView("paciente/form");
        }

        repository.update(paciente);
        attributes.addFlashAttribute("mensagem", "Paciente atualizado com sucesso.");
        return new ModelAndView("redirect:/paciente/list");
    }

    //exclui um paciente
    @Transactional
    @GetMapping("/remove/{id}")
    public ModelAndView remove(@PathVariable("id") Long id) {
        repository.remove(id);
        return new ModelAndView("redirect:/paciente/list");
    }

    @GetMapping("/consultas/{id}")
    public ModelAndView consultas(@PathVariable("id") Long id, ModelMap model) {
        Paciente paciente = repository.paciente(id);
        model.addAttribute("paciente", paciente);
        model.addAttribute("consultas", repository.consultasDoPaciente(id));
        return new ModelAndView("/paciente/consultas", model);
    }
}
