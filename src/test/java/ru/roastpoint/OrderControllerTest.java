package ru.roastpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
@WebMvcTest(OrderController.class)
@org.springframework.context.annotation.Import(SecurityConfig.class)
@org.springframework.security.test.context.support.WithMockUser(username="cashier1",roles="cashier")
class OrderControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean OrderService service;
 OrderDto.OrderResponse order() {
  return new OrderDto.OrderResponse(11,LocalDateTime.of(2026,10,22,10,0),1,"accepted",
   List.of(new OrderDto.ItemResponse(3,"Капучино",2,new BigDecimal("200.00"),new BigDecimal("400.00"))),new BigDecimal("400.00"));
 }
 @Test void createsAndReturnsLocation() throws Exception {
  when(service.create(any(),eq("cashier1"))).thenReturn(order());
  mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json")
   .content("{\"items\":[{\"menuItemId\":3,\"quantity\":2}]}"))
   .andExpect(status().isCreated()).andExpect(header().string("Location","/api/orders/11"))
   .andExpect(jsonPath("$.total").value(400));
 }
 @Test void readsOrder() throws Exception {
  when(service.get(11)).thenReturn(order());
  mvc.perform(get("/api/orders/11")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(11));
 }
 @Test void rejectsEmptyOrder() throws Exception {
  mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content("{\"items\":[]}"))
   .andExpect(status().isBadRequest()); verifyNoInteractions(service);
 }
 @Test void rejectsZeroQuantity() throws Exception {
  mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json")
   .content("{\"items\":[{\"menuItemId\":3,\"quantity\":0}]}"))
   .andExpect(status().isBadRequest()); verifyNoInteractions(service);
 }
 @Test void missingOrder() throws Exception {
  when(service.get(999)).thenThrow(new ApiException(NOT_FOUND,"Заказ не найден: 999"));
  mvc.perform(get("/api/orders/999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
 }
 @Test void invalidJson() throws Exception {
  mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content("{"))
   .andExpect(status().isBadRequest()); verifyNoInteractions(service);
 }
 @Test void cannotForgeCashier() throws Exception {
  mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
   .contentType("application/json").content("{\"cashierId\":2,\"items\":[{\"menuItemId\":3,\"quantity\":1}]}"))
   .andExpect(status().isBadRequest()); verifyNoInteractions(service);
 }
 @Test void rejectsInvalidQueueLimit() throws Exception {
  mvc.perform(get("/api/orders?limit=100000")).andExpect(status().isBadRequest());
  verifyNoInteractions(service);
 }
}
