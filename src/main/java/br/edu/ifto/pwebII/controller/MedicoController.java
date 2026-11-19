package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.entity.Medico;
import br.edu.ifto.pwebII.model.entity.MedicoSecretario;
import br.edu.ifto.pwebII.model.entity.Usuario;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.MedicoSecretarioRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.UsuarioRepository;
import br.edu.ifto.pwebII.service.UsuarioService;
import br.edu.ifto.pwebII.validation.Cadastro;
import br.edu.ifto.pwebII.validation.Edicao;
import jakarta.transaction.Transactional;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("medico")
public class MedicoController {

    private final MedicoRepository repository;
    private final UsuarioService usuarioService;
    private final MedicoSecretarioRepository medicoSecretarioRepository;
    private final UsuarioRepository usuarioRepository;

    public MedicoController(MedicoRepository repository,
                            UsuarioService usuarioService,
                            MedicoSecretarioRepository medicoSecretarioRepository,
                            UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioService = usuarioService;
        this.medicoSecretarioRepository = medicoSecretarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    //carrega a página form.html do médico (cadastro novo)
    @GetMapping("form")
    public ModelAndView form(@ModelAttribute("medico") Medico medico) {
        return new ModelAndView("medico/form");
    }

    //listagem dos médicos
    @GetMapping("/list")
    public ModelAndView list(Authentication authentication, ModelMap model) {
        model.addAttribute("medicos", repository.medicos());
        if (authentication != null && !hasRole(authentication, "ROLE_ADMIN") && hasRole(authentication, "ROLE_SECRETARIO")) {
            Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
            Set<Long> idsComPermissaoPreco = usuarioLogado != null
                    ? medicoSecretarioRepository.idsMedicosComPermissaoPreco(usuarioLogado.getId())
                    : Collections.emptySet();
            model.addAttribute("idsComPermissaoPreco", idsComPermissaoPreco);
        }
        return new ModelAndView("/medico/list", model);
    }

    //cadastra um novo médico
    @Transactional
    @PostMapping("save")
    public ModelAndView save(@Validated({Default.class, Cadastro.class}) @ModelAttribute("medico") Medico medico,
                             BindingResult result,
                             RedirectAttributes attributes) {
        usuarioService.criarCredenciais(medico, UsuarioService.PERFIL_MEDICO, result);
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

        Medico existente = repository.medico(medico.getId());
        if (existente == null) {
            result.reject("medico.naoEncontrado", "Médico não encontrado.");
            return new ModelAndView("medico/form");
        }

        usuarioService.atualizarCredenciais(medico, existente, UsuarioService.PERFIL_MEDICO, result);
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

    // =========================================================================
    // Gestão de Secretários Associados e Permissões pelo próprio Médico
    // =========================================================================

    @GetMapping("/secretarios")
    public ModelAndView secretarios(Authentication authentication, ModelMap model, RedirectAttributes attributes) {
        Medico medico = repository.porUsuario(authentication.getName());
        if (medico == null) {
            attributes.addFlashAttribute("erro", "Conta não vinculada a um cadastro de médico.");
            return new ModelAndView("redirect:/");
        }

        List<MedicoSecretario> vinculos = medicoSecretarioRepository.porMedico(medico.getId());
        Set<Long> idsAssociados = vinculos.stream()
                .map(v -> v.getSecretario().getId())
                .collect(Collectors.toSet());
        List<Usuario> disponiveis = usuarioRepository.usuariosPorPerfil(UsuarioService.PERFIL_SECRETARIO).stream()
                .filter(u -> !idsAssociados.contains(u.getId()))
                .collect(Collectors.toList());

        model.addAttribute("medico", medico);
        model.addAttribute("vinculos", vinculos);
        model.addAttribute("secretariosDisponiveis", disponiveis);
        return new ModelAndView("medico/secretarios", model);
    }

    @Transactional
    @PostMapping("/secretarios/adicionar")
    public ModelAndView adicionarSecretario(@RequestParam("secretarioId") Long secretarioId,
                                            @RequestParam(value = "origem", defaultValue = "secretarios") String origem,
                                            Authentication authentication,
                                            RedirectAttributes attributes) {
        Medico medico = repository.porUsuario(authentication.getName());
        Usuario secretario = usuarioRepository.usuario(secretarioId);
        if (medico == null || secretario == null) {
            attributes.addFlashAttribute("erro", "Médico ou secretário não encontrado.");
            return redirectOrigem(origem);
        }

        boolean ehSecretario = secretario.getPerfis().stream()
                .anyMatch(p -> UsuarioService.PERFIL_SECRETARIO.equalsIgnoreCase(p.getNome()));
        if (!ehSecretario) {
            attributes.addFlashAttribute("erro", "O usuário selecionado não possui perfil de Secretário.");
            return redirectOrigem(origem);
        }

        if (medicoSecretarioRepository.porMedicoESecretario(medico.getId(), secretario.getId()) != null) {
            attributes.addFlashAttribute("erro", "Este secretário já está associado a você.");
            return redirectOrigem(origem);
        }

        MedicoSecretario vinculo = new MedicoSecretario();
        vinculo.setMedico(medico);
        vinculo.setSecretario(secretario);
        vinculo.setPodeAlterarAgenda(false);
        vinculo.setPodeAlterarPreco(false);
        medicoSecretarioRepository.save(vinculo);

        attributes.addFlashAttribute("mensagem",
                "Secretário '" + secretario.getUsuario() + "' associado com sucesso.");
        return redirectOrigem(origem);
    }

    @Transactional
    @PostMapping("/secretarios/{id}/permissao-agenda")
    public ModelAndView alternarPermissaoAgenda(@PathVariable("id") Long id,
                                                @RequestParam(value = "origem", defaultValue = "secretarios") String origem,
                                                Authentication authentication,
                                                RedirectAttributes attributes) {
        Medico medico = repository.porUsuario(authentication.getName());
        MedicoSecretario vinculo = medicoSecretarioRepository.porId(id);
        if (medico == null || vinculo == null || !vinculo.getMedico().getId().equals(medico.getId())) {
            attributes.addFlashAttribute("erro", "Vínculo de secretário não encontrado.");
            return redirectOrigem(origem);
        }

        vinculo.setPodeAlterarAgenda(!vinculo.isPodeAlterarAgenda());
        medicoSecretarioRepository.update(vinculo);
        attributes.addFlashAttribute("mensagem",
                "Permissão de alterar agenda para '" + vinculo.getSecretario().getUsuario() + "' foi "
                        + (vinculo.isPodeAlterarAgenda() ? "ATIVADA." : "DESLIGADA."));
        return redirectOrigem(origem);
    }

    @Transactional
    @PostMapping("/secretarios/{id}/permissao-preco")
    public ModelAndView alternarPermissaoPreco(@PathVariable("id") Long id,
                                               @RequestParam(value = "origem", defaultValue = "secretarios") String origem,
                                               Authentication authentication,
                                               RedirectAttributes attributes) {
        Medico medico = repository.porUsuario(authentication.getName());
        MedicoSecretario vinculo = medicoSecretarioRepository.porId(id);
        if (medico == null || vinculo == null || !vinculo.getMedico().getId().equals(medico.getId())) {
            attributes.addFlashAttribute("erro", "Vínculo de secretário não encontrado.");
            return redirectOrigem(origem);
        }

        vinculo.setPodeAlterarPreco(!vinculo.isPodeAlterarPreco());
        medicoSecretarioRepository.update(vinculo);
        attributes.addFlashAttribute("mensagem",
                "Permissão de alterar preço da consulta para '" + vinculo.getSecretario().getUsuario() + "' foi "
                        + (vinculo.isPodeAlterarPreco() ? "ATIVADA." : "DESLIGADA."));
        return redirectOrigem(origem);
    }

    @Transactional
    @PostMapping("/secretarios/{id}/remover")
    public ModelAndView removerSecretario(@PathVariable("id") Long id,
                                          @RequestParam(value = "origem", defaultValue = "secretarios") String origem,
                                          Authentication authentication,
                                          RedirectAttributes attributes) {
        Medico medico = repository.porUsuario(authentication.getName());
        MedicoSecretario vinculo = medicoSecretarioRepository.porId(id);
        if (medico == null || vinculo == null || !vinculo.getMedico().getId().equals(medico.getId())) {
            attributes.addFlashAttribute("erro", "Vínculo de secretário não encontrado.");
            return redirectOrigem(origem);
        }

        String nomeSec = vinculo.getSecretario().getUsuario();
        medicoSecretarioRepository.remove(id);
        attributes.addFlashAttribute("mensagem", "Secretário '" + nomeSec + "' desvinculado com sucesso.");
        return redirectOrigem(origem);
    }

    // =========================================================================
    // Alteração de preço da consulta (ADMIN, próprio MEDICO ou SECRETARIO autorizado)
    // =========================================================================

    @GetMapping("/preco/{id}")
    public ModelAndView precoForm(@PathVariable("id") Long id,
                                  Authentication authentication,
                                  ModelMap model,
                                  RedirectAttributes attributes) {
        Medico medico = repository.medico(id);
        if (medico == null) {
            attributes.addFlashAttribute("erro", "Médico não encontrado.");
            return new ModelAndView("redirect:/");
        }
        if (!podeAlterarPrecoDoMedico(medico, authentication)) {
            attributes.addFlashAttribute("erro",
                    "Você não tem permissão deste médico para alterar o preço da consulta.");
            return new ModelAndView(hasRole(authentication, "ROLE_SECRETARIO") ? "redirect:/medico/list" : "redirect:/");
        }
        model.addAttribute("medico", medico);
        return new ModelAndView("medico/preco", model);
    }

    @Transactional
    @PostMapping("/preco/{id}")
    public ModelAndView atualizarPreco(@PathVariable("id") Long id,
                                       @RequestParam("valorConsulta") Double valorConsulta,
                                       Authentication authentication,
                                       RedirectAttributes attributes) {
        Medico medico = repository.medico(id);
        if (medico == null) {
            attributes.addFlashAttribute("erro", "Médico não encontrado.");
            return new ModelAndView("redirect:/");
        }
        if (!podeAlterarPrecoDoMedico(medico, authentication)) {
            attributes.addFlashAttribute("erro",
                    "Você não tem permissão deste médico para alterar o preço da consulta.");
            return new ModelAndView(hasRole(authentication, "ROLE_SECRETARIO") ? "redirect:/medico/list" : "redirect:/");
        }
        if (valorConsulta == null || valorConsulta < 0) {
            attributes.addFlashAttribute("erro", "Informe um valor válido (maior ou igual a zero).");
            return new ModelAndView("redirect:/medico/preco/" + id);
        }

        medico.setValorConsulta(valorConsulta);
        repository.update(medico);

        attributes.addFlashAttribute("mensagem",
                "Valor da consulta atualizado com sucesso! A alteração vale apenas para novos agendamentos.");
        if (hasRole(authentication, "ROLE_MEDICO") && !hasRole(authentication, "ROLE_ADMIN")) {
            return new ModelAndView("redirect:/agenda/list");
        }
        return new ModelAndView("redirect:/medico/list");
    }

    private boolean podeAlterarPrecoDoMedico(Medico medico, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (hasRole(authentication, "ROLE_MEDICO")) {
            Medico medicoLogado = repository.porUsuario(authentication.getName());
            if (medicoLogado != null && medicoLogado.getId().equals(medico.getId())) {
                return true;
            }
        }
        if (hasRole(authentication, "ROLE_SECRETARIO")) {
            Usuario usuarioLogado = usuarioRepository.porUsuario(authentication.getName());
            if (usuarioLogado != null) {
                return medicoSecretarioRepository.podeAlterarPreco(medico.getId(), usuarioLogado.getId());
            }
        }
        return false;
    }

    private ModelAndView redirectOrigem(String origem) {
        if ("agenda".equals(origem)) {
            return new ModelAndView("redirect:/agenda/list");
        }
        return new ModelAndView("redirect:/medico/secretarios");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
