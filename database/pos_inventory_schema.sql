use posinventorysystem;
-- ============================================================
-- POS & Inventory Management System - Database Schema
-- Target: MySQL 5.7+ / MariaDB 10.2+ (InnoDB, utf8mb4)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- Independent / lookup tables first
-- ------------------------------------------------------------

CREATE TABLE role (
    role_id     INT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE setting (
    setting_id  INT AUTO_INCREMENT PRIMARY KEY,
    store_name  VARCHAR(150) NOT NULL,
    currency    VARCHAR(10)  NOT NULL,
    tax_rate    DECIMAL(5,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE unit (
    unit_id     INT AUTO_INCREMENT PRIMARY KEY,
    unit_name   VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
    category_id     INT AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier (
    supplier_id     INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name   VARCHAR(150) NOT NULL,
    phone           VARCHAR(30)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer (
    customer_id     INT AUTO_INCREMENT PRIMARY KEY,
    customer_name   VARCHAR(150) NOT NULL,
    phone           VARCHAR(30)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- user (depends on role)
-- ------------------------------------------------------------

CREATE TABLE user (
    user_id         INT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role_id         INT NOT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id) REFERENCES role(role_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- product (depends on category, unit)
-- ------------------------------------------------------------

CREATE TABLE product (
    product_id          INT AUTO_INCREMENT PRIMARY KEY,
    barcode             VARCHAR(100),
    product_name        VARCHAR(150) NOT NULL,
    category_id         INT NOT NULL,
    unit_id             INT NOT NULL,
    cost_price          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    selling_price       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantity_in_stock   INT NOT NULL DEFAULT 0,
    reorder_level       INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES category(category_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_product_unit
        FOREIGN KEY (unit_id) REFERENCES unit(unit_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- sale (depends on user, customer)
-- ------------------------------------------------------------

CREATE TABLE sale (
    sale_id     INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no  VARCHAR(50) NOT NULL UNIQUE,
    sale_date   DATETIME NOT NULL,
    user_id     INT NOT NULL,
    customer_id INT NULL,
    subtotal    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status      VARCHAR(30) NOT NULL DEFAULT 'completed',
    CONSTRAINT fk_sale_user
        FOREIGN KEY (user_id) REFERENCES user(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_sale_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- sale_item (depends on sale, product)
-- ------------------------------------------------------------

CREATE TABLE sale_item (
    sale_item_id    INT AUTO_INCREMENT PRIMARY KEY,
    sale_id         INT NOT NULL,
    product_id      INT NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL,
    line_total      DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_saleitem_sale
        FOREIGN KEY (sale_id) REFERENCES sale(sale_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_saleitem_product
        FOREIGN KEY (product_id) REFERENCES product(product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- payment (depends on sale)
-- ------------------------------------------------------------

CREATE TABLE payment (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    sale_id         INT NOT NULL,
    method          VARCHAR(30) NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    payment_date    DATETIME NOT NULL,
    CONSTRAINT fk_payment_sale
        FOREIGN KEY (sale_id) REFERENCES sale(sale_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- purchase (depends on supplier, user)
-- ------------------------------------------------------------

CREATE TABLE purchase (
    purchase_id     INT AUTO_INCREMENT PRIMARY KEY,
    reference_no    VARCHAR(50) NOT NULL UNIQUE,
    purchase_date   DATETIME NOT NULL,
    supplier_id     INT NOT NULL,
    user_id         INT NOT NULL,
    total           DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_purchase_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_user
        FOREIGN KEY (user_id) REFERENCES user(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- purchase_item (depends on purchase, product)
-- ------------------------------------------------------------

CREATE TABLE purchase_item (
    purchase_item_id    INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id         INT NOT NULL,
    product_id          INT NOT NULL,
    quantity            DECIMAL(10,2) NOT NULL,
    unit_cost           DECIMAL(10,2) NOT NULL,
    line_total          DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_purchaseitem_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchase(purchase_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_purchaseitem_product
        FOREIGN KEY (product_id) REFERENCES product(product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- movement (depends on product, user) - stock ledger
-- ------------------------------------------------------------

CREATE TABLE movement (
    movement_id         INT AUTO_INCREMENT PRIMARY KEY,
    product_id          INT NOT NULL,
    movement_type       VARCHAR(30) NOT NULL,   -- e.g. 'sale', 'purchase', 'adjustment'
    quantity_change      INT NOT NULL,
    reference_id        INT,                    -- points to sale_id / purchase_id depending on movement_type
    user_id             INT NOT NULL,
    movement_date       DATETIME NOT NULL,
    CONSTRAINT fk_movement_product
        FOREIGN KEY (product_id) REFERENCES product(product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_movement_user
        FOREIGN KEY (user_id) REFERENCES user(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Helpful indexes (FKs are auto-indexed by InnoDB, these add more)
-- ------------------------------------------------------------

CREATE INDEX idx_product_barcode ON product(barcode);
CREATE INDEX idx_sale_date ON sale(sale_date);
CREATE INDEX idx_purchase_date ON purchase(purchase_date);
CREATE INDEX idx_movement_date ON movement(movement_date);
CREATE INDEX idx_movement_reference ON movement(reference_id);

SET FOREIGN_KEY_CHECKS = 1;
