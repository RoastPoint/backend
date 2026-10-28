package ru.roastpoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/menu")
@Validated
public class MenuController {
 private final JdbcClient db;
 public MenuController(JdbcClient db) { this.db=db; }
 public record Category(int id,String name) {}
 public record CategoryRequest(@NotBlank @Size(max=100) String name) {}
 public record MenuRequest(@NotNull @Positive Integer categoryId,@NotBlank @Size(max=150) String name,
  @NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal price,@NotNull Boolean available) {}
 public record MenuItem(int id,int categoryId,String name,BigDecimal price,boolean available) {}
 @GetMapping("/categories") public List<Category> categories() {
  return db.sql("SELECT id,name FROM categories ORDER BY id").query(Category.class).list();
 }
 @PostMapping("/categories") public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
  Category category=db.sql("INSERT INTO categories(name) VALUES(:name) RETURNING id,name")
   .param("name",request.name().strip()).query(Category.class).single();
  return ResponseEntity.created(URI.create("/api/menu/categories")).body(category);
 }
 @GetMapping public List<MenuItem> menu() {
  return db.sql("SELECT id,category_id,name,price,is_available AS available FROM menu_items ORDER BY category_id,id")
   .query(MenuItem.class).list();
 }
 @GetMapping("/{id}") public MenuItem get(@PathVariable @Positive int id) {
  return db.sql("SELECT id,category_id,name,price,is_available AS available FROM menu_items WHERE id=:id")
   .param("id",id).query(MenuItem.class).optional().orElseThrow(()->new ApiException(NOT_FOUND,"Позиция меню не найдена"));
 }
 @PostMapping @Transactional public ResponseEntity<MenuItem> create(@Valid @RequestBody MenuRequest request) {
  checkCategory(request.categoryId());
  int id=db.sql("INSERT INTO menu_items(category_id,name,price,is_available) VALUES(:category,:name,:price,:available) RETURNING id")
   .param("category",request.categoryId()).param("name",request.name().strip()).param("price",request.price())
   .param("available",request.available()).query(Integer.class).single();
  return ResponseEntity.created(URI.create("/api/menu/"+id)).body(get(id));
 }
 @PutMapping("/{id}") @Transactional public MenuItem update(@PathVariable @Positive int id,@Valid @RequestBody MenuRequest request) {
  checkCategory(request.categoryId());
  int count=db.sql("UPDATE menu_items SET category_id=:category,name=:name,price=:price,is_available=:available WHERE id=:id")
   .param("category",request.categoryId()).param("name",request.name().strip()).param("price",request.price())
   .param("available",request.available()).param("id",id).update();
  if(count==0) throw new ApiException(NOT_FOUND,"Позиция меню не найдена");
  return get(id);
 }
 private void checkCategory(int id) {
  if(db.sql("SELECT id FROM categories WHERE id=:id FOR SHARE").param("id",id).query(Integer.class).optional().isEmpty())
   throw new ApiException(NOT_FOUND,"Категория не найдена");
 }
}
