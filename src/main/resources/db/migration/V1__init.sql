CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_name VARCHAR(100),
    vendor VARCHAR(100),
    product VARCHAR(100),
    status VARCHAR(50),
    order_date DATE,
    delivery_date DATE
);
