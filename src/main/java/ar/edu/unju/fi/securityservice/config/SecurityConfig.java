package ar.edu.unju.fi.securityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Factor de costo de BCrypt (2^costo iteraciones). El valor por defecto de Spring es 10; con 12 cada
     * hash es unas 4 veces más caro de calcular, lo que encarece los ataques de fuerza bruta contra una
     * base filtrada. El costo queda dentro del propio hash, así que los hashes anteriores siguen validando.
     */
    static final int COSTO_BCRYPT_POR_DEFECTO = 12;

    @Bean
    public PasswordEncoder passwordEncoder(
            @Value("${security.password.bcrypt-strength:" + COSTO_BCRYPT_POR_DEFECTO + "}") int costo) {
        return new BCryptPasswordEncoder(costo);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            // La autenticación viaja en el JWT: el servidor no guarda sesión.
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // Solo health (lo usa el healthcheck de Docker) e info; el resto de actuator exige autenticación.
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
}
