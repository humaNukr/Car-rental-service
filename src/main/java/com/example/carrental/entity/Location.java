package com.example.carrental.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "locations")
@Getter
@Setter
@SQLDelete(sql = "UPDATE locations SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted=false")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String workHours;

    @Column(nullable = false)
    private String email;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<String> phones;

    @Column(nullable = false)
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    @Column(nullable = false)
    private boolean isDeleted = false;

}
