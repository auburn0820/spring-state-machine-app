-- 샘플 고객 데이터
INSERT INTO customers (name, email, phone_number, address, created_at) VALUES
    ('김철수', 'kim@example.com', '010-1234-5678', '서울시 강남구', CURRENT_TIMESTAMP),
    ('이영희', 'lee@example.com', '010-2345-6789', '부산시 해운대구', CURRENT_TIMESTAMP),
    ('박민수', 'park@example.com', '010-3456-7890', '대구시 중구', CURRENT_TIMESTAMP);

-- 샘플 주문 데이터
INSERT INTO orders (customer_id, total_amount, state, notes, created_at) VALUES
    (1, 150000.00, 'CREATED', '빠른 배송 요청', CURRENT_TIMESTAMP),
    (2, 89000.00, 'PAID', '포장지 요청 없음', CURRENT_TIMESTAMP),
    (3, 250000.00, 'IN_PREPARATION', '선물용 포장 요청', CURRENT_TIMESTAMP);

-- 샘플 주문 아이템 데이터
INSERT INTO order_items (order_id, product_name, quantity, unit_price, total_price) VALUES
    (1, '노트북', 1, 150000.00, 150000.00),
    (2, '키보드', 2, 35000.00, 70000.00),
    (2, '마우스', 1, 19000.00, 19000.00),
    (3, '모니터', 2, 125000.00, 250000.00);

-- 샘플 결제 데이터
INSERT INTO payments (order_id, amount, payment_method, payment_status, transaction_id, created_at, processed_at) VALUES
    (2, 89000.00, 'CREDIT_CARD', 'COMPLETED', 'TXN_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 250000.00, 'BANK_TRANSFER', 'PENDING', NULL, CURRENT_TIMESTAMP, NULL);