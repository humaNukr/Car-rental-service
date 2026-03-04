package com.example.carrental.service.impl;

import com.example.carrental.entity.Car;
import com.example.carrental.entity.CarImage;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.file.FileStorageException;
import com.example.carrental.properties.FileProperties;
import com.example.carrental.repository.CarImageRepository;
import com.example.carrental.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarImageServiceTest {

    @TempDir
    Path tempDir;
    @Mock
    private FileProperties fileProperties;
    @Mock
    private CarRepository carRepository;
    @Mock
    private CarImageRepository carImageRepository;
    @Mock
    private FileProperties.Subdirs subdirs;
    private CarImageService service;

    @BeforeEach
    void setUp() {
        lenient().when(fileProperties.getImagesUploadDir()).thenReturn(tempDir.toString());
        lenient().when(fileProperties.getSubdirs()).thenReturn(subdirs);
        lenient().when(subdirs.getCar()).thenReturn("cars");

        service = new CarImageService(fileProperties, carRepository, carImageRepository);
    }

    @Nested
    @DisplayName("uploadImages()")
    class UploadImages {

        @Test
        @DisplayName("Success: Should set first image as Main")
        void shouldSetFirstImageAsMain() throws IOException {
            Long carId = 1L;
            Car car = new Car();
            car.setId(carId);
            car.setImages(new ArrayList<>());

            MultipartFile file1 = mock(MultipartFile.class);
            MultipartFile file2 = mock(MultipartFile.class);

            when(file1.getOriginalFilename()).thenReturn("photo1.jpg");
            when(file2.getOriginalFilename()).thenReturn("photo2.jpg");
            when(file1.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
            when(file2.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

            when(carRepository.findById(carId)).thenReturn(Optional.of(car));

            service.uploadImages(carId, List.of(file1, file2));

            verify(carRepository).save(car);

            assertThat(car.getImages()).hasSize(2);
            assertThat(car.getImages().get(0).isMain()).isTrue();
            assertThat(car.getImages().get(1).isMain()).isFalse();

            Path carDir = tempDir.resolve("cars/car-1");
            assertThat(Files.exists(carDir)).isTrue();
            try (var stream = Files.list(carDir)) {
                assertThat(stream.count()).isEqualTo(2);
            }
        }

        @Test
        @DisplayName("Success: Should NOT set Main if exists")
        void shouldNotSetMainIfExists() throws IOException {
            Long carId = 1L;
            Car car = new Car();
            car.setId(carId);

            CarImage existing = new CarImage();
            existing.setMain(true);
            car.setImages(new ArrayList<>(List.of(existing)));

            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("new.jpg");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

            when(carRepository.findById(carId)).thenReturn(Optional.of(car));

            service.uploadImages(carId, List.of(file));

            assertThat(car.getImages()).hasSize(2);
            assertThat(car.getImages().get(1).isMain()).isFalse();
        }

        @Test
        @DisplayName("Fail: Should throw Exception when files list is empty")
        void shouldThrowWhenNoFiles() {
            assertThatThrownBy(() -> service.uploadImages(1L, List.of()))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessage("No files provided");
        }
    }

    @Nested
    @DisplayName("getImagesPaths()")
    class GetImagesPaths {

        @Test
        @DisplayName("Success: Should sort Main image to the top")
        void shouldSortMainImageFirst() throws IOException {
            Long carId = 10L;

            Path carDir = tempDir.resolve("cars/car-" + carId);
            Files.createDirectories(carDir);
            Files.createFile(carDir.resolve("A.jpg"));
            Files.createFile(carDir.resolve("B_main.jpg"));
            Files.createFile(carDir.resolve("C.jpg"));

            String mainUrl = "/images/cars/car-10/B_main.jpg";

            CarImage dbImage = new CarImage();
            dbImage.setImageUrl(mainUrl);
            when(carImageRepository.findByCarIdAndIsMainTrue(carId))
                    .thenReturn(Optional.of(dbImage));

            List<String> result = service.getImagesPaths(carId);

            assertThat(result).hasSize(3);
            assertThat(result.getFirst()).isEqualTo(mainUrl);
            assertThat(result).contains("/images/cars/car-10/A.jpg", "/images/cars/car-10/C.jpg");
        }

        @Test
        @DisplayName("Success: Should return unsorted if no Main in DB")
        void shouldReturnUnsortedIfNoMain() throws IOException {
            Long carId = 11L;
            Path carDir = tempDir.resolve("cars/car-" + carId);
            Files.createDirectories(carDir);
            Files.createFile(carDir.resolve("X.jpg"));

            when(carImageRepository.findByCarIdAndIsMainTrue(carId)).thenReturn(Optional.empty());

            List<String> result = service.getImagesPaths(carId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).endsWith("X.jpg");
        }
    }

    @Nested
    @DisplayName("setMainImage()")
    class SetMainImage {
        @Test
        void shouldSwapMainImage() {
            Long carId = 5L;
            String newMainUrl = "new.jpg";

            Car car = new Car();
            car.setId(carId);

            CarImage newImage = new CarImage();
            newImage.setCar(car);
            newImage.setImageUrl(newMainUrl);
            newImage.setMain(false);

            CarImage oldImage = new CarImage();
            oldImage.setCar(car);
            oldImage.setMain(true);

            when(carImageRepository.findByImageUrl(newMainUrl)).thenReturn(Optional.of(newImage));
            when(carImageRepository.findByCarIdAndIsMainTrue(carId)).thenReturn(Optional.of(oldImage));

            service.setMainImage(carId, newMainUrl);

            assertThat(oldImage.isMain()).isFalse();
            verify(carImageRepository).save(oldImage);

            assertThat(newImage.isMain()).isTrue();
            verify(carImageRepository).save(newImage);
        }
    }

    @Nested
    @DisplayName("deleteImages()")
    class DeleteImages {

        @Test
        @DisplayName("Success: Should delete physical file and remove from Entity list")
        void shouldDeleteFileAndEntity() throws IOException {
            Long carId = 1L;
            Car car = new Car();
            car.setId(carId);
            car.setImages(new ArrayList<>());

            String imageUrl = "/images/cars/car-1/delete_me.jpg";
            CarImage carImage = new CarImage();
            carImage.setId(100L);
            carImage.setImageUrl(imageUrl);
            carImage.setCar(car);

            car.getImages().add(carImage);

            lenient().when(fileProperties.getBaseDir()).thenReturn(tempDir.toString());

            String relativePath = imageUrl.substring(1);
            Path filePath = tempDir.resolve(relativePath);

            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);

            when(carRepository.findById(carId)).thenReturn(Optional.of(car));
            when(carImageRepository.findByImageUrl(imageUrl)).thenReturn(Optional.of(carImage));

            service.deleteImages(carId, List.of(imageUrl));

            assertThat(Files.exists(filePath)).isFalse();

            assertThat(car.getImages()).isEmpty();
        }

        @Test
        @DisplayName("Fail: Should throw if Car not found")
        void shouldThrowIfCarNotFound() {
            when(carRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteImages(99L, List.of("img.jpg")))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Car not found");
        }

        @Test
        @DisplayName("Fail: Should throw if Image Entity not found")
        void shouldThrowIfImageEntityNotFound() throws IOException {
            String imageUrl = "/images/cars/car-1/ghost.jpg";

            lenient().when(fileProperties.getBaseDir()).thenReturn(tempDir.toString());
            String relativePath = imageUrl.substring(1);
            Path filePath = tempDir.resolve(relativePath);
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);


            Car car = new Car();
            Long carId = 1L;

            when(carRepository.findById(carId)).thenReturn(Optional.of(car));
            when(carImageRepository.findByImageUrl(imageUrl)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteImages(carId, List.of(imageUrl)))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("CarImage not found");
        }
    }

    @Nested
    @DisplayName("deleteFolder()")
    class DeleteFolder {

        @Test
        @DisplayName("Success: Should recursively delete car folder")
        void shouldDeleteFolderRecursively() throws IOException {
            Long carId = 5L;
            String carSubFolder = "cars";

            Path carDir = tempDir.resolve(carSubFolder).resolve("car-" + carId);
            Files.createDirectories(carDir);

            Files.createFile(carDir.resolve("photo1.jpg"));
            Files.createFile(carDir.resolve("photo2.jpg"));

            service.deleteFolder(carId);

            assertThat(Files.exists(carDir)).isFalse();
        }

        @Test
        @DisplayName("Fail: Should throw FileStorageException if directory missing")
        void shouldThrowIfDirMissing() {
            Long carId = 5L;
            assertThatThrownBy(() -> service.deleteFolder(carId))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Directory not found");
        }
    }
}