package com.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.demo.dto.AlertItem;
import com.demo.model.AlertConfig;

/**
 * Thuật toán đánh giá cảnh báo cho một lần đo.
 *   value >  threshold                    -> CRITICAL (vượt ngưỡng)
 *   value >= threshold - warningMargin    -> WARNING  (sắp vượt ngưỡng)
 *   còn lại                               -> NORMAL
 * Trạng thái tổng = mức cao nhất trong các chỉ số (nhiệt độ, độ ẩm).
 */
@Component
public class AlertEvaluator {

    public static final String METRIC_TEMPERATURE = "TEMPERATURE";
    public static final String METRIC_HUMIDITY = "HUMIDITY";
    public static final String NORMAL = "NORMAL";
    public static final String WARNING = "WARNING";
    public static final String CRITICAL = "CRITICAL";

    public record Evaluation(String status, List<AlertItem> items) {

        /** Có cảnh báo mức CRITICAL cho chỉ số này không? */
        public boolean isCritical(String metric) {
            for (AlertItem i : items) {
                if (i.metric().equals(metric) && CRITICAL.equals(i.level())) {
                    return true;
                }
            }
            return false;
        }
    }

    public Evaluation evaluate(String deviceId, double temperature, double humidity, AlertConfig cfg) {
        List<AlertItem> items = new ArrayList<>();

        double tThr = cfg.getTemperatureThreshold();
        double tMargin = cfg.getTemperatureWarningMargin();
        if (temperature > tThr) {
            items.add(new AlertItem(METRIC_TEMPERATURE, CRITICAL, temperature, tThr,
                    "Cảnh báo nhiệt độ – " + deviceId + " đang ở mức " + num(temperature, 1)
                            + " °C, vượt ngưỡng " + num(tThr, 1) + " °C."));
        } else if (tMargin > 0 && temperature >= tThr - tMargin) {
            items.add(new AlertItem(METRIC_TEMPERATURE, WARNING, temperature, tThr,
                    "Nhiệt độ " + deviceId + " đang ở mức " + num(temperature, 1)
                            + " °C, sắp chạm ngưỡng " + num(tThr, 1) + " °C."));
        }

        double hThr = cfg.getHumidityThreshold();
        double hMargin = cfg.getHumidityWarningMargin();
        if (humidity > hThr) {
            items.add(new AlertItem(METRIC_HUMIDITY, CRITICAL, humidity, hThr,
                    "Cảnh báo độ ẩm – " + deviceId + " đang ở mức " + num(humidity, 1)
                            + " %, vượt ngưỡng " + num(hThr, 1) + " %."));
        } else if (hMargin > 0 && humidity >= hThr - hMargin) {
            items.add(new AlertItem(METRIC_HUMIDITY, WARNING, humidity, hThr,
                    "Độ ẩm " + deviceId + " đang ở mức " + num(humidity, 1)
                            + " %, sắp chạm ngưỡng " + num(hThr, 1) + " %."));
        }

        String status = NORMAL;
        for (AlertItem i : items) {
            if (CRITICAL.equals(i.level())) {
                status = CRITICAL;
                break;
            }
            status = WARNING;
        }
        return new Evaluation(status, items);
    }

    /** Làm tròn và bỏ số 0 thừa: 38.20 -> "38.2", 35.0 -> "35". */
    static String num(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
