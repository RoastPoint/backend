package ru.roastpoint;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
  return Map.of("headerName",token.getHeaderName(),"token",token.getToken());
 }
 @GetMapping("/me") public Map<String,Object> me(Authentication authentication) {
  return Map.of("login",authentication.getName(),"roles",authentication.getAuthorities().stream().map(Object::toString).toList());
 }
}
