package com.example.carrental.service.impl;

import com.example.carrental.properties.RentalProperties;
import com.example.carrental.dto.payment.CreateFineDto;
import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.enums.RentalStatus;
import com.example.carrental.event.RentalCreatedEvent;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.exception.rental.RentalAlreadyFinishedException;
import com.example.carrental.mapper.rental.RentalMapper;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.service.interfaces.PaymentService;
import com.example.carrental.service.interfaces.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final RentalMapper rentalMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RentalProperties rentalProperties;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public RentalResponseDto createRental(RentalRequestDto requestDto) {
        User user = getCurrentUser();

        Car car = carRepository.findById(requestDto.getCarId())
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new CarUnavailableException("Car is not available for rental");
        }

        Rental rental = rentalMapper.toEntity(requestDto, car, user);
        rental.setStatus(RentalStatus.PENDING);

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);

        Rental savedRental = rentalRepository.save(rental);
        eventPublisher.publishEvent(new RentalCreatedEvent(savedRental.getId()));
        return rentalMapper.toDto(savedRental);
    }

    @Override
    @Transactional
    public RentalResponseDto updateRental(Long id, RentalUpdateRequestDto requestDto) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found"));

        if (rental.getActualReturnDate() != null) {
            throw new RentalAlreadyFinishedException("Cannot update a finished rental");
        }

        rentalMapper.updateRentalFromDto(requestDto, rental);

        return rentalMapper.toDto(rentalRepository.save(rental));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalResponseDto> getMyRentals(Pageable pageable) {
        User user = getCurrentUser();

        return rentalRepository.findAllByUserId(user.getId(), pageable).stream()
                .map(rentalMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RentalResponseDto returnCar(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found"));

        if (rental.getActualReturnDate() != null) {
            throw new RentalAlreadyFinishedException("Rental is already finished");
        }

        rental.setActualReturnDate(LocalDate.now());
        rental.setStatus(RentalStatus.COMPLETED);

        Car car = rental.getCar();
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);

        if (rental.getActualReturnDate().isAfter(rental.getReturnDate())) {
            long lateDays = ChronoUnit.DAYS.between(rental.getReturnDate(), rental.getActualReturnDate());
            if (lateDays > 0) {
                BigDecimal dailyFee = rental.getCar().getDailyFee();
                BigDecimal multiplier = BigDecimal.valueOf(rentalProperties.getFine().getLateReturnMultiplier());
                BigDecimal fineAmount = dailyFee.multiply(BigDecimal.valueOf(lateDays)).multiply(multiplier);

                CreateFineDto autoFine = new CreateFineDto(fineAmount, "LATE_RETURN");
                paymentService.createFine(rental.getId(), autoFine);
            }
        }

        return rentalMapper.toDto(rentalRepository.save(rental));

    }

    @Override
    public List<RentalResponseDto> getAllActive(Pageable pageable) {
        return rentalRepository.findAllByActualReturnDateIsNull(pageable).stream()
                .map(rentalMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RentalResponseDto getRentalById(Long id) {
        Rental rental =  rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found with id: " + id));
        return rentalMapper.toDto(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalResponseDto getMyRentalById(Long id) {
        User currentUser = getCurrentUser();

        Rental rental = rentalRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental not found or access denied"));
        return rentalMapper.toDto(rental);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
