package com.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.demo.model.AlertConfig;
import com.demo.service.AlertEvaluator;
import com.demo.service.AlertEvaluator.Evaluation;

/** Kiểm thử thuần logic (không cần MySQL). */
class AlertEvaluatorTest {

    private final AlertEvaluator evaluator = new AlertEvaluator();
    private final AlertConfig cfg = AlertConfig.defaults(); // 35 °C, 80 %, biên 3 / 5

    @Test
    void normalValues() {
        Evaluation e = evaluator.evaluate("DEV_01", 25, 60, cfg);
        assertEquals("NORMAL", e.status());
        assertTrue(e.items().isEmpty());
    }

    @Test
    void temperatureExactlyAtThresholdIsNotCritical() {
        Evaluation e = evaluator.evaluate("DEV_01", 35, 60, cfg);
        assertEquals("WARNING", e.status());
        assertFalse(e.isCritical("TEMPERATURE"));
    }

    @Test
    void temperatureAboveThresholdIsCritical() {
        Evaluation e = evaluator.evaluate("DEV_01", 35.1, 60, cfg);
        assertEquals("CRITICAL", e.status());
        assertTrue(e.isCritical("TEMPERATURE"));
    }

    @Test
    void humidityAboveThresholdIsCritical() {
        Evaluation e = evaluator.evaluate("DEV_01", 25, 80.5, cfg);
        assertEquals("CRITICAL", e.status());
        assertTrue(e.isCritical("HUMIDITY"));
        assertFalse(e.isCritical("TEMPERATURE"));
    }

    @Test
    void warningZoneBeforeThreshold() {
        Evaluation e = evaluator.evaluate("DEV_01", 33, 76, cfg);
        assertEquals("WARNING", e.status());
        assertEquals(2, e.items().size());
    }

    @Test
    void bothCriticalProducesTwoItems() {
        Evaluation e = evaluator.evaluate("DEV_01", 38.2, 85, cfg);
        assertEquals("CRITICAL", e.status());
        assertEquals(2, e.items().size());
    }
}
