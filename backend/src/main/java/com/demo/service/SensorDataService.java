package com.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.dto.PageResponse;
import com.demo.dto.SensorDataRequest;
import com.demo.dto.SensorDataResponse;
import com.demo.exception.ApiException;
import com.demo.model.AlertConfig;
import com.demo.model.Device;
import com.demo.model.SensorData;
import com.demo.repository.DeviceRepository;
import com.demo.repository.SensorDataRepository;
import com.demo.service.AlertEvaluator.Evaluation;
import com.demo.util.TimeUtil;

@Service
public class SensorDataService {

    private static final Logger log = LoggerFactory.getLogger(SensorDataService.class);

    /** Khoảng giá trị hợp lệ của cảm biến (ngoài khoảng này coi là dữ liệu lỗi). */
    public static final double MIN_TEMP = -40, MAX_TEMP = 125, MIN_HUM = 0, MAX_HUM = 100;
    public static final int DEFAULT_LIMIT = 500, MAX_LIMIT = 5000;

    private final SensorDataRepository sensorDataRepository;
    private final DeviceRepository deviceRepository;
    private final ConfigService configService;
    private final AlertEvaluator evaluator;
    private final AlertService alertService;
    private final DtoMapper mapper;
    private final boolean autoRegister;

    public SensorDataService(SensorDataRepository sensorDataRepository, DeviceRepository deviceRepository,
                             ConfigService configService, AlertEvaluator evaluator, AlertService alertService,
                             DtoMapper mapper,
                             @Value("${iot.auto-register-devices:false}") boolean autoRegister) {
        this.sensorDataRepository = sensorDataRepository;
        this.deviceRepository = deviceRepository;
        this.configService = configService;
        this.evaluator = evaluator;
        this.alertService = alertService;
        this.mapper = mapper;
        this.autoRegister = autoRegister;
    }

