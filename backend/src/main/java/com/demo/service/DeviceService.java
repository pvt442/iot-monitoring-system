package com.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.dto.DeviceRequest;
import com.demo.dto.DeviceResponse;
import com.demo.exception.ApiException;
import com.demo.model.AlertConfig;
import com.demo.model.Device;
import com.demo.repository.AlertLogRepository;
import com.demo.repository.DeviceRepository;
import com.demo.repository.SensorDataRepository;

@Service
public class DeviceService {

    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{1,50}$");

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final AlertLogRepository alertLogRepository;
    private final ConfigService configService;
    private final DtoMapper mapper;

    public DeviceService(DeviceRepository deviceRepository, SensorDataRepository sensorDataRepository,
                         AlertLogRepository alertLogRepository, ConfigService configService, DtoMapper mapper) {
        this.deviceRepository = deviceRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.alertLogRepository = alertLogRepository;
        this.configService = configService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        AlertConfig cfg = configService.getConfig();
        LocalDateTime now = LocalDateTime.now();
        return deviceRepository.findAllByOrderByDeviceIdAsc().stream()
                .map(d -> mapper.toDevice(d, cfg, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(String deviceId) {
        Device d = deviceRepository.findById(deviceId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thiết bị " + deviceId + "."));
        return mapper.toDevice(d, configService.getConfig(), LocalDateTime.now());
    }

    public record SaveResult(DeviceResponse device, boolean created) {
    }

    /** Thêm mới hoặc cập nhật thiết bị (upsert theo deviceId). Trường null thì giữ giá trị cũ. */
    @Transactional
    public SaveResult saveDevice(DeviceRequest req) {
        String id = req.deviceId() == null ? "" : req.deviceId().trim();
        if (!ID_PATTERN.matcher(id).matches()) {
            throw ApiException.badRequest(
                    "deviceId bắt buộc, tối đa 50 ký tự, chỉ gồm chữ, số và các ký tự _ - . (ví dụ DEV_01).");
        }
        if (req.name() != null && req.name().length() > 100) {
            throw ApiException.badRequest("Tên thiết bị tối đa 100 ký tự.");
        }
        if (req.location() != null && req.location().length() > 100) {
            throw ApiException.badRequest("Vị trí tối đa 100 ký tự.");
        }
        String status = req.status() == null || req.status().isBlank() ? null : req.status().trim().toUpperCase();
        if (status != null && !status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw ApiException.badRequest("status chỉ nhận ACTIVE hoặc INACTIVE.");
        }

        Device device = deviceRepository.findById(id).orElse(null);
        boolean created = device == null;
        if (created) {
            device = new Device();
            device.setDeviceId(id);
            device.setStatus("ACTIVE");
        }
        if (req.name() != null) {
            device.setName(req.name().trim());
        }
        if (req.location() != null) {
            device.setLocation(req.location().trim());
        }
        if (status != null) {
            device.setStatus(status);
        }
        Device saved = deviceRepository.save(device);
        return new SaveResult(mapper.toDevice(saved, configService.getConfig(), LocalDateTime.now()), created);
    }

    /** Xóa thiết bị cùng toàn bộ dữ liệu cảm biến và nhật ký cảnh báo liên quan. */
    @Transactional
    public void deleteDevice(String deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw ApiException.notFound("Không tìm thấy thiết bị " + deviceId + ".");
        }
        alertLogRepository.deleteByDevice_DeviceId(deviceId);
        sensorDataRepository.deleteByDevice_DeviceId(deviceId);
        deviceRepository.deleteById(deviceId);
    }
}
