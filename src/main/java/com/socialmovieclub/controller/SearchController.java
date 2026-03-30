package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.GlobalSearchResponse;
import com.socialmovieclub.service.search.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final GlobalSearchService searchService;

    @GetMapping("/global")
    public RestResponse<GlobalSearchResponse> globalSearch(
            @RequestParam String query,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return searchService.executeSearch(query, lang);
    }
}