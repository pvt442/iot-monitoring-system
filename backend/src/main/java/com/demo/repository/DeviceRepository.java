package com.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.model.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    // JpaRepository đã cung cấp sẵn các hàm như save(), findAll(), findById(), deleteById()...
    // Ký hiệu <Device, String> nghĩa là: Quản lý bảng Device, và khóa chính (deviceId) có kiểu String.
}