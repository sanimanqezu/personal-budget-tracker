package com.sanele.budgettracker.controller;

import com.sanele.budgettracker.dto.CategorySummaryResponse;
import com.sanele.budgettracker.dto.MonthlySummaryResponse;
import com.sanele.budgettracker.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @RequestParam String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                summaryService.getMonthlySummary(userDetails.getUsername(), month));
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<CategorySummaryResponse>> getCategorySummary(
            @RequestParam String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                summaryService.getCategorySummary(userDetails.getUsername(), month));
    }
}
