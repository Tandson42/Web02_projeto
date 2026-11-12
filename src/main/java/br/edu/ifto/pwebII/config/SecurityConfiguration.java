package br.edu.ifto.pwebII.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // classe de configuração
@EnableWebSecurity // indica que serão definidas configurações personalizadas de segurança
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/login", "/error").permitAll()
                // área do paciente: somente as próprias consultas
                .requestMatchers("/minhas-consultas").hasRole("PACIENTE")
                // área do médico: somente os próprios pacientes e consultas
                .requestMatchers("/meus-pacientes", "/minhas-consultas-medico").hasRole("MEDICO")
                .requestMatchers("/paciente/consultas/**").hasAnyRole("SECRETARIO", "ADMIN", "MEDICO")
                // secretário e admin: acesso completo às funcionalidades administrativas e de cadastro
                .requestMatchers("/medico/**").hasAnyRole("SECRETARIO", "ADMIN")
                .requestMatchers("/paciente/**").hasAnyRole("SECRETARIO", "ADMIN")
                .requestMatchers("/consulta/**").hasAnyRole("SECRETARIO", "ADMIN")
                .requestMatchers("/usuario/**").hasAnyRole("SECRETARIO", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login") // URL da página de login personalizada
                .defaultSuccessUrl("/", true)
                .permitAll() // a página de login é acessível sem autenticação
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .rememberMe(withDefaults()); // mantém o usuário autenticado após fechar o navegador
        return http.build();
    }

    /**
     * Instancia o codificador BCrypt e deixa seu controle sob responsabilidade do Spring.
     * Sempre que o Spring Security precisar codificar ou comparar uma senha, utilizará este bean.
     *
     * Não há mais usuários em memória: o bean UsuarioDetailsService (que consulta o banco)
     * é o único UserDetailsService e é combinado automaticamente com este PasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}