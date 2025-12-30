CREATE DATABASE IF NOT EXISTS ec_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ec_system;

CREATE TABLE IF NOT EXISTS sys_pay_alert (
  id BIGINT NOT NULL AUTO_INCREMENT,
  alert_id VARCHAR(36) NOT NULL COMMENT 'UUID from AlertRaised.alertId',
  order_id VARCHAR(64) NOT NULL COMMENT 'Business order identifier',
  rule CHAR(1) NOT NULL COMMENT 'A/B/C',
  severity VARCHAR(2) NOT NULL COMMENT 'P1/P2',
  detected_at DATETIME(3) NOT NULL COMMENT 'Alert detected time',
  status VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/ACK/RESOLVED',
  message VARCHAR(255) NOT NULL COMMENT 'Human readable summary',
  facts_json JSON NULL COMMENT 'Evidence/facts for UI/debug',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_pay_alert_alert_id (alert_id),
  KEY idx_sys_pay_alert_order_id (order_id),
  KEY idx_sys_pay_alert_detected_at (detected_at),
  KEY idx_sys_pay_alert_status_detected (status, detected_at)
) ENGINE=InnoDB;

ALTER TABLE sys_pay_alert
  ADD CONSTRAINT chk_sys_pay_alert_rule CHECK (rule IN ('A','B','C')),
  ADD CONSTRAINT chk_sys_pay_alert_severity CHECK (severity IN ('P1','P2')),
  ADD CONSTRAINT chk_sys_pay_alert_status CHECK (status IN ('NEW','ACK','RESOLVED'));
