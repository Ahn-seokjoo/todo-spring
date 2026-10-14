CREATE INDEX idx_seller_status_created_price
    ON todo_purchase (seller_id, purchase_status, created_at, price);
