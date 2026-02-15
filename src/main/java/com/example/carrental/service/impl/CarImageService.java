package com.example.carrental.service.impl;

import com.example.carrental.properties.FileProperties;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.CarImage;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.file.FileStorageException;
import com.example.carrental.repository.CarImageRepository;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.service.abstracts.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class CarImageService extends ImageService {

    private final FileProperties fileProperties;
    private final CarRepository carRepository;
    private final CarImageRepository carImageRepository;

    public CarImageService(FileProperties fileProperties,
                           CarRepository carRepository,
                           CarImageRepository carImageRepository
    ) {
        super(fileProperties.getImagesUploadDir());
        this.fileProperties = fileProperties;
        this.carRepository = carRepository;
        this.carImageRepository = carImageRepository;
    }

    @Transactional
    public void uploadImages(Long id, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileStorageException("No files provided");
        }

        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));

        boolean hasMain = car.getImages().stream().anyMatch(CarImage::isMain);

        String subFolder = fileProperties.getSubdirs().getCar() + "/car-" + id;

        for (MultipartFile file : files) {
            String fileName = super.uploadImage(file, subFolder);

            CarImage image = new CarImage();
            image.setCar(car);
            image.setImageUrl("/images/" + subFolder + "/" + fileName);

            if (!hasMain) {
                image.setMain(true);
                hasMain = true;
            }

            car.getImages().add(image);
        }
        carRepository.save(car);
    }

    public List<String> getImagesPaths(Long id) {
        List<String> diskPaths = super.getImagesPaths(fileProperties.getSubdirs().getCar() + "/car-" + id);

        String mainImageUrl = carImageRepository.findByCarIdAndIsMainTrue(id)
                .map(CarImage::getImageUrl)
                .orElse(null);

        if (mainImageUrl == null) {
            return diskPaths;
        }

        return diskPaths.stream()
                .sorted((url1, url2) -> {
                    if (url1.equals(mainImageUrl)) return -1;
                    if (url2.equals(mainImageUrl)) return 1;
                    return url1.compareTo(url2);
                })
                .toList();
    }

    @Transactional
    public void setMainImage(Long carId, String imageUrl) {
        CarImage newMain = carImageRepository.findByImageUrl(imageUrl)
                .orElseThrow(() -> new EntityNotFoundException("CarImage not found with imageUrl: " + imageUrl));

        if (!newMain.getCar().getId().equals(carId)) {
            throw new IllegalArgumentException("Image belongs to another car");
        }

        carImageRepository.findByCarIdAndIsMainTrue(carId)
                .ifPresent(oldMain -> {
                    oldMain.setMain(false);
                    carImageRepository.save(oldMain);
                });

        newMain.setMain(true);
        carImageRepository.save(newMain);
    }

    @Transactional
    public void deleteImages(Long carId, List<String> imageUrls) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + carId));
        for (String imageUrl : imageUrls) {
            super.deleteImage(fileProperties.getBaseDir(), imageUrl);

            CarImage carImage = carImageRepository.findByImageUrl(imageUrl)
                    .orElseThrow(() -> new EntityNotFoundException("CarImage not found with imageUrl: " + imageUrl));
            car.getImages().remove(carImage);
        }
    }

    public void deleteFolder(Long carId) {
        carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + carId));

        super.deleteFolder(fileProperties.getSubdirs().getCar() + "/car-" + carId);
    }

}
