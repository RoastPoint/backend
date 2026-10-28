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
 public record Summary(int id,LocalDateTime createdAt,int cashierId,String status,BigDecimal total) {}

 @Transactional(readOnly=true)
 public List<Summary> list(String status,int limit,int offset) {
  return db.sql("""
   SELECT o.id,o.created_at,o.cashier_id,o.status,coalesce(sum(i.quantity*i.unit_price),0) AS total
   FROM orders o LEFT JOIN order_items i ON i.order_id=o.id
   WHERE (:status IS NULL OR o.status=:status)
   GROUP BY o.id ORDER BY o.created_at,o.id LIMIT :limit OFFSET :offset
   """).param("status",status,java.sql.Types.VARCHAR).param("limit",limit).param("offset",offset).query(Summary.class).list();
 }

 @Transactional
 public OrderResponse changeStatus(int id,String target,String login) {
  String role=db.sql("SELECT role FROM users WHERE login=:login FOR SHARE")
   .param("login",login).query(String.class).optional().orElseThrow(()->new ApiException(FORBIDDEN,"Нет доступа"));
  String required=target.equals("issued")?"cashier":"barista";
  if(!role.equals(required)) throw new ApiException(FORBIDDEN,"Для этого действия нужна роль "+required);
  String current=db.sql("SELECT status FROM orders WHERE id=:id FOR UPDATE")
   .param("id",id).query(String.class).optional().orElseThrow(()->new ApiException(NOT_FOUND,"Заказ не найден: "+id));
  String next=switch(current) { case "accepted" -> "preparing"; case "preparing" -> "ready"; case "ready" -> "issued"; default -> ""; };
  if(!next.equals(target)) throw new ApiException(CONFLICT,"Недопустимый переход: "+current+" → "+target);
  db.sql("UPDATE orders SET status=:status,issued_at=CASE WHEN :status='issued' THEN CURRENT_TIMESTAMP ELSE issued_at END WHERE id=:id")
   .param("status",target).param("id",id).update();
  return get(id);
 }

 @Transactional
 public OrderResponse create(CreateRequest request, String login) {
  int cashierId=db.sql("SELECT id FROM users WHERE login=:login AND role='cashier' FOR SHARE")
   .param("login",login).query(Integer.class).optional()
   .orElseThrow(()->new ApiException(FORBIDDEN,"Создавать заказ может только кассир"));
  var selected=new TreeMap<Integer,Integer>();
  for(var item:request.items()) {
   if(selected.putIfAbsent(item.menuItemId(),item.quantity())!=null)
    throw new ApiException(BAD_REQUEST,"Позиции не должны повторяться: измените количество");
  }
  // Stable lock order avoids deadlocks between overlapping baskets.
  var prices=new HashMap<Integer,BigDecimal>();
  var names=new HashMap<Integer,String>();
  for(int id:selected.keySet()) {
   Menu menu=db.sql("SELECT id,name,price,is_available AS available FROM menu_items WHERE id=:id FOR SHARE")
    .param("id",id).query(Menu.class).optional()
    .orElseThrow(()->new ApiException(NOT_FOUND,"Позиция меню не найдена: "+id));
   if(!menu.available()) throw new ApiException(CONFLICT,"Позиция недоступна: "+menu.name());
   prices.put(id,menu.price());
   names.put(id,menu.name());
  }
  int orderId=db.sql("INSERT INTO orders(cashier_id) VALUES(:cashier) RETURNING id")
   .param("cashier",cashierId).query(Integer.class).single();
  selected.forEach((id,qty)->db.sql("INSERT INTO order_items(order_id,menu_item_id,quantity,unit_price,item_name) VALUES(:o,:m,:q,:p,:name)")
   .param("o",orderId).param("m",id).param("q",qty).param("p",prices.get(id)).param("name",names.get(id)).update());
  return get(orderId);
 }
 @Transactional(readOnly=true)
 public OrderResponse get(int id) {
  Header h=db.sql("SELECT id,created_at,cashier_id,status FROM orders WHERE id=:id")
   .param("id",id).query(Header.class).optional()
   .orElseThrow(()->new ApiException(NOT_FOUND,"Заказ не найден: "+id));
  List<ItemResponse> items=db.sql("""
   SELECT i.menu_item_id,i.item_name AS name,i.quantity,i.unit_price,i.quantity*i.unit_price AS line_total
   FROM order_items i
   WHERE i.order_id=:id ORDER BY i.id
   """).param("id",id).query(ItemResponse.class).list();
  BigDecimal total=items.stream().map(ItemResponse::lineTotal).reduce(new BigDecimal("0.00"),BigDecimal::add);
  return new OrderResponse(h.id(),h.createdAt(),h.cashierId(),h.status(),items,total);
 }
}
