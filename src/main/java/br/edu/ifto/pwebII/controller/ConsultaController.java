package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Consulta;
import br.edu.ifto.pwebII.model.entity.Medico;
import br.edu.ifto.pwebII.model.entity.Usuario;
import br.edu.ifto.pwebII.model.jdbc.repository.ConsultaRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoSecretarioRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.PacienteRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.UsuarioRepository;
import br.edu.ifto.pwebII.validation.Edicao;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller MVC para a entidade Consulta.
 * Inclui área de observações clínicas: apenas o médico da consulta pode editar;
 * o secretário associado ao médico pode visualizar em modo somente leitura.
 */
@Controller
@RequestMapping("consulta")
public class ConsultaController {

    private final ConsultaRepository repository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final MedicoSecretarioRepository medicoSecretarioRepository;
    private final UsuarioRepository usuarioRepository;

    public ConsultaController(ConsultaRepository repository,
                              PacienteRepository pacienteRepository,
                              MedicoRepository medicoRepository,
                              MedicoSecretarioRepository medicoSecretarioRepository,
                              UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.medicoSecretarioRepository = medicoSecretarioRepository;
        this.usuarioRepository = usuarioRepository;
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

    //atualiza uma consulta existente (preserva a observação médica se editado por secretário)
    @Transactional
    @PostMapping("update")
    public ModelAndView update(@Validated({Default.class, Edicao.class}) @ModelAttribute("consulta") Consulta consulta,
                               BindingResult result,
                               Authentication authentication,
                               RedirectAttributes attributes,
                               ModelMap model) {
        if (result.hasErrors()) {
            populaSelects(model);
            return new ModelAndView("consulta/form", model);
        }

        // Secretário não pode alterar as observações clínicas registradas pelo médico
        if (hasRole(authentication, "ROLE_SECRETARIO") && !hasRole(authentication, "ROLE_ADMIN")) {
            Consulta existente = repository.consulta(consulta.getId());
            if (existente != null) {
                consulta.setObservacao(existente.getObservacao());
            }
        }

        repository.update(consulta);
        attributes.addFlashAttribute("mensagem", "Consulta atualizada com sucesso.");
        return new ModelAndView("redirect:/consulta/list");
    }

    // =========================================================================
    // Área de Observações da Consulta (Médico edita; Secretário associado visualiza)
    // =========================================================================

    @GetMapping("/observacao/{id}")
    public ModelAndView observacaoForm(@PathVariable("id") Long id,
                                       Authentication authentication,
                                       ModelMap model,
                                       RedirectAttributes attributes) {
        Consulta consulta = repository.consulta(id);
        if (consulta == null) {
            attributes.addFlashAttribute("erro", "Consulta não encontrada.");
            return new ModelAndView("redirect:/agenda/list");
        }

        boolean podeEditar = ehMedicoDaConsulta(consulta, authentication);
        boolean podeVisualizar = podeEditar
                || hasRole(authentication, "ROLE_ADMIN")
                || ehSecretarioDoMedico(consulta, authentication);

        if (!podeVisualizar) {
            attributes.addFlashAttribute("erro",
                    "Você não tem permissão para visualizar as observações desta consulta.");
            return new ModelAndView("redirect:/agenda/list");
        }

        model.addAttribute("consulta", consulta);
        model.addAttribute("podeEditar", podeEditar);
        return new ModelAndView("consulta/observacao", model);
    }

    @Transactional
    @PostMapping("/observacao/{id}")
    public ModelAndView salvarObservacao(@PathVariable("id") Long id,
                                         @RequestParam(value = "observacao", required = false) String observacao,
                                         Authentication authentication,
                                         RedirectAttributes attributes) {
        Consulta consulta = repository.consulta(id);
        if (consulta == null) {
            attributes.addFlashAttribute("erro", "Consulta não encontrada.");
            return new ModelAndView("redirect:/agenda/list");
        }

        if (!ehMedicoDaConsulta(consulta, authentication)) {
            attributes.addFlashAttribute("erro",
                    "Somente o médico responsável pela consulta pode alterar as observações.");
            return new ModelAndView("redirect:/agenda/list");
        }

        if (observacao != null && observacao.length() > 500) {
            attributes.addFlashAttribute("erro", "A observação deve ter no máximo 500 caracteres.");
            return new ModelAndView("redirect:/consulta/observacao/" + id);
        }

        consulta.setObservacao(observacao != null ? observacao.trim() : "");
        repository.update(consulta);

        attributes.addFlashAttribute("mensagem", "Observações da consulta salvas com sucesso!");
        return new ModelAndView("redirect:/agenda/list");
    }

    private boolean ehMedicoDaConsulta(Consulta consulta, Authentication authentication) {
        if (authentication == null || !hasRole(authentication, "ROLE_MEDICO") || consulta.getMedico() == null) {
            return false;
        }
        Medico medicoLogado = medicoRepository.porUsuario(authentication.getName());
        return medicoLogado != null && medicoLogado.getId().equals(consulta.getMedico().getId());
    }

    private boolean ehSecretarioDoMedico(Consulta consulta, Authentication authentication) {
        if (authentication == null || !hasRole(authentication, "ROLE_SECRETARIO") || consulta.getMedico() == null) {
            return false;
        }
        Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
        return usuarioLogado != null
                && medicoSecretarioRepository.existeVinculo(consulta.getMedico().getId(), usuarioLogado.getId());
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }

    //alimenta os <select> de paciente e médico (inclusive quando há erros)
    private void populaSelects(ModelMap model) {
        model.addAttribute("pacientes", pacienteRepository.pacientes());
        model.addAttribute("medicos", medicoRepository.medicos());
    }

    //exclui uma consulta
    @Transactional
    @GetMapping("/remove/{id}")
    public ModelAndView remove(@PathVariable("id") Long id, ModelMap model) {
        repository.remove(id);
        return new ModelAndView("redirect:/consulta/list");
    }
}
