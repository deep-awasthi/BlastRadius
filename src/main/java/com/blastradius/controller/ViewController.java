package com.blastradius.controller;

import com.blastradius.dto.DashboardDto;
import com.blastradius.dto.ScanResponse;
import com.blastradius.service.DashboardService;
import com.blastradius.service.ScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Thymeleaf view controller for serving HTML pages.
 */
@Controller
public class ViewController {

    private static final Logger log = LoggerFactory.getLogger(ViewController.class);

    private final ScanService scanService;
    private final DashboardService dashboardService;

    public ViewController(ScanService scanService, DashboardService dashboardService) {
        this.scanService = scanService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @RequestParam(required = false) Long scanId) {
        List<ScanResponse> scans = scanService.getAllScans();
        model.addAttribute("scans", scans);

        // Try to load dashboard for given scanId or latest completed scan
        if (scanId == null && !scans.isEmpty()) {
            scans.stream()
                 .filter(s -> "COMPLETED".equals(s.getStatus()))
                 .findFirst()
                 .ifPresent(s -> {
                     try {
                         DashboardDto dashboard = dashboardService.buildDashboard(s.getId());
                         model.addAttribute("dashboard", dashboard);
                         model.addAttribute("activeScanId", s.getId());
                     } catch (Exception e) {
                         log.warn("Could not load dashboard for scan {}", s.getId());
                     }
                 });
        } else if (scanId != null) {
            try {
                DashboardDto dashboard = dashboardService.buildDashboard(scanId);
                model.addAttribute("dashboard", dashboard);
                model.addAttribute("activeScanId", scanId);
            } catch (Exception e) {
                log.warn("Could not load dashboard for scan {}", scanId);
            }
        }

        return "dashboard";
    }

    @GetMapping("/scanner")
    public String scanner(Model model) {
        model.addAttribute("scans", scanService.getAllScans());
        return "scanner";
    }

    @GetMapping("/graph")
    public String graph(Model model, @RequestParam(required = false) Long scanId) {
        List<ScanResponse> scans = scanService.getAllScans();
        model.addAttribute("scans", scans);
        if (scanId != null) {
            model.addAttribute("activeScanId", scanId);
        } else {
            scans.stream()
                 .filter(s -> "COMPLETED".equals(s.getStatus()))
                 .findFirst()
                 .ifPresent(s -> model.addAttribute("activeScanId", s.getId()));
        }
        return "graph";
    }

    @GetMapping("/reports")
    public String reports(Model model, @RequestParam(required = false) Long scanId) {
        List<ScanResponse> scans = scanService.getAllScans();
        model.addAttribute("scans", scans);
        if (scanId != null) {
            model.addAttribute("activeScanId", scanId);
        } else {
            scans.stream()
                 .filter(s -> "COMPLETED".equals(s.getStatus()))
                 .findFirst()
                 .ifPresent(s -> model.addAttribute("activeScanId", s.getId()));
        }
        return "reports";
    }
}
