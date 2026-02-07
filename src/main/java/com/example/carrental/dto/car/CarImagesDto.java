package com.example.carrental.dto.car;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CarImagesDto {
    private Long carId;
    private List<String> imageUrls;
}