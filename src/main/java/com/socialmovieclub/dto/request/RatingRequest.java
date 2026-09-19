package com.socialmovieclub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RatingRequest {

//    @NotNull(message = "{movie.id.required}")
    private UUID movieId;

    private Long tmdbId;

    @NotNull(message = "{rating.score.required}")
    @Min(value = 1, message = "{rating.score.invalid}")
    @Max(value = 10, message = "{rating.score.invalid}")
    private Integer score;

    @NotBlank(message = "{movie.contentType.required}")
    private String contentType; // "MOVIE" veya "TV"
}

//todo bu konuya bakilacak.
/*
Örneğin Controller:
@RequestParam(
    required = false,
    defaultValue = "MOVIE"
) String contentType
alıyor.
Ama RatingRequest zaten:
@NotBlank
private String contentType;
içeriyor.
Yani şu anda iki farklı contentType kaynağın var.
Bu ileride:
query parameter → MOVIE
body            → TV
olursa hangisi doğru?
Tek source of truth seçmeliyiz.
uhtemelen request body yeterli:
{
  "tmdbId": 123,
  "score": 9,
  "contentType": "TV"
}
ve Service:
request.getContentType()
kullanabilir.

 */
