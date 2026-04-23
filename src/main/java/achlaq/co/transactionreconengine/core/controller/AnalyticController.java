package achlaq.co.transactionreconengine.core.controller;

import achlaq.co.transactionreconengine.common.projection.HighValueUserProjection;
import achlaq.co.transactionreconengine.core.service.AnalyticService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticController {

    private final AnalyticService analyticsService;

    @GetMapping("/high-value-users")
    public List<HighValueUserProjection> getTopSpenders() {
        return analyticsService.getHighValueUsers();
    }
}