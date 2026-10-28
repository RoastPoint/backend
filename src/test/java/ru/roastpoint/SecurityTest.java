package ru.roastpoint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({OrderController.class,AuthController.class})
@Import(SecurityConfig.class)
class SecurityTest {
 @Autowired MockMvc mvc;
 @Autowired PasswordEncoder encoder;
 @MockitoBean OrderService orders;
 @MockitoBean UserDetailsService users;
 @Test void anonymousCannotReadOrders() throws Exception {
  mvc.perform(get("/api/orders/1")).andExpect(status().isUnauthorized());
  verifyNoInteractions(orders);
 }
 @Test void baristaCannotCreateOrder() throws Exception {
  mvc.perform(post("/api/orders").with(user("barista1").roles("barista")).with(csrf())
   .contentType("application/json").content("{\"items\":[{\"menuItemId\":1,\"quantity\":1}]}"))
   .andExpect(status().isForbidden());
  verifyNoInteractions(orders);
 }
 @Test void cashierCannotChangeMenuOrReadReports() throws Exception {
  mvc.perform(put("/api/menu/1").with(user("cashier1").roles("cashier")).with(csrf()))
   .andExpect(status().isForbidden());
  mvc.perform(get("/api/reports/sales?from=2026-10-01&to=2026-10-28").with(user("cashier1").roles("cashier")))
   .andExpect(status().isForbidden());
 }
 @Test void csrfRequiredForChanges() throws Exception {
  mvc.perform(post("/api/orders").with(user("cashier1").roles("cashier")))
   .andExpect(status().isForbidden());
  verifyNoInteractions(orders);
 }
 @Test void loginCreatesSessionAndLogoutInvalidatesIt() throws Exception {
  when(users.loadUserByUsername("cashier1")).thenReturn(User.withUsername("cashier1")
   .password(encoder.encode("test-password-123")).roles("cashier").build());
  var result=mvc.perform(post("/api/auth/login").with(csrf()).param("login","cashier1").param("password","test-password-123"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.login").value("cashier1")).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("ROLE_cashier"));
  mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
  org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
 }
 @Test void badPasswordDoesNotAuthenticate() throws Exception {
  when(users.loadUserByUsername("cashier1")).thenReturn(User.withUsername("cashier1")
   .password(encoder.encode("test-password-123")).roles("cashier").build());
  mvc.perform(post("/api/auth/login").with(csrf()).param("login","cashier1").param("password","wrong"))
   .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
 }
}
