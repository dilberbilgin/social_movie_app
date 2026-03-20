package com.socialmovieclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomPageResponse<T> implements Serializable {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private boolean last;

    // Spring Page'i bizim basit DTO'muza çeviren yardımcı metot
    public static <T> CustomPageResponse<T> of(Page<T> page) {
        return new CustomPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.isLast()
        );
    }
}