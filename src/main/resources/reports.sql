-- Run manually in PostgreSQL. These queries are not startup scripts.
SET search_path TO coffee_shop,public;

-- Daily revenue for an inclusive date range (issuance date).
SELECT day,orders_count,revenue FROM daily_sales
WHERE day BETWEEN DATE '2026-10-01' AND DATE '2026-10-31' ORDER BY day;

-- Popular items, all-time issued orders only.
SELECT s.menu_item_id,m.name,s.quantity,s.revenue
FROM product_sales s JOIN menu_items m ON m.id=s.menu_item_id
ORDER BY s.quantity DESC,s.menu_item_id;

-- Revenue and count by cashier in a half-open period.
SELECT u.id,u.full_name,count(*) AS orders_count,sum(o.total) AS revenue
FROM order_totals o JOIN users u ON u.id=o.cashier_id
WHERE o.status='issued' AND o.issued_at>=TIMESTAMP '2026-10-01 00:00:00'
 AND o.issued_at<TIMESTAMP '2026-11-01 00:00:00'
GROUP BY u.id,u.full_name ORDER BY revenue DESC,u.id;
