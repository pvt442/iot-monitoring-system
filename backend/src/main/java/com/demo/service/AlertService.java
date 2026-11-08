package com.demo.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.dto.AlertItem;
import com.demo.dto.AlertLogResponse;
import com.demo.dto.PageResponse;
import com.demo.model.AlertLog;
import com.demo.model.Device;
import com.demo.model.SensorData;
import com.demo.repository.AlertLogRepository;
import com.demo.service.AlertEvaluator.Evaluation;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertLogRepository alertLogRepository;
    private final DtoMapper mapper;

    public AlertService(AlertLogRepository alertLogRepository, DtoMapper mapper) {
        this.alertLogRepository = alertLogRepository;
        this.mapper = mapper;
    }

    /**
     * Ghi nhật ký khi một chỉ số CHUYỂN từ không-vượt-ngưỡng sang vượt ngưỡng.
     * Nếu bản ghi trước đó đã vượt ngưỡng cùng chỉ số thì không ghi lại (chống spam hàng nghìn dòng).
     */
    @Transactional
    public void recordTransitions(Device device, SensorData saved, Evaluation previous, Evaluation current) {
        for (AlertItem item : current.items()) {
            if (!AlertEvaluator.CRITICAL.equals(item.level())) {
                continue;
            }
            boolean alreadyAlerting = previous != null && previous.isCritical(item.metric());
            if (alreadyAlerting) {
                continue;
            }
            AlertLog entry = new AlertLog();
            entry.setDevice(device);
            entry.setDataId(saved.getDataId());
            entry.setMetric(item.metric());
            entry.setLevel(item.level());
            entry.setActualValue((float) item.value());
            entry.setThresholdValue((float) item.threshold());
            entry.setMessage(item.message());
            entry.setCreatedAt(LocalDateTime.now());
            alertLogRepository.save(entry);
            log.warn("⚠ {}", item.message());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertLogResponse> search(String deviceId, String metric, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        boolean hasDevice = deviceId != null && !deviceId.isBlank();
        String m = metric == null || metric.isBlank() ? null : metric.trim().toUpperCase();

        Page<AlertLog> result;
        if (hasDevice && m != null) {
            result = alertLogRepository.findByDevice_DeviceIdAndMetric(deviceId.trim(), m, pageable);
        } else if (hasDevice) {
            result = alertLogRepository.findByDevice_DeviceId(deviceId.trim(), pageable);
        } else if (m != null) {
            result = alertLogRepository.findByMetric(m, pageable);
        } else {
            result = alertLogRepository.findAll(pageable);
        }
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toAlert).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
