package br.edu.ifto.pwebII.controller;

import br.edu.ifto.pwebII.model.dto.UsuarioForm;
import br.edu.ifto.pwebII.model.entity.Perfil;
import br.edu.ifto.pwebII.model.entity.Usuario;
import br.edu.ifto.pwebII.model.jdbc.repository.PerfilRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.UsuarioRepository;
import br.edu.ifto.pwebII.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Área administrativa de contas de acesso e papéis.
 * O acesso é restrito a ROLE_ADMIN em SecurityConfiguration.
 */
@Controller
@RequestMapping("usuario")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioRepository usuarioRepository,
                             PerfilRepository perfilRepository,
                             UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/list")
    public ModelAndView list(ModelMap model) {
        model.addAttribute("usuarios", usuarioRepository.usuarios());
        return new ModelAndView("/usuario/list", model);
    }

    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable("id") Long id, ModelMap model,
                             RedirectAttributes attributes) {
        Usuario usuario = usuarioRepository.usuario(id);
        if (usuario == null) {
            attributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return new ModelAndView("redirect:/usuario/list");
        }

        model.addAttribute("usuarioForm", paraFormulario(usuario));
        model.addAttribute("perfis", perfilRepository.perfis());
        return new ModelAndView("/usuario/edit", model);
    }

    @PostMapping("update")
    public ModelAndView update(@Valid @ModelAttribute("usuarioForm") UsuarioForm formulario,
                               BindingResult result,
                               @AuthenticationPrincipal Usuario administradorLogado,
                               ModelMap model,
                               RedirectAttributes attributes) {
        Usuario usuario = formulario.getId() != null
                ? usuarioRepository.usuario(formulario.getId())
                : null;

        if (usuario == null) {
            attributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return new ModelAndView("redirect:/usuario/list");
        }

        if (!result.hasErrors()) {
            Long idAdministrador = administradorLogado != null ? administradorLogado.getId() : null;
            usuarioService.atualizarConta(formulario, usuario, idAdministrador, result);
        }

        if (result.hasErrors()) {
            model.addAttribute("perfis", perfilRepository.perfis());
            return new ModelAndView("/usuario/edit", model);
        }

        attributes.addFlashAttribute("mensagem", "Usuário atualizado com sucesso.");
        return new ModelAndView("redirect:/usuario/list");
    }

    private UsuarioForm paraFormulario(Usuario usuario) {
        UsuarioForm formulario = new UsuarioForm();
        formulario.setId(usuario.getId());
        formulario.setUsuario(usuario.getUsuario());

        List<Long> idsPerfis = new ArrayList<>();
        for (Perfil perfil : usuario.getPerfis()) {
            idsPerfis.add(perfil.getId());
        }
        formulario.setPerfis(idsPerfis);
        return formulario;
    }
}