-- TCC mode seed data

-- TCC Storage: one product with stock
USE `seata_storage`;
INSERT INTO `tcc_storage` (xid, product_id, total, used, residue, frozen, status) VALUES ('init', 1, 100, 0, 100, 0, 1)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue), frozen=VALUES(frozen), status=VALUES(status);

-- TCC Account: two users with balances
USE `seata_account`;
INSERT INTO `tcc_account` (xid, user_id, total, used, residue, frozen, status) VALUES ('init', 1, 1000.00, 0.00, 1000.00, 0.00, 1)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue), frozen=VALUES(frozen), status=VALUES(status);
INSERT INTO `tcc_account` (xid, user_id, total, used, residue, frozen, status) VALUES ('init', 2, 50.00, 0.00, 50.00, 0.00, 1)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue), frozen=VALUES(frozen), status=VALUES(status);

-- TCC Order has no initial data (created by TCC operations)
USE `seata_order`;
