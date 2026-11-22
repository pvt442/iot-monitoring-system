package com.demo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.dto.ConfigRequest;
import com.demo.exception.ApiException;
import com.demo.model.AlertConfig;
import com.demo.repository.AlertConfigRepository;

/** Đọc/ghi ngưỡng cảnh báo dùng chung (bảng alert_config). */
@Service
public class ConfigService {

    private final AlertConfigRepository repository;

    public ConfigService(AlertConfigRepository repository) {
        this.repository = repository;
    }

    /** Trả về cấu hình hiện hành; tự tạo dòng mặc định nếu chưa có. */
    @Transactional
    public AlertConfig getConfig() {
        return repository.findById(AlertConfig.SINGLETON_ID)
                .orElseGet(() -> repository.save(AlertConfig.defaults()));
    }

    @Transactional
    public AlertConfig update(ConfigRequest req) {
        AlertConfig c = getConfig();

        float tThr = pick(req.temperatureThreshold(), c.getTemperatureThreshold());
        float hThr = pick(req.humidityThreshold(), c.getHumidityThreshold());
        float tMar = pick(req.temperatureWarningMargin(), c.getTemperatureWarningMargin());
        float hMar = pick(req.humidityWarningMargin(), c.getHumidityWarningMargin());
        int timeout = req.offlineTimeoutSeconds() != null ? req.offlineTimeoutSeconds() : c.getOfflineTimeoutSeconds();

        if (tThr < -40 || tThr > 125) {
            throw ApiException.badRequest("Ngưỡng nhiệt độ phải trong khoảng -40 đến 125 °C.");
        }
        if (hThr < 0 || hThr > 100) {
            throw ApiException.badRequest("Ngưỡng độ ẩm phải trong khoảng 0 đến 100 %.");
        }
        if (tMar < 0 || tMar > 50 || hMar < 0 || hMar > 50) {
            throw ApiException.badRequest("Biên 'sắp vượt ngưỡng' phải trong khoảng 0 đến 50.");
        }
        if (timeout < 3 || timeout > 3600) {
            throw ApiException.badRequest("Thời gian timeout Offline phải trong khoảng 3 đến 3600 giây.");
        }

        c.setTemperatureThreshold(tThr);
        c.setHumidityThreshold(hThr);
        c.setTemperatureWarningMargin(tMar);
        c.setHumidityWarningMargin(hMar);
        c.setOfflineTimeoutSeconds(timeout);
        c.setUpdatedAt(LocalDateTime.now());
        return repository.save(c);
    }

    @Transactional
    public AlertConfig reset() {
        AlertConfig c = getConfig();
        c.resetToDefaults();
        return repository.save(c);
    }

    private static float pick(Double requested, Float current) {
        return requested != null ? requested.floatValue() : current;
    }
}
