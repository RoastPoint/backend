package ru.roastpoint;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/reports")
@Validated
public class ReportController {
 private final JdbcClient db;
 public ReportController(JdbcClient db) { this.db=db; }
 public record DailySales(LocalDate day,long ordersCount,BigDecimal revenue) {}
 @GetMapping("/sales") public List<DailySales> sales(
  @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
  @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
  if(to.isBefore(from) || java.time.temporal.ChronoUnit.DAYS.between(from,to)>366)
   throw new ApiException(BAD_REQUEST,"Период должен быть от 0 до 366 дней, from ≤ to");
  return db.sql("SELECT day,orders_count,revenue FROM daily_sales WHERE day BETWEEN :from AND :to ORDER BY day")
   .param("from",from).param("to",to).query(DailySales.class).list();
 }
}
