package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.WeeklyWinnerResponse;
import com.socialmovieclub.service.WeeklyWinnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weekly-winners")
@RequiredArgsConstructor
public class WeeklyWinnerController {

    private final WeeklyWinnerService weeklyWinnerService;

    /**
     * En son belirlenen haftalık kazananı döner.
     * Ana sayfadaki "Haftanın Yorumu" kartı için kullanılır.
     */
    @GetMapping("/last")
    public RestResponse<WeeklyWinnerResponse> getLastWinner() {
        return weeklyWinnerService.getLastWinner();
    }

    @GetMapping("/trending")
    public RestResponse<List<WeeklyWinnerResponse>> getTrendingReviews(@RequestParam(defaultValue = "4") int limit) {
        return weeklyWinnerService.getTrendingReviewsThisWeek(limit);
    }

    /**
     * Geçmiş haftaların kazananlarını sayfalı olarak döner.
     */
    @GetMapping("/hall-of-fame")
    public RestResponse<Page<WeeklyWinnerResponse>> getHallOfFame(Pageable pageable) {
        return weeklyWinnerService.getAllWinners(pageable);
    }
}