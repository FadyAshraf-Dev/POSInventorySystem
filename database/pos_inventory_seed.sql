-- ============================================================
-- POS & Inventory Management System - Seed Data (v2)
-- Run AFTER pos_inventory_schema.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- role (Admin, Cashier only — per earlier decision)
-- ------------------------------------------------------------
INSERT INTO role (role_id, role_name) VALUES
                                          (1, 'Admin'),
                                          (2, 'Cashier');

-- ------------------------------------------------------------
-- setting
-- ------------------------------------------------------------
INSERT INTO setting (setting_id, store_name, currency, tax_rate, store_address, store_phone, store_email) VALUES
    (1, 'Nexus Fashion Store', 'EGP', 14.00, '12 El Tahrir St, Cairo, Egypt', '0223456789', 'contact@nexusfashion.example');

-- ------------------------------------------------------------
-- unit
-- ------------------------------------------------------------
INSERT INTO unit (unit_id, unit_name) VALUES
                                          (1, 'Piece'),
                                          (2, 'Pair'),
                                          (3, 'Box'),
                                          (4, 'Meter');

-- ------------------------------------------------------------
-- category
-- ------------------------------------------------------------
INSERT INTO category (category_id, category_name) VALUES
                                                      (1, 'Men Apparel'),
                                                      (2, 'Women Apparel'),
                                                      (3, 'Footwear'),
                                                      (4, 'Accessories'),
                                                      (5, 'Kids Wear');

-- ------------------------------------------------------------
-- supplier
-- ------------------------------------------------------------
INSERT INTO supplier (supplier_id, supplier_name, phone, address) VALUES
                                                                      (1, 'Delta Textiles Co.', '01012345678', '45 Industrial Zone, 6th of October City, Egypt'),
                                                                      (2, 'Cairo Footwear Supply', '01098765432', '8 Nasr Road, Nasr City, Cairo, Egypt'),
                                                                      (3, 'Alexandria Fabrics', '01123456789', '22 Corniche St, Alexandria, Egypt');

-- ------------------------------------------------------------
-- customer
-- ------------------------------------------------------------
INSERT INTO customer (customer_id, customer_name, phone, address) VALUES
                                                                      (1, 'Walk-in Customer', NULL, NULL),
                                                                      (2, 'Mariam Adel', '01234567890', '15 Mohandessin St, Giza, Egypt'),
                                                                      (3, 'Omar Khaled', '01555123456', '3 Heliopolis Ave, Cairo, Egypt'),
                                                                      (4, 'Nourhan Sameh', '01099988776', '9 Maadi Corniche, Cairo, Egypt');

-- ------------------------------------------------------------
-- user  (password_hash values are placeholders — bcrypt('password123') style)
-- ------------------------------------------------------------
INSERT INTO user (user_id, full_name, username, password_hash, role_id, is_active) VALUES
                                                                                       (1, 'Fady Admin', 'fady.admin', '$2y$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPQRS', 1, 1),
                                                                                       (2, 'Hana Mostafa', 'hana.m', '$2y$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPQRT', 2, 1),
                                                                                       (3, 'Youssef Tarek', 'youssef.t', '$2y$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPQRU', 2, 1);

