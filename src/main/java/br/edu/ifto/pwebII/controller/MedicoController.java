package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Medico;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoRepository;
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

@Controller
@RequestMapping("medico")
public class MedicoController {

    private final MedicoRepository repository;

    public MedicoController(MedicoRepository repository) {
        this.repository = repository;
    }

    //carrega a página form.html do médico (cadastro novo)
    @GetMapping("form")
    public ModelAndView form(@ModelAttribute("medico") Medico medico) {
        return new ModelAndView("medico/form");
    }

    //listagem dos médicos
    @GetMapping("/list")
    public ModelAndView list(ModelMap model) {
        model.addAttribute("medicos", repository.medicos());
        return new ModelAndView("/medico/list", model);
    }

    //cadastra um novo médico
    @Transactional
    @PostMapping("save")
    public ModelAndView save(@Valid @ModelAttribute("medico") Medico medico,
                             BindingResult result,
                             RedirectAttributes attributes) {
        if (result.hasErrors()) {
            return new ModelAndView("medico/form");
        }

        repository.save(medico);
        attributes.addFlashAttribute("mensagem", "Médico cadastrado com sucesso.");
        return new ModelAndView("redirect:/medico/list");
    }

    //carrega o formulário preenchido para edição
    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable("id") Long id, ModelMap model) {
        model.addAttribute("medico", repository.medico(id));
        return new ModelAndView("/medico/form", model);
    }

    //atualiza um médico existente (a edição exige o identificador)
    @Transactional
    @PostMapping("update")
    public ModelAndView update(@Validated({Default.class, Edicao.class}) @ModelAttribute("medico") Medico medico,
                               BindingResult result,
                               RedirectAttributes attributes) {
        if (result.hasErrors()) {
            return new ModelAndView("medico/form");
        }

        repository.update(medico);
        attributes.addFlashAttribute("mensagem", "Médico atualizado com sucesso.");
        return new ModelAndView("redirect:/medico/list");
    }

    //exclui um médico
    @Transactional
    @GetMapping("/remove/{id}")
    public ModelAndView remove(@PathVariable("id") Long id) {
        repository.remove(id);
        return new ModelAndView("redirect:/medico/list");
    }


    @GetMapping("/consultas/{id}")
    public ModelAndView consultas(@PathVariable("id") Long id, ModelMap model) {
        Medico medico = repository.medico(id);
        model.addAttribute("medico", medico);
        model.addAttribute("consultas", repository.consultasDoMedico(id));
        return new ModelAndView("/medico/consultas", model);
    }
}
