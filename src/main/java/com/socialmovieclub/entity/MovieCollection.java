package com.socialmovieclub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movie_collections")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MovieCollection extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean isPublic = true; // Varsayılan olarak herkes görebilir

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "collection_movies",
            joinColumns = @JoinColumn(name = "collection_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private Set<Movie> movies = new HashSet<>();

    // Yardımcı metodlar (DRY prensibi için)
    public void addMovie(Movie movie) {
        this.movies.add(movie);
    }

    public void removeMovie(Movie movie) {
        this.movies.remove(movie);
    }

    private String contentType; // "MOVIE" veya "TV"
}