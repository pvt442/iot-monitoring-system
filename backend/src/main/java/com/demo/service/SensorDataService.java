package com.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.demo.model.SensorData;
import com.demo.repository.SensorDataRepository;

@Service
public class SensorDataService {

    @Autowired
    private SensorDataRepository sensorDataRepository;

    // Lưu dữ liệu cảm biến và kiểm tra cảnh báo
    public SensorData saveSensorData(SensorData data) {
        // Logic nghiệp vụ: Cảnh báo nếu nhiệt độ vượt quá 35 độ C
        if (data.getTemperature() != null && data.getTemperature() > 35.0f) {
            System.out.println("⚠️ CẢNH BÁO QUAN TRỌNG: Thiết bị đang quá nhiệt! Nhiệt độ hiện tại: "
                    + data.getTemperature() + "°C");
            // Ghi chú: Sau này nhóm bạn có thể code thêm tính năng gửi Email hoặc gửi thông báo Telegram ở đây
        }

        return sensorDataRepository.save(data);
    }
    public List<SensorData> getAllHistory() {
        return sensorDataRepository.findAllByOrderByTimestampDesc();
    }

    // Lấy lịch sử theo thiết bị cụ thể
    public List<SensorData> getHistoryByDevice(String deviceId) {
        return sensorDataRepository.findByDevice_DeviceIdOrderByTimestampDesc(deviceId);
    }

    // Lấy dữ liệu mới nhất theo thiết bị cụ thể
    public SensorData getLatestByDevice(String deviceId) {
        return sensorDataRepository.findFirstByDevice_DeviceIdOrderByTimestampDesc(deviceId);
    }
}