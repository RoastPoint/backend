package ru.roastpoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.security.Principal;
import static ru.roastpoint.OrderDto.*;
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
 private final OrderService service;
 public OrderController(OrderService service) { this.service=service; }
 @PostMapping
 public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateRequest request, Principal principal) {
  var order=service.create(request,principal.getName());
  return ResponseEntity.created(URI.create("/api/orders/"+order.id())).body(order);
 }
 @GetMapping("/{id}")
 public OrderResponse get(@PathVariable @Positive int id) { return service.get(id); }
 @GetMapping
 public List<OrderService.Summary> list(
  @RequestParam(required=false) @Pattern(regexp="accepted|preparing|ready|issued") String status,
  @RequestParam(defaultValue="50") @Min(1) @Max(100) int limit,
  @RequestParam(defaultValue="0") @Min(0) int offset) { return service.list(status,limit,offset); }
 @PatchMapping("/{id}/status")
 public OrderResponse changeStatus(@PathVariable @Positive int id,@Valid @RequestBody StatusRequest request,Authentication auth) {
  return service.changeStatus(id,request.status(),auth.getName());
 }
}
