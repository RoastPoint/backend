package ru.roastpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import java.util.List;

@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

 static void error(HttpServletResponse response, ObjectMapper json, int status, String message) throws java.io.IOException {
  response.setStatus(status);
  response.setContentType("application/json;charset=UTF-8");
  json.writeValue(response.getWriter(),Map.of("timestamp",Instant.now().toString(),"status",status,"message",message,"details",List.of()));
 }

 @Bean SecurityFilterChain security(HttpSecurity http, ObjectMapper json) throws Exception {
  return http
   .authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/orders").hasRole("cashier")
    .requestMatchers(HttpMethod.PATCH,"/api/orders/*/status").hasAnyRole("cashier","barista")
    .requestMatchers(HttpMethod.GET,"/api/orders/**").hasAnyRole("cashier","barista","admin")
    .requestMatchers(HttpMethod.GET,"/api/menu/**", "/api/menu").authenticated()
    .requestMatchers("/api/menu/**", "/api/menu", "/api/reports/**").hasRole("admin")
    .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
    .anyRequest().denyAll())
   .requestCache(cache -> cache.disable())
   .formLogin(form -> form.loginProcessingUrl("/api/auth/login").usernameParameter("login")
    .successHandler((request,response,authentication) -> {
     response.setContentType("application/json;charset=UTF-8");
     json.writeValue(response.getWriter(),Map.of("login",authentication.getName(),"roles",authentication.getAuthorities().stream().map(Object::toString).toList()));
    })
    .failureHandler((request,response,exception) -> error(response,json,401,"Неверный логин или пароль")))
   .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
    .logoutSuccessHandler((request,response,authentication) -> response.setStatus(204)))
   .exceptionHandling(errors -> errors
    .authenticationEntryPoint((request,response,exception) -> error(response,json,401,"Требуется вход"))
    .accessDeniedHandler((request,response,exception) -> error(response,json,403,"Недостаточно прав или неверный CSRF-токен")))
   .build();
 }
}
