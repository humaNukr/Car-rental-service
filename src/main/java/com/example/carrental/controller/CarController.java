package com.example.carrental.controller;

import com.example.carrental.dto.car.CarDetailsDto;
import com.example.carrental.dto.car.CarImagesDto;
import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.dto.car.CarUpdateRequestDto;
import com.example.carrental.mapper.car.CarMapper;
import com.example.carrental.service.impl.CarImageService;
import com.example.carrental.service.interfaces.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;
    private final CarImageService imageService;

    @GetMapping
    public List<CarResponseDto> getAll(
            @ModelAttribute CarSearchParameters searchParameters,
            Pageable pageable
    ) {
        return carService.getAll(searchParameters, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public CarResponseDto createCar(@RequestBody @Valid CarRequestDto carRequestDto) {
        return carService.save(carRequestDto);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public CarResponseDto updateCar(@PathVariable Long id, @RequestBody @Valid CarUpdateRequestDto carRequestDto) {
        return carService.update(id, carRequestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void delete(@PathVariable Long id) {
        carService.delete(id);
    }


    @GetMapping("/{id}/images")
    public CarImagesDto getCarImages(@PathVariable Long id) {
        List<String> imageUrls = imageService.getImagesPaths(id);
        return new CarImagesDto(id, imageUrls);
    }

    @GetMapping("/{id}/details")
    public CarDetailsDto getCarDetails(@PathVariable Long id) {
        return carService.getDetails(id);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> uploadCarImages(
            @PathVariable Long id,
            @RequestPart("file") List<MultipartFile> files
    ) {
        carService.uploadImages(id, files);
        return ResponseEntity.ok().build();
    }

    @PutMapping("{id}/images/main")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> setMainImage(
            @PathVariable Long id,
            @RequestParam String imageUrl
    ) {
        carService.setMainImage(id, imageUrl);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{id}/images")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteImages(@PathVariable Long id, @RequestParam List<String> imageUrls) {
        carService.deleteImages(id, imageUrls);
        return ResponseEntity.ok().build();
    }


}
