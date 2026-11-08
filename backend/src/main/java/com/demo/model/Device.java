package com.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Thiết bị IoT được đăng ký trong hệ thống (bảng devices). */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String location;

    /** Trạng thái quản lý (ACTIVE / INACTIVE). Online/Offline được tính từ lastSeen, không lưu ở đây. */
    @Column(length = 20)
    private String status;

    /** Thời điểm Server nhận gói dữ liệu gần nhất từ thiết bị (dùng để tính Online/Offline). */
    @Column(name = "last_seen", columnDefinition = "DATETIME(3)")
    private LocalDateTime lastSeen;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
