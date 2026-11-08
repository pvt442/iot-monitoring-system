package com.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.dto.DeviceOverview;
import com.demo.dto.OverviewResponse;
import com.demo.dto.SensorDataResponse;
import com.demo.model.AlertConfig;
import com.demo.model.Device;
import com.demo.repository.DeviceRepository;
import com.demo.repository.SensorDataRepository;
import com.demo.util.TimeUtil;

/** Tổng hợp dữ liệu cho Dashboard trong một lần gọi API. */
@Service
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final ConfigService configService;
    private final DtoMapper mapper;

    public DashboardService(DeviceRepository deviceRepository, SensorDataRepository sensorDataRepository,
                            ConfigService configService, DtoMapper mapper) {
        this.deviceRepository = deviceRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.configService = configService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview() {
        AlertConfig cfg = configService.getConfig();
        LocalDateTime now = LocalDateTime.now();

        List<DeviceOverview> rows = new ArrayList<>();
        int online = 0, alerting = 0, warning = 0;

        for (Device d : deviceRepository.findAllByOrderByDeviceIdAsc()) {
            boolean isOnline = DtoMapper.isOnline(d, cfg, now);
            SensorDataResponse latest = sensorDataRepository
                    .findFirstByDevice_DeviceIdOrderByTimestampDescDataIdDesc(d.getDeviceId())
                    .map(s -> mapper.toSensor(s, cfg))
                    .orElse(null);
            String alertStatus = latest == null ? AlertEvaluator.NORMAL : latest.alertStatus();

            if (isOnline) {
                online++;
                if (AlertEvaluator.CRITICAL.equals(alertStatus)) {
                    alerting++;
                } else if (AlertEvaluator.WARNING.equals(alertStatus)) {
                    warning++;
                }
            }
            rows.add(new DeviceOverview(d.getDeviceId(), d.getName(), d.getLocation(), d.getStatus(),
                    TimeUtil.format(d.getLastSeen()), isOnline, isOnline ? "ONLINE" : "OFFLINE",
                    alertStatus, latest));
        }

        int total = rows.size();
        return new OverviewResponse(
                TimeUtil.format(now),
                new OverviewResponse.Summary(total, online, total - online, alerting, warning),
                mapper.toConfig(cfg),
                rows);
    }
}
