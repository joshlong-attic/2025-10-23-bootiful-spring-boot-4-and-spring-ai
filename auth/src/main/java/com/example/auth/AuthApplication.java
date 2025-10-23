package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.security.Principal;
import java.util.Map;

import static org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer.authorizationServer;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    SecurityFilterChain authorizationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .with(authorizationServer(), as -> as.oidc(Customizer.withDefaults()))
                .webAuthn(rp -> rp
                        .allowedOrigins("http://localhost:9090")
                        .rpName("bootiful")
                        .rpId("localhost")
                )
                .oneTimeTokenLogin(ott ->
                        ott.tokenGenerationSuccessHandler((_,
                                                           response,
                                                           oneTimeToken) -> {

                            response.getWriter().println("you've got console mail!");
                            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);

                            IO.println("please go to http://localhost:9090/login/ott?token=" +
                                    oneTimeToken.getTokenValue());
                        }))
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsPasswordService userDetailsPasswordService(
            JdbcUserDetailsManager jdbcUserDetailsManager) {
        return (user, newPassword) -> {
            var u = User.withUserDetails(user).password(newPassword).build();
            jdbcUserDetailsManager.updateUser(u);
            return u;
        };
    }

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

//    InMemoryUserDetailsManager inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {
//        var user1Pw = passwordEncoder.encode("pw");
//        var user2Pw = passwordEncoder.encode("pw");
//        IO.println(user1Pw);
//        IO.println(user2Pw);
//        var user1 = User.withUsername("josh").password(user1Pw).roles("USER").build();
//        var user2 = User.withUsername("james").password(user2Pw).roles("USER").build();
//        return new InMemoryUserDetailsManager(user1, user2);
//    }

}

// authentication


// authorization

@Controller
@ResponseBody
class MeController {

    @GetMapping("/")
    Map<String, String> me(Principal p) {
        return Map.of("name", p.getName());
    }
}