package com.socialmovieclub.service.search;

import com.socialmovieclub.dto.response.SearchResultDto;

import java.util.List;

public interface SearchProvider {

    String getType();

    List<SearchResultDto> search(String query, String lang, int limit);
}