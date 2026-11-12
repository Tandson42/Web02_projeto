package br.edu.ifto.pwebII.service;

import br.edu.ifto.pwebII.model.dto.UsuarioForm;
import br.edu.ifto.pwebII.model.entity.Perfil;
import br.edu.ifto.pwebII.model.entity.Pessoa;
import br.edu.ifto.pwebII.model.entity.Usuario;
import br.edu.ifto.pwebII.model.jdbc.repository.PerfilRepository;
import br.edu.ifto.pwebII.model.jdbc.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Centraliza a criação e a atualização das credenciais de acesso,
 * tanto no cadastro de pessoas quanto na área administrativa de usuários.
 */
@Service
public class UsuarioService {

    private static final String PERFIL_INICIAL = "USER";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PerfilRepository perfilRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria a conta de acesso de uma nova pessoa, com senha BCrypt e perfil inicial USER.
     */
    @Transactional
    public void criarCredenciais(Pessoa pessoa, BindingResult result) {
        if (result.hasErrors()) {
            return;
        }

        if (pessoa.getSenha() == null || pessoa.getSenha().isBlank()) {
            result.rejectValue("senha", "senha.obrigatoria", "Informe a senha de acesso.");
            return;
        }
        validarSenhaOpcional(pessoa.getSenha(), result);

        validarLoginUnico(pessoa.getLogin(), null, result);
        if (result.hasErrors()) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsuario(pessoa.getLogin().trim());
        usuario.setSenha(passwordEncoder.encode(pessoa.getSenha()));
        usuario.getPerfis().add(perfilInicial());
        pessoa.setUsuario(usuario);
    }

    /**
     * Atualiza o login e, opcionalmente, a senha da conta já vinculada à pessoa.
     * Se a pessoa ainda não tiver conta (dados antigos), ela é criada desde que
     * login e senha sejam informados.
     */
    @Transactional
    public void atualizarCredenciais(Pessoa formulario, Pessoa existente, BindingResult result) {
        if (result.hasErrors()) {
            return;
        }

        Usuario usuario = existente.getUsuario();

        if (usuario == null) {
            if (formulario.getSenha() == null || formulario.getSenha().isBlank()) {
                result.rejectValue("senha", "senha.obrigatoria",
                        "Informe a senha para criar a conta de acesso desta pessoa.");
            } else {
                validarSenhaOpcional(formulario.getSenha(), result);
            }
            if (result.hasErrors()) {
                return;
            }

            validarLoginUnico(formulario.getLogin(), null, result);
            if (result.hasErrors()) {
                return;
            }

            Usuario novo = new Usuario();
            novo.setUsuario(formulario.getLogin().trim());
            novo.setSenha(passwordEncoder.encode(formulario.getSenha()));
            novo.getPerfis().add(perfilInicial());
            existente.setUsuario(novo);
            formulario.setUsuario(novo);
            return;
        }

        validarLoginUnico(formulario.getLogin(), usuario.getId(), result);
        validarSenhaOpcional(formulario.getSenha(), result);
        if (result.hasErrors()) {
            return;
        }

        usuario.setUsuario(formulario.getLogin().trim());
        if (formulario.getSenha() != null && !formulario.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(formulario.getSenha()));
        }

        // O controller fará o merge do formulário; por isso ele recebe a conta gerenciada.
        formulario.setUsuario(usuario);
    }

    /**
     * Atualiza login, papéis e, opcionalmente, a senha de uma conta pela área administrativa.
     * Protege o administrador logado e impede que o sistema fique sem nenhum ADMIN.
     */
    @Transactional
    public void atualizarConta(UsuarioForm formulario, Usuario usuario,
                               Long idAdministradorLogado, BindingResult result) {
        validarLoginUnico(formulario.getUsuario(), usuario.getId(), result);
        validarSenhaOpcional(formulario.getSenha(), result);
        if (result.hasErrors()) {
            return;
        }

        Set<Perfil> novosPerfis = new HashSet<>();
        if (formulario.getPerfis() != null) {
            for (Long idPerfil : formulario.getPerfis()) {
                Perfil perfil = perfilRepository.porId(idPerfil);
                if (perfil != null) {
                    novosPerfis.add(perfil);
                }
            }
        }

        if (novosPerfis.isEmpty()) {
            result.rejectValue("perfis", "perfis.obrigatorios", "Selecione ao menos um papel.");
            return;
        }

        boolean eraAdmin = possuiPerfil(usuario, "ADMIN");
        boolean continuaAdmin = novosPerfis.stream()
                .anyMatch(p -> "ADMIN".equalsIgnoreCase(p.getNome()));

        if (eraAdmin && !continuaAdmin) {
            if (usuario.getId().equals(idAdministradorLogado)) {
                result.rejectValue("perfis", "perfis.proprioAdmin",
                        "Você não pode remover o seu próprio papel ADMIN.");
                return;
            }
            if (usuarioRepository.contarAdministradores() <= 1) {
                result.rejectValue("perfis", "perfis.ultimoAdmin",
                        "O sistema deve manter ao menos um usuário ADMIN.");
                return;
            }
        }

        usuario.setUsuario(formulario.getUsuario().trim());
        usuario.setPerfis(novosPerfis);
        if (formulario.getSenha() != null && !formulario.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(formulario.getSenha()));
        }
        usuarioRepository.update(usuario);
    }

    private boolean possuiPerfil(Usuario usuario, String nomePerfil) {
        return usuario.getPerfis().stream()
                .anyMatch(p -> nomePerfil.equalsIgnoreCase(p.getNome()));
    }

    private void validarSenhaOpcional(String senha, BindingResult result) {
        if (senha == null || senha.isBlank()) {
            return;
        }
        if (senha.length() < 6) {
            result.rejectValue("senha", "senha.curta", "A senha deve ter no mínimo 6 caracteres.");
        }
        if (senha.length() > 72) {
            result.rejectValue("senha", "senha.longa", "A senha deve ter no máximo 72 caracteres.");
        }
    }

    private void validarLoginUnico(String login, Long idUsuarioAtual, BindingResult result) {
        if (login == null || login.isBlank()) {
            result.rejectValue("login", "login.obrigatorio", "Informe o nome de usuário.");
            return;
        }

        String valor = login.trim();
        if (valor.length() < 3 || valor.length() > 50) {
            result.rejectValue("login", "login.tamanho",
                    "O nome de usuário deve ter entre 3 e 50 caracteres.");
            return;
        }
        if (!valor.matches("[A-Za-z0-9._-]+")) {
            result.rejectValue("login", "login.formato",
                    "O nome de usuário deve conter apenas letras, números, ponto, hífen ou sublinhado.");
            return;
        }

        boolean duplicado = idUsuarioAtual == null
                ? usuarioRepository.existePorUsuario(valor)
                : usuarioRepository.existeOutroUsuario(valor, idUsuarioAtual);

        if (duplicado) {
            result.rejectValue("login", "login.duplicado", "Este nome de usuário já está em uso.");
        }
    }

    private Perfil perfilInicial() {
        Perfil perfil = perfilRepository.porNome(PERFIL_INICIAL);
        if (perfil == null) {
            perfil = new Perfil();
            perfil.setNome(PERFIL_INICIAL);
            perfilRepository.save(perfil);
        }
        return perfil;
    }
}