    /**
     * Luồng tiếp nhận dữ liệu: kiểm tra hợp lệ -> xác định thiết bị -> lưu -> cập nhật lastSeen
     * -> đánh giá ngưỡng -> ghi nhật ký cảnh báo (nếu mới vượt ngưỡng) -> trả kết quả kèm trạng thái cảnh báo.
     */
    @Transactional
    public SensorDataResponse receive(SensorDataRequest req) {
        String deviceId = req.resolveDeviceId();
        if (deviceId == null) {
            throw ApiException.badRequest("Thiếu deviceId (dùng trường \"deviceId\" hoặc \"device\":{\"deviceId\":...}).");
        }
        if (deviceId.length() > 50) {
            throw ApiException.badRequest("deviceId tối đa 50 ký tự.");
        }
        if (req.temperature() == null || req.humidity() == null) {
            throw ApiException.badRequest("Thiếu temperature hoặc humidity.");
        }
        double temp = req.temperature();
        double hum = req.humidity();
        if (Double.isNaN(temp) || temp < MIN_TEMP || temp > MAX_TEMP) {
            throw ApiException.badRequest("temperature phải trong khoảng " + (int) MIN_TEMP + " đến " + (int) MAX_TEMP + " °C.");
        }
        if (Double.isNaN(hum) || hum < MIN_HUM || hum > MAX_HUM) {
            throw ApiException.badRequest("humidity phải trong khoảng 0 đến 100 %.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime measuredAt = TimeUtil.parseOrDefault(req.timestamp(), now);
        if (measuredAt.isAfter(now.plusMinutes(1))) {
            measuredAt = now; // đồng hồ thiết bị chạy nhanh bất thường -> dùng giờ Server
        }

        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            if (!autoRegister) {
                throw ApiException.notFound("Thiết bị " + deviceId
                        + " chưa được đăng ký. Hãy gọi POST /api/devices trước (hoặc bật iot.auto-register-devices=true).");
            }
            device = new Device();
            device.setDeviceId(deviceId);
            device.setName(deviceId);
            device.setStatus("ACTIVE");
            log.info("Tự đăng ký thiết bị mới: {}", deviceId);
        }

        AlertConfig cfg = configService.getConfig();

        // Đánh giá bản ghi trước đó (nếu có) để biết đây có phải lần MỚI vượt ngưỡng không
        Evaluation previous = sensorDataRepository
                .findFirstByDevice_DeviceIdOrderByTimestampDescDataIdDesc(deviceId)
                .map(p -> evaluator.evaluate(deviceId, DtoMapper.round2(p.getTemperature()),
                        DtoMapper.round2(p.getHumidity()), cfg))
                .orElse(null);

        device.setLastSeen(now);
        Device savedDevice = deviceRepository.save(device);

        SensorData data = new SensorData();
        data.setDevice(savedDevice);
        data.setTemperature((float) round2(temp));
        data.setHumidity((float) round2(hum));
        data.setTimestamp(measuredAt);
        SensorData saved = sensorDataRepository.save(data);

        Evaluation current = evaluator.evaluate(deviceId, round2(temp), round2(hum), cfg);
        alertService.recordTransitions(savedDevice, saved, previous, current);

        return mapper.toSensor(saved, current);
    }

    @Transactional(readOnly = true)
    public SensorDataResponse getLatestByDevice(String deviceId) {
        requireDevice(deviceId);
        SensorData s = sensorDataRepository.findFirstByDevice_DeviceIdOrderByTimestampDescDataIdDesc(deviceId)
                .orElseThrow(() -> ApiException.notFound("Thiết bị " + deviceId + " chưa có dữ liệu cảm biến."));
        return mapper.toSensor(s, configService.getConfig());
    }

    /** Lịch sử của một thiết bị, mới nhất trước. from/to tùy chọn; limit mặc định 500, tối đa 5000. */
    @Transactional(readOnly = true)
    public List<SensorDataResponse> getHistoryByDevice(String deviceId, String from, String to, Integer limit) {
        requireDevice(deviceId);
        return list(deviceId, from, to, limit);
    }

    /** Lịch sử toàn hệ thống (hoặc lọc theo deviceId), mới nhất trước. */
    @Transactional(readOnly = true)
    public List<SensorDataResponse> getAllHistory(String deviceId, String from, String to, Integer limit) {
        if (deviceId != null && !deviceId.isBlank()) {
            requireDevice(deviceId.trim());
        }
        return list(deviceId, from, to, limit);
    }

    /** Tìm kiếm có phân trang cho trang Data History. */
    @Transactional(readOnly = true)
    public PageResponse<SensorDataResponse> search(String deviceId, String from, String to, int page, int size) {
        LocalDateTime f = lowerBound(from);
        LocalDateTime t = upperBound(to);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), order());
        AlertConfig cfg = configService.getConfig();

        Page<SensorData> result;
        if (deviceId != null && !deviceId.isBlank()) {
            requireDevice(deviceId.trim());
            result = sensorDataRepository.findPageByDevice_DeviceIdAndTimestampBetween(deviceId.trim(), f, t, pageable);
        } else {
            result = sensorDataRepository.findPageByTimestampBetween(f, t, pageable);
        }
        return new PageResponse<>(
                result.getContent().stream().map(s -> mapper.toSensor(s, cfg)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private List<SensorDataResponse> list(String deviceId, String from, String to, Integer limit) {
        LocalDateTime f = lowerBound(from);
        LocalDateTime t = upperBound(to);
        int size = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        Pageable pageable = PageRequest.of(0, size, order());
        AlertConfig cfg = configService.getConfig();

        List<SensorData> rows;
        if (deviceId != null && !deviceId.isBlank()) {
            rows = sensorDataRepository.findByDevice_DeviceIdAndTimestampBetween(deviceId.trim(), f, t, pageable);
        } else {
            rows = sensorDataRepository.findByTimestampBetween(f, t, pageable);
        }
        return rows.stream().map(s -> mapper.toSensor(s, cfg)).toList();
    }

    private void requireDevice(String deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw ApiException.notFound("Không tìm thấy thiết bị " + deviceId + ".");
        }
    }

    private static Sort order() {
        return Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("dataId"));
    }

    private static LocalDateTime lowerBound(String from) {
        LocalDateTime f = TimeUtil.parse(from, false);
        return f == null ? TimeUtil.MIN : f;
    }

    private static LocalDateTime upperBound(String to) {
        LocalDateTime t = TimeUtil.parse(to, true);
        return t == null ? TimeUtil.MAX : t;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
