SET search_path TO coffee_shop,public;
-- Idempotent upgrade of the original scenario-1 database.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK(status IN ('accepted','preparing','ready','issued'));
ALTER TABLE orders ADD COLUMN IF NOT EXISTS issued_at timestamp;
-- Historical demo data has no issuance time; use its creation time once.
UPDATE orders SET issued_at=created_at WHERE status='issued' AND issued_at IS NULL;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS item_name varchar(150);
UPDATE order_items i SET item_name=m.name FROM menu_items m WHERE i.menu_item_id=m.id AND i.item_name IS NULL;
ALTER TABLE order_items ALTER COLUMN item_name SET NOT NULL;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='categories'::regclass AND conname='category_name_nonblank') THEN
  ALTER TABLE categories ADD CONSTRAINT category_name_nonblank CHECK(length(btrim(name))>0);
  ALTER TABLE menu_items ADD CONSTRAINT menu_name_nonblank CHECK(length(btrim(name))>0);
  ALTER TABLE users ADD CONSTRAINT user_login_nonblank CHECK(length(btrim(login))>0);
  ALTER TABLE users ADD CONSTRAINT user_name_nonblank CHECK(length(btrim(full_name))>0);
  ALTER TABLE order_items ADD CONSTRAINT item_quantity_limit CHECK(quantity<=1000);
  ALTER TABLE order_items ADD CONSTRAINT item_name_nonblank CHECK(length(btrim(item_name))>0);
  ALTER TABLE orders ADD CONSTRAINT issued_time_consistent CHECK((status='issued')=(issued_at IS NOT NULL));
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='users'::regclass AND conname='users_id_role_unique') THEN
  ALTER TABLE users ADD CONSTRAINT users_id_role_unique UNIQUE(id,role);
  ALTER TABLE orders ADD COLUMN cashier_role varchar(20) NOT NULL DEFAULT 'cashier' CHECK(cashier_role='cashier');
  ALTER TABLE orders ADD CONSTRAINT orders_cashier_role_fk FOREIGN KEY(cashier_id,cashier_role) REFERENCES users(id,role);
 END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_orders_queue ON orders(status,created_at,id);
CREATE INDEX IF NOT EXISTS idx_menu_category ON menu_items(category_id);
CREATE INDEX IF NOT EXISTS idx_orders_issued_at ON orders(issued_at) WHERE status='issued';

CREATE OR REPLACE FUNCTION validate_order_transition() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.status<>OLD.status AND NOT (
  (OLD.status='accepted' AND NEW.status='preparing') OR
  (OLD.status='preparing' AND NEW.status='ready') OR
  (OLD.status='ready' AND NEW.status='issued')) THEN
  RAISE EXCEPTION 'Invalid order status transition' USING ERRCODE='23514';
 END IF;
 IF NEW.status='issued' AND OLD.status<>'issued' THEN NEW.issued_at=CURRENT_TIMESTAMP; END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE TRIGGER order_status_transition BEFORE UPDATE ON orders
 FOR EACH ROW EXECUTE FUNCTION validate_order_transition();

CREATE OR REPLACE VIEW order_totals AS
 SELECT o.id,o.created_at,o.issued_at,o.cashier_id,o.status,
 coalesce(sum(i.quantity*i.unit_price),0)::numeric(18,2) AS total
 FROM orders o LEFT JOIN order_items i ON i.order_id=o.id GROUP BY o.id;
CREATE OR REPLACE VIEW daily_sales AS
 SELECT issued_at::date AS day,count(*) AS orders_count,sum(total)::numeric(18,2) AS revenue
 FROM order_totals WHERE status='issued' GROUP BY issued_at::date;
CREATE OR REPLACE VIEW product_sales AS
 SELECT i.menu_item_id,sum(i.quantity) AS quantity,sum(i.quantity*i.unit_price)::numeric(18,2) AS revenue
 FROM order_items i JOIN orders o ON o.id=i.order_id WHERE o.status='issued' GROUP BY i.menu_item_id;
