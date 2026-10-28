SET search_path TO coffee_shop,public;
-- Seed exactly once only for a new database; no DELETE/TRUNCATE on restart.
-- Initialization must be run by one app instance at a time.
DO $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM users) AND NOT EXISTS(SELECT 1 FROM categories)
    AND NOT EXISTS(SELECT 1 FROM orders) AND NOT EXISTS(SELECT 1 FROM menu_items) THEN
  INSERT INTO users(full_name,login,password_hash,role) VALUES
   ('Анна Иванова','cashier1','$2a$12$bcW4Y2fzGMaZEu3ZK1EAE.LH8Nf8lCNrI3jn0PpFDV81349cmMiLK','cashier'),
   ('Иван Петров','barista1','$2a$12$bcW4Y2fzGMaZEu3ZK1EAE.LH8Nf8lCNrI3jn0PpFDV81349cmMiLK','barista'),
   ('Мария Соколова','admin1','$2a$12$bcW4Y2fzGMaZEu3ZK1EAE.LH8Nf8lCNrI3jn0PpFDV81349cmMiLK','admin');
  INSERT INTO categories(name) VALUES('Кофе'),('Чай'),('Выпечка');
  INSERT INTO menu_items(category_id,name,price,is_available) VALUES
   (1,'Эспрессо',120,true),(1,'Американо',150,true),(1,'Капучино',200,true),
   (1,'Латте',220,true),(1,'Раф',260,true),(2,'Чай чёрный',100,true),
   (2,'Чай зелёный',100,true),(3,'Круассан',160,true),(3,'Маффин',140,true),
   (3,'Булочка с корицей',180,false);
  INSERT INTO orders(cashier_id,status,created_at,issued_at)
   SELECT 1,CASE WHEN n<=7 THEN 'issued' WHEN n=8 THEN 'preparing' ELSE 'accepted' END,
    TIMESTAMP '2026-10-22 09:00:00'+n*INTERVAL '10 minutes',
    CASE WHEN n<=7 THEN TIMESTAMP '2026-10-22 09:00:00'+n*INTERVAL '10 minutes' END FROM generate_series(1,10) AS n;
  INSERT INTO order_items(order_id,menu_item_id,quantity,unit_price,item_name)
   SELECT o.id,m.id,1,m.price,m.name FROM orders o JOIN menu_items m ON m.id=o.id;
 END IF;
END $$;