-- ------------------------------------------------------------
-- product (is_active defaults to 1 — all active/sellable)
-- ------------------------------------------------------------
INSERT INTO product (product_id, barcode, product_name, category_id, unit_id, cost_price, selling_price, quantity_in_stock, reorder_level, is_active) VALUES
                                                                                                                                                          (1, '6291001000011', 'Men Cotton T-Shirt - Black', 1, 1, 80.00, 150.00, 120, 20, 1),
                                                                                                                                                          (2, '6291001000028', 'Men Slim Fit Jeans - Blue', 1, 1, 180.00, 320.00, 60, 15, 1),
                                                                                                                                                          (3, '6291001000035', 'Women Summer Dress - Floral', 2, 1, 150.00, 280.00, 45, 10, 1),
                                                                                                                                                          (4, '6291001000042', 'Women Blouse - White', 2, 1, 90.00, 170.00, 70, 15, 1),
                                                                                                                                                          (5, '6291001000059', 'Men Leather Sneakers', 3, 2, 300.00, 550.00, 35, 10, 1),
                                                                                                                                                          (6, '6291001000066', 'Women Heels - Nude', 3, 2, 250.00, 480.00, 25, 8, 1),
                                                                                                                                                          (7, '6291001000073', 'Leather Belt - Brown', 4, 1, 60.00, 120.00, 90, 20, 1),
                                                                                                                                                          (8, '6291001000080', 'Kids Hoodie - Grey', 5, 1, 70.00, 140.00, 50, 10, 1);

