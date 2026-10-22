package ru.roastpoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import static ru.roastpoint.OrderDto.*;
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
 private final OrderService service;
 public OrderController(OrderService service) { this.service=service; }
 @PostMapping
 public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateRequest request) {
  var order=service.create(request);
  return ResponseEntity.created(URI.create("/api/orders/"+order.id())).body(order);
 }
 @GetMapping("/{id}")
 public OrderResponse get(@PathVariable @Positive int id) { return service.get(id); }
}
