package com.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.model.SensorData;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Integer> {
    // Ký hiệu <SensorData, Integer> vì khóa chính (dataId) của bảng này là kiểu Integer.
}