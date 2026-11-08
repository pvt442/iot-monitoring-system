package com.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.demo.dto.AlertLogResponse;
import com.demo.dto.ConfigResponse;
import com.demo.dto.DeviceInfo;
import com.demo.dto.DeviceResponse;
import com.demo.dto.SensorDataResponse;
import com.demo.model.AlertConfig;
import com.demo.model.AlertLog;
import com.demo.model.Device;
import com.demo.model.SensorData;
import com.demo.service.AlertEvaluator.Evaluation;
import com.demo.util.TimeUtil;

/** Chuyển Entity sang DTO trả về cho Frontend/Client. */
@Component
public class DtoMapper {

    private final AlertEvaluator evaluator;

    public DtoMapper(AlertEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public static double round2(Float v) {
        if (v == null) {
            return 0d;
        }
        return BigDecimal.valueOf(v.doubleValue()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static boolean isOnline(Device d, AlertConfig cfg, LocalDateTime now) {
        if (d.getLastSeen() == null) {
            return false;
        }
        long seconds = Duration.between(d.getLastSeen(), now).getSeconds();
        return seconds <= cfg.getOfflineTimeoutSeconds();
    }

    public DeviceResponse toDevice(Device d, AlertConfig cfg, LocalDateTime now) {
        boolean online = isOnline(d, cfg, now);
        return new DeviceResponse(d.getDeviceId(), d.getName(), d.getLocation(), d.getStatus(),
                TimeUtil.format(d.getLastSeen()), online, online ? "ONLINE" : "OFFLINE");
    }

    public DeviceInfo toDeviceInfo(Device d) {
        return new DeviceInfo(d.getDeviceId(), d.getName(), d.getLocation(), d.getStatus());
    }

    public SensorDataResponse toSensor(SensorData s, AlertConfig cfg) {
        double t = round2(s.getTemperature());
        double h = round2(s.getHumidity());
        Evaluation ev = evaluator.evaluate(s.getDevice().getDeviceId(), t, h, cfg);
        return toSensor(s, ev);
    }

    public SensorDataResponse toSensor(SensorData s, Evaluation ev) {
        return new SensorDataResponse(
                s.getDataId(),
                s.getDevice().getDeviceId(),
                toDeviceInfo(s.getDevice()),
                round2(s.getTemperature()),
                round2(s.getHumidity()),
                TimeUtil.format(s.getTimestamp()),
                ev.status(),
                ev.items());
    }

    public AlertLogResponse toAlert(AlertLog a) {
        return new AlertLogResponse(
                a.getId(),
                a.getDevice().getDeviceId(),
                a.getDevice().getName(),
                a.getDataId(),
                a.getMetric(),
                a.getLevel(),
                round2(a.getActualValue()),
                round2(a.getThresholdValue()),
                a.getMessage(),
                TimeUtil.format(a.getCreatedAt()),
                "BACKEND");
    }

    public ConfigResponse toConfig(AlertConfig c) {
        return new ConfigResponse(
                round2(c.getTemperatureThreshold()),
                round2(c.getHumidityThreshold()),
                round2(c.getTemperatureWarningMargin()),
                round2(c.getHumidityWarningMargin()),
                c.getOfflineTimeoutSeconds(),
                TimeUtil.format(c.getUpdatedAt()));
    }
}
