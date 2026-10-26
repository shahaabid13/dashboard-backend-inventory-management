-- Normalize the weighbridge composite-key column to the Java mapping `wb_id`.
-- Run this once in inventory_db on local and production before syncing.

SET @has_wb_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'weighbridge_entries'
      AND column_name = 'wb_id'
);

SET @has_legacy_wb_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'weighbridge_entries'
      AND column_name = 'wbId'
);

SET @copy_sql = CASE
    WHEN @has_wb_id = 1 AND @has_legacy_wb_id = 1
        THEN 'UPDATE weighbridge_entries SET wb_id = wbId WHERE wb_id IS NULL OR wb_id = '''''
    ELSE 'SELECT 1'
END;

PREPARE copy_weighbridge_ids FROM @copy_sql;
EXECUTE copy_weighbridge_ids;
DEALLOCATE PREPARE copy_weighbridge_ids;

SET @sql = CASE
    WHEN @has_wb_id = 1 AND @has_legacy_wb_id = 1
        THEN 'ALTER TABLE weighbridge_entries DROP COLUMN wbId'
    WHEN @has_wb_id = 0 AND @has_legacy_wb_id = 1
        THEN 'ALTER TABLE weighbridge_entries CHANGE COLUMN wbId wb_id VARCHAR(255) NOT NULL'
    ELSE 'SELECT 1'
END;

PREPARE normalize_weighbridge_columns FROM @sql;
EXECUTE normalize_weighbridge_columns;
DEALLOCATE PREPARE normalize_weighbridge_columns;
