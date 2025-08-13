-- Fix currency column type from CHAR(3) to VARCHAR(3) to match JPA entity
ALTER TABLE invoices ALTER COLUMN currency TYPE VARCHAR(3);
