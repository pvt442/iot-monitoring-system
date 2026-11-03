package com.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.model.SensorData;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Integer> {
    
    // Lấy toàn bộ lịch sử, sắp xếp mới nhất lên đầu
    List<SensorData> findAllByOrderByTimestampDesc();

    // 1. API Lấy lịch sử cảm biến theo mã thiết bị (để vẽ biểu đồ)
    List<SensorData> findByDevice_DeviceIdOrderByTimestampDesc(String deviceId);

    // 2. API Lấy dữ liệu cảm biến mới nhất của thiết bị (để hiển thị thông số hiện tại)
    SensorData findFirstByDevice_DeviceIdOrderByTimestampDesc(String deviceId);
}