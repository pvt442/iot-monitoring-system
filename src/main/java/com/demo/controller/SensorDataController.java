package com.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.dto.PageResponse;
import com.demo.dto.SensorDataRequest;
import com.demo.dto.SensorDataResponse;
import com.demo.service.SensorDataService;

@RestController
@RequestMapping("/api/sensor-data")
public class SensorDataController {

    private final SensorDataService sensorDataService;

    public SensorDataController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    /** IoT Client gửi dữ liệu cảm biến. Trả 201 kèm alertStatus/alerts của lần đo này. */
    @PostMapping
    public ResponseEntity<SensorDataResponse> receiveSensorData(@RequestBody SensorDataRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorDataService.receive(request));
    }

    /** Lịch sử toàn hệ thống, mới nhất trước. Tham số tùy chọn: deviceId, from, to, limit (mặc định 500, tối đa 5000). */
    @GetMapping
    public List<SensorDataResponse> getAllHistory(
            @RequestParam(name = "deviceId", required = false) String deviceId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return sensorDataService.getAllHistory(deviceId, from, to, limit);
    }

    /** Tìm kiếm có phân trang: ?deviceId=&from=&to=&page=0&size=20 */
    @GetMapping("/search")
    public PageResponse<SensorDataResponse> search(
            @RequestParam(name = "deviceId", required = false) String deviceId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return sensorDataService.search(deviceId, from, to, page, size);
    }

    /** Lịch sử của một thiết bị (vẽ biểu đồ), mới nhất trước. Tham số tùy chọn: from, to, limit. */
    @GetMapping("/{deviceId}/history")
    public List<SensorDataResponse> getHistoryData(
            @PathVariable("deviceId") String deviceId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return sensorDataService.getHistoryByDevice(deviceId, from, to, limit);
    }

    /** Bản ghi mới nhất của một thiết bị; 404 nếu thiết bị không tồn tại hoặc chưa có dữ liệu. */
    @GetMapping("/{deviceId}/latest")
    public SensorDataResponse getLatestData(@PathVariable("deviceId") String deviceId) {
        return sensorDataService.getLatestByDevice(deviceId);
    }
}
