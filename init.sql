-- ============================================================
--  IoT Data Server - khởi tạo cơ sở dữ liệu (MySQL 8+)
--  Chạy:  mysql -u root -p < init.sql
--  Script an toàn khi chạy lại (CREATE ... IF NOT EXISTS), không xóa dữ liệu.
-- ============================================================
CREATE DATABASE IF NOT EXISTS iot_monitoring
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE iot_monitoring;

-- 1. Thiết bị IoT
CREATE TABLE IF NOT EXISTS devices (
    device_id VARCHAR(50)  NOT NULL PRIMARY KEY,
    name      VARCHAR(100),
    location  VARCHAR(100),
    status    VARCHAR(20)  DEFAULT 'ACTIVE',          -- ACTIVE / INACTIVE (trạng thái quản lý)
    last_seen DATETIME(3)  NULL                        -- lần cuối Server nhận dữ liệu -> tính Online/Offline
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Lịch sử dữ liệu cảm biến (1 thiết bị : n bản ghi)
CREATE TABLE IF NOT EXISTS sensor_data (
    data_id     INT AUTO_INCREMENT PRIMARY KEY,
    device_id   VARCHAR(50) NOT NULL,
    temperature FLOAT,                                 -- °C
    humidity    FLOAT,                                 -- %
    timestamp   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_sensor_device FOREIGN KEY (device_id) REFERENCES devices(device_id),
    INDEX idx_sensor_device_time (device_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Nhật ký cảnh báo (Server ghi khi một chỉ số MỚI vượt ngưỡng)
CREATE TABLE IF NOT EXISTS alert_logs (
    alert_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id       VARCHAR(50) NOT NULL,
    data_id         INT NULL,                          -- bản ghi cảm biến gây cảnh báo
    metric          VARCHAR(20) NOT NULL,              -- TEMPERATURE / HUMIDITY
    alert_level     VARCHAR(20) NOT NULL,              -- CRITICAL
    actual_value    FLOAT,
    threshold_value FLOAT,
    message         VARCHAR(255),
    created_at      DATETIME(3),
    CONSTRAINT fk_alert_device FOREIGN KEY (device_id) REFERENCES devices(device_id),
    INDEX idx_alert_device_time (device_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Cấu hình ngưỡng dùng chung (luôn đúng 1 dòng, id = 1)
CREATE TABLE IF NOT EXISTS alert_config (
    id                         INT PRIMARY KEY,
    temperature_threshold      FLOAT NOT NULL DEFAULT 35,   -- cảnh báo khi nhiệt độ > 35 °C
    humidity_threshold         FLOAT NOT NULL DEFAULT 80,   -- cảnh báo khi độ ẩm > 80 %
    temperature_warning_margin FLOAT NOT NULL DEFAULT 3,    -- "sắp vượt ngưỡng": từ 32 °C
    humidity_warning_margin    FLOAT NOT NULL DEFAULT 5,    -- "sắp vượt ngưỡng": từ 75 %
    offline_timeout_seconds    INT   NOT NULL DEFAULT 15,   -- quá 15 giây không có dữ liệu -> Offline
    updated_at                 DATETIME(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO alert_config (id, updated_at)
SELECT 1, NOW(3) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM alert_config WHERE id = 1);

-- ------------------------------------------------------------
--  Chỉ chạy các dòng cần thiết, một lần. Backend (ddl-auto=update) cũng tự thêm cột/bảng còn thiếu.
-- ------------------------------------------------------------
-- ALTER TABLE devices     ADD COLUMN last_seen DATETIME(3) NULL;
-- ALTER TABLE sensor_data MODIFY COLUMN timestamp DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3);
-- ALTER TABLE sensor_data MODIFY COLUMN device_id VARCHAR(50) NOT NULL;
-- CREATE INDEX idx_sensor_device_time ON sensor_data (device_id, timestamp);
