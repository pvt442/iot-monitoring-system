package com.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.demo.model.AlertConfig;
import com.demo.service.ConfigService;

/** Khi khởi động, đảm bảo bảng alert_config có dòng cấu hình mặc định. */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ConfigService configService;

    public DataInitializer(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AlertConfig c = configService.getConfig();
        log.info("IoT Data Server sẵn sàng. Ngưỡng: nhiệt độ > {} °C, độ ẩm > {} %, Offline sau {} giây.",
                c.getTemperatureThreshold(), c.getHumidityThreshold(), c.getOfflineTimeoutSeconds());
    }
}
