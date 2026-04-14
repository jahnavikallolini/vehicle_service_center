-- ============================================================
--  AutoService Pro – Stored Procedures & Triggers
--  Run this once after creating your database tables
-- ============================================================

USE vehicleservicecenter;   -- ← change to your database name

DELIMITER $$

-- ─── 1. Create_Service_Order ────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS Create_Service_Order$$
CREATE PROCEDURE Create_Service_Order(
    IN  p_vehicle_id          INT,
    IN  p_service_date        DATE,
    IN  p_problem_description TEXT
)
BEGIN
    -- Trigger "Before insert" will validate vehicle exists
    INSERT INTO Service_Orders(vehicle_id, service_date, problem_description, status)
    VALUES (p_vehicle_id, p_service_date, p_problem_description, 'Open');
END$$

-- ─── 2. Assign_Mechanic ─────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS Assign_Mechanic$$
CREATE PROCEDURE Assign_Mechanic(
    IN p_order_id    INT,
    IN p_mechanic_id INT
)
BEGIN
    DECLARE v_avail VARCHAR(20);
    SELECT availability_status INTO v_avail FROM Mechanics WHERE mechanic_id = p_mechanic_id;
    IF v_avail != 'Available' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Mechanic is not available';
    END IF;
    UPDATE Service_Orders SET mechanic_id = p_mechanic_id, status = 'In Progress'
    WHERE order_id = p_order_id;
    UPDATE Mechanics SET availability_status = 'Busy' WHERE mechanic_id = p_mechanic_id;
END$$

-- ─── 3. Add_Part_Usage ──────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS Add_Part_Usage$$
CREATE PROCEDURE Add_Part_Usage(
    IN p_order_id INT,
    IN p_part_id  INT,
    IN p_quantity INT
)
BEGIN
    DECLARE v_price    DECIMAL(10,2);
    DECLARE v_stock    INT;
    DECLARE v_line_cost DECIMAL(10,2);

    SELECT price, stock_qty INTO v_price, v_stock FROM Spare_Parts WHERE part_id = p_part_id;

    IF v_stock < p_quantity THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient stock';
    END IF;

    SET v_line_cost = v_price * p_quantity;

    INSERT INTO Parts_Used(order_id, part_id, quantity, line_cost)
    VALUES (p_order_id, p_part_id, p_quantity, v_line_cost);
    -- Stock reduction is handled by AFTER INSERT trigger on Parts_Used
END$$

-- ─── 4. Generate_Bill ───────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS Generate_Bill$$
CREATE PROCEDURE Generate_Bill(
    IN p_order_id INT,
    IN p_tax_rate DECIMAL(5,2)   -- e.g. 18.00 for 18%
)
BEGIN
    DECLARE v_labor  DECIMAL(10,2) DEFAULT 0;
    DECLARE v_parts  DECIMAL(10,2) DEFAULT 0;
    DECLARE v_tax    DECIMAL(10,2) DEFAULT 0;
    DECLARE v_total  DECIMAL(10,2) DEFAULT 0;

    SELECT COALESCE(SUM(service_cost),0) INTO v_labor
    FROM Service_Order_Details WHERE order_id = p_order_id;

    SELECT COALESCE(SUM(line_cost),0) INTO v_parts
    FROM Parts_Used WHERE order_id = p_order_id;

    SET v_tax   = (v_labor + v_parts) * p_tax_rate / 100;
    SET v_total = v_labor + v_parts + v_tax;

    -- Upsert: replace existing bill for this order
    INSERT INTO Bills(order_id, labor_total, parts_total, tax, grand_total, payment_status)
    VALUES (p_order_id, v_labor, v_parts, v_tax, v_total, 'Pending')
    ON DUPLICATE KEY UPDATE
        labor_total    = v_labor,
        parts_total    = v_parts,
        tax            = v_tax,
        grand_total    = v_total;
END$$

-- ─── 5. Complete_Service ────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS Complete_Service$$
CREATE PROCEDURE Complete_Service(IN p_order_id INT)
BEGIN
    DECLARE v_mechanic_id INT;

    SELECT mechanic_id INTO v_mechanic_id FROM Service_Orders WHERE order_id = p_order_id;

    UPDATE Service_Orders SET status = 'Completed' WHERE order_id = p_order_id;

    -- Make mechanic available again (trigger also does this, but belt+suspenders)
    IF v_mechanic_id IS NOT NULL THEN
        UPDATE Mechanics SET availability_status = 'Available' WHERE mechanic_id = v_mechanic_id;
    END IF;
END$$

DELIMITER ;

-- ============================================================
--  TRIGGERS
-- ============================================================

DELIMITER $$

-- ─── T1: After insert on Parts_Used → reduce stock ──────────────────────────
DROP TRIGGER IF EXISTS trg_parts_used_after_insert$$
CREATE TRIGGER trg_parts_used_after_insert
AFTER INSERT ON Parts_Used
FOR EACH ROW
BEGIN
    UPDATE Spare_Parts SET stock_qty = stock_qty - NEW.quantity WHERE part_id = NEW.part_id;
END$$

-- ─── T2: Before insert on Spare_Parts → prevent negative price ───────────────
DROP TRIGGER IF EXISTS trg_spare_parts_before_insert$$
CREATE TRIGGER trg_spare_parts_before_insert
BEFORE INSERT ON Spare_Parts
FOR EACH ROW
BEGIN
    IF NEW.price < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Price cannot be negative';
    END IF;
    IF NEW.stock_qty < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock quantity cannot be negative';
    END IF;
END$$

-- ─── T3: After service completion → mark mechanic available ──────────────────
DROP TRIGGER IF EXISTS trg_order_after_complete$$
CREATE TRIGGER trg_order_after_complete
AFTER UPDATE ON Service_Orders
FOR EACH ROW
BEGIN
    IF NEW.status = 'Completed' AND OLD.status != 'Completed' AND NEW.mechanic_id IS NOT NULL THEN
        UPDATE Mechanics SET availability_status = 'Available' WHERE mechanic_id = NEW.mechanic_id;
    END IF;
END$$

-- ─── T4: Before service order insert → validate vehicle exists ───────────────
DROP TRIGGER IF EXISTS trg_order_before_insert$$
CREATE TRIGGER trg_order_before_insert
BEFORE INSERT ON Service_Orders
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count FROM Vehicles WHERE vehicle_id = NEW.vehicle_id;
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle does not exist';
    END IF;
END$$

DELIMITER ;

-- Ensure Bills has a unique constraint on order_id for ON DUPLICATE KEY to work:
ALTER TABLE Bills ADD UNIQUE IF NOT EXISTS (order_id);
