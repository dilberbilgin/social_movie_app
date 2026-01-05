package com.socialmovieclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "languages")
@Getter
@Setter
public class Language extends BaseEntity {
    @Column(unique = true, nullable = false, length = 5)
    private String code; // tr, en

    @Column(nullable = false)
    private String name; // Turkish, English
}