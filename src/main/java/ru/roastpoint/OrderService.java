package ru.roastpoint;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static ru.roastpoint.OrderDto.*;
import static org.springframework.http.HttpStatus.*;

@Service
public class OrderService {
 private final JdbcClient db;
 public OrderService(JdbcClient db) { this.db=db; }
 record Header(int id,LocalDateTime createdAt,int cashierId,String status) {}
 record Menu(int id,String name,BigDecimal price,boolean available) {}

 @Transactional
 public OrderResponse create(CreateRequest request) {
  // Role is checked in the same transaction; this is validation, not authentication.
  String role=db.sql("SELECT role FROM users WHERE id=:id FOR SHARE")
   .param("id",request.cashierId()).query(String.class).optional()
   .orElseThrow(()->new ApiException(NOT_FOUND,"Кассир не найден"));
  if(!role.equals("cashier")) throw new ApiException(BAD_REQUEST,"Пользователь должен иметь роль cashier");
  var selected=new TreeMap<Integer,Integer>();
  for(var item:request.items()) {
   if(selected.putIfAbsent(item.menuItemId(),item.quantity())!=null)
    throw new ApiException(BAD_REQUEST,"Позиции не должны повторяться: измените количество");
  }
  // Stable lock order avoids deadlocks between overlapping baskets.
  var prices=new HashMap<Integer,BigDecimal>();
  for(int id:selected.keySet()) {
   Menu menu=db.sql("SELECT id,name,price,is_available AS available FROM menu_items WHERE id=:id FOR SHARE")
    .param("id",id).query(Menu.class).optional()
    .orElseThrow(()->new ApiException(NOT_FOUND,"Позиция меню не найдена: "+id));
   if(!menu.available()) throw new ApiException(CONFLICT,"Позиция недоступна: "+menu.name());
   prices.put(id,menu.price());
  }
  int orderId=db.sql("INSERT INTO orders(cashier_id) VALUES(:cashier) RETURNING id")
   .param("cashier",request.cashierId()).query(Integer.class).single();
  selected.forEach((id,qty)->db.sql("INSERT INTO order_items(order_id,menu_item_id,quantity,unit_price) VALUES(:o,:m,:q,:p)")
   .param("o",orderId).param("m",id).param("q",qty).param("p",prices.get(id)).update());
  return get(orderId);
 }
 @Transactional(readOnly=true)
 public OrderResponse get(int id) {
  Header h=db.sql("SELECT id,created_at,cashier_id,status FROM orders WHERE id=:id")
   .param("id",id).query(Header.class).optional()
   .orElseThrow(()->new ApiException(NOT_FOUND,"Заказ не найден: "+id));
  List<ItemResponse> items=db.sql("""
   SELECT i.menu_item_id,m.name,i.quantity,i.unit_price,i.quantity*i.unit_price AS line_total
   FROM order_items i JOIN menu_items m ON m.id=i.menu_item_id
   WHERE i.order_id=:id ORDER BY i.id
   """).param("id",id).query(ItemResponse.class).list();
  BigDecimal total=items.stream().map(ItemResponse::lineTotal).reduce(new BigDecimal("0.00"),BigDecimal::add);
  return new OrderResponse(h.id(),h.createdAt(),h.cashierId(),h.status(),items,total);
 }
}
