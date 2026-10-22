package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Consulta;
import br.edu.ifto.pwebII.model.jdbc.repository.ConsultaRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.PacienteRepository;
import br.edu.ifto.pwebII.validation.Edicao;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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

/**
 * Controller MVC para a entidade Consulta.
 * A consulta está associada a um Paciente e a um Medico, por isso o controller
 * também injeta os repositories dessas entidades para popular os <select>.
 * Diferença de implementação: usamos ***injeção por construtor***.
 */
@Controller
@RequestMapping("consulta")
public class ConsultaController {

    private final ConsultaRepository repository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;

    public ConsultaController(ConsultaRepository repository,
                              PacienteRepository pacienteRepository,
                              MedicoRepository medicoRepository) {
        this.repository = repository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
    }

    //carrega a página form.html da consulta (cadastro novo)
    @GetMapping("form")
    public ModelAndView form(@ModelAttribute("consulta") Consulta consulta, ModelMap model) {
        model.addAttribute("pacientes", pacienteRepository.pacientes());
        model.addAttribute("medicos", medicoRepository.medicos());
        return new ModelAndView("consulta/form", model);
    }

    //listagem das consultas
    @GetMapping("/list")
    public ModelAndView list(ModelMap model) {
        model.addAttribute("consultas", repository.consultas());
        return new ModelAndView("/consulta/list", model);
    }

    //cadastra uma nova consulta
    @Transactional
    @PostMapping("save")
    public ModelAndView save(@Valid @ModelAttribute("consulta") Consulta consulta,
                             BindingResult result,
                             RedirectAttributes attributes,
                             ModelMap model) {
        if (result.hasErrors()) {
            populaSelects(model);
            return new ModelAndView("consulta/form", model);
        }

        repository.save(consulta);
        attributes.addFlashAttribute("mensagem", "Consulta cadastrada com sucesso.");
        return new ModelAndView("redirect:/consulta/list");
    }

    //carrega o formulário preenchido para edição
    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable("id") Long id, ModelMap model) {
        model.addAttribute("consulta", repository.consulta(id));
        populaSelects(model);
        return new ModelAndView("/consulta/form", model);
    }

    //atualiza uma consulta existente (a edição exige o identificador)
    @Transactional
    @PostMapping("update")
    public ModelAndView update(@Validated({Default.class, Edicao.class}) @ModelAttribute("consulta") Consulta consulta,
                               BindingResult result,
                               RedirectAttributes attributes,
                               ModelMap model) {
        if (result.hasErrors()) {
            populaSelects(model);
            return new ModelAndView("consulta/form", model);
        }

        repository.update(consulta);
        attributes.addFlashAttribute("mensagem", "Consulta atualizada com sucesso.");
        return new ModelAndView("redirect:/consulta/list");
    }

    //alimenta os <select> de paciente e médico (inclusive quando há erros)
    private void populaSelects(ModelMap model) {
        model.addAttribute("pacientes", pacienteRepository.pacientes());
        model.addAttribute("medicos", medicoRepository.medicos());
    }

    //exclui uma consulta
    @Transactional
    @GetMapping("/remove/{id}")
    public ModelAndView remove(@PathVariable("id") Long id,  ModelMap model) {
//        if (erro ao remover){
//            model.addAttribute("erro_remover", "Você não tem privilégios para remover");
//        }
        repository.remove(id);
        return new ModelAndView("redirect:/consulta/list");
    }
}
