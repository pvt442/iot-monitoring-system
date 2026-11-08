package com.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.model.SensorData;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Integer> {

    /** Bản ghi mới nhất của một thiết bị (thứ tự: thời điểm đo, rồi mã bản ghi). */
    Optional<SensorData> findFirstByDevice_DeviceIdOrderByTimestampDescDataIdDesc(String deviceId);

    /** Danh sách (không phân trang) - sắp xếp/giới hạn truyền qua Pageable. */
    List<SensorData> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<SensorData> findByDevice_DeviceIdAndTimestampBetween(
            String deviceId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** Có phân trang (kèm tổng số bản ghi) - dùng cho trang Data History. */
    Page<SensorData> findPageByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<SensorData> findPageByDevice_DeviceIdAndTimestampBetween(
            String deviceId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    void deleteByDevice_DeviceId(String deviceId);
}
