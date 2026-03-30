package com.socialmovieclub.service.search;

import com.socialmovieclub.dto.response.SearchResultDto;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserSearchProvider implements SearchProvider {
    private final UserRepository userRepository;

    @Override
    public String getType() { return "USER"; }

    @Override
    public List<SearchResultDto> search(String query, String lang, int limit) {
        // Pageable istemeyen yeni metodumuzu kullanıyoruz
        return userRepository.findByUsernameContainingIgnoreCase(query).stream()
                .limit(limit)
                .map(u -> SearchResultDto.builder()
                        .id(u.getId().toString())
                        .title(u.getUsername())
                        .subTitle("User")
                        .imageUrl(null)
                        .type(getType())
                        .build())
                .collect(Collectors.toList());
    }
}