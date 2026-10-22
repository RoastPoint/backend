package ru.roastpoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDto {
 private OrderDto() {}
 public record ItemRequest(@NotNull @Positive Integer menuItemId,
                           @NotNull @Min(1) @Max(1000) Integer quantity) {}
 public record CreateRequest(@NotNull @Positive Integer cashierId,
                             @NotEmpty @Size(max=100) List<@NotNull @Valid ItemRequest> items) {}
 public record ItemResponse(int menuItemId,String name,int quantity,BigDecimal unitPrice,BigDecimal lineTotal) {}
 public record OrderResponse(int id,LocalDateTime createdAt,int cashierId,String status,
                             List<ItemResponse> items,BigDecimal total) {}
}
