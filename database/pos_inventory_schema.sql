-- ============================================================
-- POS & Inventory Management System - Database Schema (v2)
-- Target: MySQL 5.7+ / MariaDB 10.2+ (InnoDB, utf8mb4)
--
-- Changes from v1:
--   - Soft deletes: deleted_at added to every deletable table
--   - product.is_active added (separate from soft-delete — see note below)
--   - purchase.status added (completed / cancelled / refunded)
--   - UNIQUE constraints: product.barcode, role.role_name, category.category_name
--   - CHECK constraints: non-negative stock/prices, positive quantities,
--     fixed value sets for status/method/movement_type
--   - purchase_item.quantity changed from DECIMAL to INT (matches sale_item
--     and product.quantity_in_stock — one quantity type across the schema)
--   - New columns: sale_item.discount, movement.reason, customer.address,
--     supplier.address, setting store details (address, phone, email)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- Independent / lookup tables first
-- ------------------------------------------------------------

CREATE TABLE role (
                      role_id     INT AUTO_INCREMENT PRIMARY KEY,
                      role_name   VARCHAR(50) NOT NULL,
                      deleted_at  DATETIME(6) NULL,
                      UNIQUE KEY uq_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- setting: single-row (or few-row) store configuration.
-- No deleted_at — settings aren't a "deletable" entity in the normal sense;
-- there's always exactly one active config row.
CREATE TABLE setting (
                         setting_id      INT AUTO_INCREMENT PRIMARY KEY,
                         store_name      VARCHAR(150) NOT NULL,
                         currency        VARCHAR(10)  NOT NULL,
                         tax_rate        DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                         store_address   VARCHAR(255),
                         store_phone     VARCHAR(30),
                         store_email     VARCHAR(150),
                         CONSTRAINT chk_setting_tax_rate CHECK (tax_rate >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE unit (
                      unit_id     INT AUTO_INCREMENT PRIMARY KEY,
                      unit_name   VARCHAR(50) NOT NULL,
                      deleted_at  DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
                          category_id     INT AUTO_INCREMENT PRIMARY KEY,
                          category_name   VARCHAR(100) NOT NULL,
                          deleted_at      DATETIME(6) NULL,
                          UNIQUE KEY uq_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier (
                          supplier_id     INT AUTO_INCREMENT PRIMARY KEY,
                          supplier_name   VARCHAR(150) NOT NULL,
                          phone           VARCHAR(30),
                          address         VARCHAR(255),
                          deleted_at      DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer (
                          customer_id     INT AUTO_INCREMENT PRIMARY KEY,
                          customer_name   VARCHAR(150) NOT NULL,
                          phone           VARCHAR(30),
                          address         VARCHAR(255),
                          deleted_at      DATETIME(6) NULL
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
                      deleted_at      DATETIME(6) NULL,
                      CONSTRAINT fk_user_role
                          FOREIGN KEY (role_id) REFERENCES role(role_id)
                              ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- product (depends on category, unit)
-- ------------------------------------------------------------

-- NOTE on is_active vs deleted_at:
--   deleted_at = this product record was soft-deleted (shouldn't appear
--                anywhere, including historical reports, except admin "trash")
--   is_active  = this product is currently sellable/stocked (e.g.
--                discontinued, seasonal, out of production) but its history
--                still matters for reports/analytics and it's NOT deleted.
-- A product can be is_active = 0 and deleted_at IS NULL (discontinued, kept
-- for history) — that's the common case review item 4c was asking for.
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
                         is_active           TINYINT(1) NOT NULL DEFAULT 1,
                         deleted_at          DATETIME(6) NULL,
                         CONSTRAINT fk_product_category
                             FOREIGN KEY (category_id) REFERENCES category(category_id)
                                 ON UPDATE CASCADE ON DELETE RESTRICT,
                         CONSTRAINT fk_product_unit
                             FOREIGN KEY (unit_id) REFERENCES unit(unit_id)
                                 ON UPDATE CASCADE ON DELETE RESTRICT,
                         UNIQUE KEY uq_product_barcode (barcode),
                         CONSTRAINT chk_product_stock_nonneg CHECK (quantity_in_stock >= 0),
                         CONSTRAINT chk_product_reorder_nonneg CHECK (reorder_level >= 0),
                         CONSTRAINT chk_product_cost_nonneg CHECK (cost_price >= 0),
                         CONSTRAINT chk_product_selling_nonneg CHECK (selling_price >= 0)
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
                      deleted_at  DATETIME(6) NULL,
                      CONSTRAINT fk_sale_user
                          FOREIGN KEY (user_id) REFERENCES user(user_id)
                              ON UPDATE CASCADE ON DELETE RESTRICT,
                      CONSTRAINT fk_sale_customer
                          FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
                              ON UPDATE CASCADE ON DELETE SET NULL,
                      CONSTRAINT chk_sale_subtotal_nonneg CHECK (subtotal >= 0),
                      CONSTRAINT chk_sale_discount_nonneg CHECK (discount >= 0),
                      CONSTRAINT chk_sale_tax_nonneg CHECK (tax >= 0),
                      CONSTRAINT chk_sale_total_nonneg CHECK (total >= 0),
                      CONSTRAINT chk_sale_status CHECK (status IN ('completed', 'cancelled', 'refunded'))
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
                           unit_cost       DECIMAL(10,2) NOT NULL COMMENT 'Product cost price captured when this sale item was created',
                           discount        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                           line_total      DECIMAL(10,2) NOT NULL,
                           deleted_at      DATETIME(6) NULL,
                           CONSTRAINT fk_saleitem_sale
                               FOREIGN KEY (sale_id) REFERENCES sale(sale_id)
                                   ON UPDATE CASCADE ON DELETE RESTRICT,
                           CONSTRAINT fk_saleitem_product
                               FOREIGN KEY (product_id) REFERENCES product(product_id)
                                   ON UPDATE CASCADE ON DELETE RESTRICT,
                           CONSTRAINT chk_saleitem_qty_positive CHECK (quantity > 0),
                           CONSTRAINT chk_saleitem_unit_price_nonneg CHECK (unit_price >= 0),
                           CONSTRAINT chk_saleitem_unit_cost_nonneg CHECK (unit_cost >= 0),
                           CONSTRAINT chk_saleitem_discount_nonneg CHECK (discount >= 0),
                           CONSTRAINT chk_saleitem_line_total_nonneg CHECK (line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- payment (depends on sale) — insert-only ledger, no deleted_at
-- ------------------------------------------------------------

CREATE TABLE payment (
                         payment_id      INT AUTO_INCREMENT PRIMARY KEY,
                         sale_id         INT NOT NULL,
                         method          VARCHAR(30) NOT NULL,
                         amount          DECIMAL(10,2) NOT NULL,
                         payment_date    DATETIME NOT NULL,
                         CONSTRAINT fk_payment_sale
                             FOREIGN KEY (sale_id) REFERENCES sale(sale_id)
                                 ON UPDATE CASCADE ON DELETE RESTRICT,
                         CONSTRAINT chk_payment_amount_nonneg CHECK (amount >= 0),
                         CONSTRAINT chk_payment_method CHECK (method IN ('cash', 'card', 'mobile', 'other'))
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
                          status          VARCHAR(30) NOT NULL DEFAULT 'completed',
                          deleted_at      DATETIME(6) NULL,
                          CONSTRAINT fk_purchase_supplier
                              FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id)
                                  ON UPDATE CASCADE ON DELETE RESTRICT,
                          CONSTRAINT fk_purchase_user
                              FOREIGN KEY (user_id) REFERENCES user(user_id)
                                  ON UPDATE CASCADE ON DELETE RESTRICT,
                          CONSTRAINT chk_purchase_total_nonneg CHECK (total >= 0),
                          CONSTRAINT chk_purchase_status CHECK (status IN ('completed', 'cancelled', 'refunded'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- purchase_item (depends on purchase, product)
-- quantity changed from DECIMAL to INT to match sale_item/product
-- ------------------------------------------------------------

CREATE TABLE purchase_item (
                               purchase_item_id    INT AUTO_INCREMENT PRIMARY KEY,
                               purchase_id         INT NOT NULL,
                               product_id          INT NOT NULL,
                               quantity             INT NOT NULL,
                               unit_cost            DECIMAL(10,2) NOT NULL,
                               line_total           DECIMAL(10,2) NOT NULL,
                               deleted_at            DATETIME(6) NULL,
                               CONSTRAINT fk_purchaseitem_purchase
                                   FOREIGN KEY (purchase_id) REFERENCES purchase(purchase_id)
                                       ON UPDATE CASCADE ON DELETE RESTRICT,
                               CONSTRAINT fk_purchaseitem_product
                                   FOREIGN KEY (product_id) REFERENCES product(product_id)
                                       ON UPDATE CASCADE ON DELETE RESTRICT,
                               CONSTRAINT chk_purchaseitem_qty_positive CHECK (quantity > 0),
                               CONSTRAINT chk_purchaseitem_unit_cost_nonneg CHECK (unit_cost >= 0),
                               CONSTRAINT chk_purchaseitem_line_total_nonneg CHECK (line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- movement (depends on product, user) — insert-only stock ledger, no deleted_at
-- ------------------------------------------------------------

CREATE TABLE movement (
                          movement_id         INT AUTO_INCREMENT PRIMARY KEY,
                          product_id          INT NOT NULL,
                          movement_type       VARCHAR(30) NOT NULL,
                          quantity_change      INT NOT NULL,
                          reference_id        INT,
                          reason               VARCHAR(255),
                          user_id             INT NOT NULL,
                          movement_date       DATETIME NOT NULL,
                          CONSTRAINT fk_movement_product
                              FOREIGN KEY (product_id) REFERENCES product(product_id)
                                  ON UPDATE CASCADE ON DELETE RESTRICT,
                          CONSTRAINT fk_movement_user
                              FOREIGN KEY (user_id) REFERENCES user(user_id)
                                  ON UPDATE CASCADE ON DELETE RESTRICT,
                          CONSTRAINT chk_movement_type CHECK (
                              movement_type IN ('sale', 'purchase', 'adjustment', 'opening_stock', 'return')
                              )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Helpful indexes (FKs are auto-indexed by InnoDB, these add more)
-- ------------------------------------------------------------

CREATE INDEX idx_sale_date ON sale(sale_date);
CREATE INDEX idx_purchase_date ON purchase(purchase_date);
CREATE INDEX idx_movement_date ON movement(movement_date);
CREATE INDEX idx_movement_reference ON movement(reference_id);

-- Soft-delete filtering indexes — every findAll()/findById() query filters
-- on deleted_at IS NULL, so this keeps those scans fast as tables grow.
CREATE INDEX idx_role_deleted_at ON role(deleted_at);
CREATE INDEX idx_unit_deleted_at ON unit(deleted_at);
CREATE INDEX idx_category_deleted_at ON category(deleted_at);
CREATE INDEX idx_supplier_deleted_at ON supplier(deleted_at);
CREATE INDEX idx_customer_deleted_at ON customer(deleted_at);
CREATE INDEX idx_user_deleted_at ON user(deleted_at);
CREATE INDEX idx_product_deleted_at ON product(deleted_at);
CREATE INDEX idx_sale_deleted_at ON sale(deleted_at);
CREATE INDEX idx_sale_item_deleted_at ON sale_item(deleted_at);
CREATE INDEX idx_purchase_deleted_at ON purchase(deleted_at);
CREATE INDEX idx_purchase_item_deleted_at ON purchase_item(deleted_at);

SET FOREIGN_KEY_CHECKS = 1;