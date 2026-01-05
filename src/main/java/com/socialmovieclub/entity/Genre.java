package com.socialmovieclub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "genres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Genre extends BaseEntity{

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    // İlişki: Bir kategorinin birden fazla dilde (TR, EN) karşılığı olabilir.
    @OneToMany(mappedBy = "genre", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GenreTranslation> translations = new HashSet<>();

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @ManyToMany(mappedBy = "genres")
    private Set<Movie> movies = new HashSet<>();


}
