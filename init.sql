CREATE DATABASE IF NOT EXISTS iot_monitoring;
USE iot_monitoring;

CREATE TABLE devices (
    device_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100),
    location VARCHAR(100),
    status VARCHAR(20)
);

CREATE TABLE sensor_data (
    data_id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(50),
    temperature FLOAT,
    humidity FLOAT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
);