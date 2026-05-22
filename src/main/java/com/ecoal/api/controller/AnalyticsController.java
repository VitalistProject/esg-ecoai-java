package com.ecoal.api.controller;

import com.ecoal.api.dto.analytics.DashboardResponse;
import com.ecoal.api.dto.analytics.DimensionProgressResponse;
import com.ecoal.api.dto.analytics.ScoreResponse;
import com.ecoal.api.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/dimensions")
    public ResponseEntity<DimensionProgressResponse> dimensions(
            @RequestParam(required = false, defaultValue = "monthly") String period) {
        return ResponseEntity.ok(analyticsService.dimensions(period));
    }

    @GetMapping("/score")
    public ResponseEntity<ScoreResponse> score() {
        return ResponseEntity.ok(analyticsService.score());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(analyticsService.dashboard());
    }
}
