package com.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.model.AlertLog;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    Page<AlertLog> findByDevice_DeviceId(String deviceId, Pageable pageable);

    Page<AlertLog> findByMetric(String metric, Pageable pageable);

    Page<AlertLog> findByDevice_DeviceIdAndMetric(String deviceId, String metric, Pageable pageable);

    void deleteByDevice_DeviceId(String deviceId);
}