-- ------------------------------------------------------------
-- movement (opening stock entries for each product)
-- ------------------------------------------------------------
INSERT INTO movement (movement_id, product_id, movement_type, quantity_change, reference_id, reason, user_id, movement_date) VALUES
                                                                                                                                 (1, 1, 'opening_stock', 120, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00'),
                                                                                                                                 (2, 2, 'opening_stock', 60, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00'),
                                                                                                                                 (3, 3, 'opening_stock', 45, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00'),
                                                                                                                                 (4, 4, 'opening_stock', 70, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00'),
                                                                                                                                 (5, 5, 'opening_stock', 35, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00'),
                                                                                                                                 (6, 6, 'opening_stock', 25, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00'),
                                                                                                                                 (7, 7, 'opening_stock', 90, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00'),
                                                                                                                                 (8, 8, 'opening_stock', 50, NULL, 'Initial stock load', 1, '2026-01-05 09:00:00');

-- ------------------------------------------------------------
-- purchase + purchase_item (status defaults to 'completed')
-- ------------------------------------------------------------
INSERT INTO purchase (purchase_id, reference_no, purchase_date, supplier_id, user_id, total, status) VALUES
                                                                                                         (1, 'PO-2026-0001', '2026-11-01 10:30:00', 1, 2, 7200.00, 'completed'),
                                                                                                         (2, 'PO-2026-0002', '2026-11-10 14:15:00', 2, 2, 9500.00, 'completed');

-- NOTE: quantity is now INT (was DECIMAL in v1) — values below are unchanged
-- whole numbers, so no data loss from the type change in this seed.
INSERT INTO purchase_item (purchase_item_id, purchase_id, product_id, quantity, unit_cost, line_total) VALUES
                                                                                                           (1, 1, 1, 40, 80.00, 3200.00),
                                                                                                           (2, 1, 2, 10, 180.00, 1800.00),
                                                                                                           (3, 1, 4, 20, 90.00, 1800.00),
                                                                                                           (4, 1, 7, 10, 60.00, 600.00),
                                                                                                           (5, 2, 5, 20, 300.00, 6000.00),
                                                                                                           (6, 2, 6, 14, 250.00, 3500.00);

INSERT INTO movement (movement_id, product_id, movement_type, quantity_change, reference_id, reason, user_id, movement_date) VALUES
                                                                                                                                 (9,  1, 'purchase', 40, 1, 'Received PO-2026-0001', 2, '2026-11-01 10:30:00'),
                                                                                                                                 (10, 2, 'purchase', 10, 1, 'Received PO-2026-0001', 2, '2026-11-01 10:30:00'),
                                                                                                                                 (11, 4, 'purchase', 20, 1, 'Received PO-2026-0001', 2, '2026-11-01 10:30:00'),
                                                                                                                                 (12, 7, 'purchase', 10, 1, 'Received PO-2026-0001', 2, '2026-11-01 10:30:00'),
                                                                                                                                 (13, 5, 'purchase', 20, 2, 'Received PO-2026-0002', 2, '2026-11-10 14:15:00'),
                                                                                                                                 (14, 6, 'purchase', 14, 2, 'Received PO-2026-0002', 2, '2026-11-10 14:15:00');

-- ------------------------------------------------------------
-- sale + sale_item + payment (status defaults to 'completed')
-- sale_item now has a discount column (defaulted to 0.00 here)
-- ------------------------------------------------------------
INSERT INTO sale (sale_id, invoice_no, sale_date, user_id, customer_id, subtotal, discount, tax, total, status) VALUES
                                                                                                                    (1, 'INV-2026-0001', '2026-11-20 11:05:00', 3, 2, 420.00, 0.00, 65.80, 485.80, 'completed'),
                                                                                                                    (2, 'INV-2026-0002', '2026-11-21 16:40:00', 3, 1, 670.00, 20.00, 91.00, 741.00, 'completed'),
                                                                                                                    (3, 'INV-2026-0003', '2026-11-22 13:20:00', 2, 3, 120.00, 0.00, 16.80, 136.80, 'completed');

INSERT INTO sale_item (sale_item_id, sale_id, product_id, quantity, unit_price, unit_cost, discount, line_total) VALUES
                                                                                                          (1, 1, 1, 2, 150.00, 80.00, 0.00, 300.00),
                                                                                                          (2, 1, 7, 1, 120.00, 60.00, 0.00, 120.00),
                                                                                                          (3, 2, 5, 1, 550.00, 300.00, 0.00, 550.00),
                                                                                                          (4, 2, 7, 1, 120.00, 60.00, 0.00, 120.00),
                                                                                                          (5, 3, 7, 1, 120.00, 60.00, 0.00, 120.00);

INSERT INTO payment (payment_id, sale_id, method, amount, payment_date) VALUES
                                                                            (1, 1, 'cash', 485.80, '2026-11-20 11:06:00'),
                                                                            (2, 2, 'card', 741.00, '2026-11-21 16:41:00'),
                                                                            (3, 3, 'cash', 136.80, '2026-11-22 13:21:00');

INSERT INTO movement (movement_id, product_id, movement_type, quantity_change, reference_id, reason, user_id, movement_date) VALUES
                                                                                                                                 (15, 1, 'sale', -2, 1, 'Sold on INV-2026-0001', 3, '2026-11-20 11:05:00'),
                                                                                                                                 (16, 7, 'sale', -1, 1, 'Sold on INV-2026-0001', 3, '2026-11-20 11:05:00'),
                                                                                                                                 (17, 5, 'sale', -1, 2, 'Sold on INV-2026-0002', 3, '2026-11-21 16:40:00'),
                                                                                                                                 (18, 7, 'sale', -1, 2, 'Sold on INV-2026-0002', 3, '2026-11-21 16:40:00'),
                                                                                                                                 (19, 7, 'sale', -1, 3, 'Sold on INV-2026-0003', 2, '2026-11-22 13:20:00');

-- ------------------------------------------------------------
-- Reconcile product.quantity_in_stock to reflect purchases/sales above
-- ------------------------------------------------------------
UPDATE product SET quantity_in_stock = 120 + 40 - 2 WHERE product_id = 1;   -- 158
UPDATE product SET quantity_in_stock = 60 + 10 WHERE product_id = 2;        -- 70
UPDATE product SET quantity_in_stock = 45 WHERE product_id = 3;             -- 45
UPDATE product SET quantity_in_stock = 70 + 20 WHERE product_id = 4;        -- 90
UPDATE product SET quantity_in_stock = 35 + 20 - 1 WHERE product_id = 5;    -- 54
UPDATE product SET quantity_in_stock = 25 + 14 WHERE product_id = 6;        -- 39
UPDATE product SET quantity_in_stock = 90 + 10 - 3 WHERE product_id = 7;    -- 97
UPDATE product SET quantity_in_stock = 50 WHERE product_id = 8;             -- 50

SET FOREIGN_KEY_CHECKS = 1;