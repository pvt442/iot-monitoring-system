package com.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.dto.ConfigRequest;
import com.demo.dto.ConfigResponse;
import com.demo.service.ConfigService;
import com.demo.service.DtoMapper;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;
    private final DtoMapper mapper;

    public ConfigController(ConfigService configService, DtoMapper mapper) {
        this.configService = configService;
        this.mapper = mapper;
    }

    @GetMapping
    public ConfigResponse get() {
        return mapper.toConfig(configService.getConfig());
    }

    @PutMapping
    public ConfigResponse update(@RequestBody ConfigRequest request) {
        return mapper.toConfig(configService.update(request));
    }

    /** Khôi phục ngưỡng mặc định: 35 °C, 80 %, biên 3 °C / 5 %, timeout 15 giây. */
    @PostMapping("/reset")
    public ConfigResponse reset() {
        return mapper.toConfig(configService.reset());
    }
}
