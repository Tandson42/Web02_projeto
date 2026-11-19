package br.edu.ifto.pwebII.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .requestMatchers("/login", "/cadastro", "/error").permitAll()
                // área do paciente: somente a própria agenda e histórico de consultas
                .requestMatchers("/minhas-consultas/**").hasRole("PACIENTE")
                // área do médico: somente as próprias consultas e pacientes
                .requestMatchers("/minhas-consultas-medico", "/meus-pacientes").hasRole("MEDICO")
                // agenda de disponibilidade e calendário
                .requestMatchers("/agenda/disponiveis").hasAnyRole("PACIENTE", "SECRETARIO", "ADMIN")
                .requestMatchers("/agenda/**").hasAnyRole("MEDICO", "SECRETARIO", "ADMIN")
                // agendamentos e confirmação de presença (confirmação restrita à clínica)
                .requestMatchers("/agendamento/confirmar/**").hasAnyRole("SECRETARIO", "ADMIN")
                .requestMatchers("/agendamento/novo/**", "/agendamento/save", "/agendamento/cancelar/**")
                    .hasAnyRole("PACIENTE", "SECRETARIO", "ADMIN")
                .requestMatchers("/agendamento/list").hasAnyRole("PACIENTE", "SECRETARIO", "ADMIN")
                // gestão de usuários e cadastro/edição/exclusão de médicos: somente ADMIN
                .requestMatchers("/usuario/**").hasRole("ADMIN")
                .requestMatchers("/medico/secretarios/**").hasRole("MEDICO")
                .requestMatchers("/medico/preco/**").hasAnyRole("MEDICO", "SECRETARIO", "ADMIN")
                .requestMatchers("/medico/list", "/medico/consultas/**").hasAnyRole("SECRETARIO", "ADMIN")
                .requestMatchers("/medico/**").hasRole("ADMIN")
                // observações clínicas da consulta: médico da consulta edita; secretário associado e admin apenas visualizam
                .requestMatchers(HttpMethod.GET, "/consulta/observacao/**").hasAnyRole("MEDICO", "SECRETARIO", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/consulta/observacao/**").hasRole("MEDICO")
                // recepção (SECRETARIO) e ADMIN: pacientes e consultas
                .requestMatchers("/paciente/**").hasAnyRole("SECRETARIO", "ADMIN")
                .requestMatchers("/consulta/**").hasAnyRole("SECRETARIO", "ADMIN")
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