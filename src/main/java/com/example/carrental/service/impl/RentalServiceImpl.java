package com.example.carrental.service.impl;

import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalReturnRequestDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Location;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.RentalStatus;
import com.example.carrental.event.RentalCreatedEvent;
import com.example.carrental.event.RentalReturnedLateEvent;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.rental.RentalAlreadyFinishedException;
import com.example.carrental.mapper.rental.RentalMapper;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.security.SecurityFacade;
import com.example.carrental.service.interfaces.CarService;
import com.example.carrental.service.interfaces.LocationService;
import com.example.carrental.service.interfaces.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityFacade securityFacade;
    private final CarService carService;
    private final LocationService locationService;

    @Override
    @Transactional
    public RentalResponseDto createRental(RentalRequestDto requestDto) {
        User user = securityFacade.getCurrentUser();

        Car car = carService.getAvailableCarForRental(requestDto.getCarId());

        Rental rental = rentalMapper.toEntity(requestDto, car, user);

        Location pickupLocation = locationService.getLocationById(requestDto.getPickupLocationId());
        Location dropOffLocation = locationService.getLocationById(requestDto.getDropOffLocationId());

        rental.setStatus(RentalStatus.PENDING);
        rental.setPickupLocation(pickupLocation);
        rental.setDropOffLocation(dropOffLocation);
        Rental savedRental = rentalRepository.save(rental);

        carService.markAsRented(car.getId());
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

        return rentalMapper.toDto(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalResponseDto> getMyRentals(Pageable pageable) {
        User user = securityFacade.getCurrentUser();

        return rentalRepository.findAllByUserId(user.getId(), pageable).stream()
                .map(rentalMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RentalResponseDto returnCar(Long rentalId, RentalReturnRequestDto dto) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found"));

        if (rental.getActualReturnDate() != null) {
            throw new RentalAlreadyFinishedException("Rental is already finished");
        }

        rental.setActualReturnDate(LocalDate.now());
        rental.setStatus(RentalStatus.COMPLETED);

        Location returnLocation = (dto != null && dto.getActualLocationId() != null)
                ? locationService.getLocationById(dto.getActualLocationId())
                : rental.getDropOffLocation();

        carService.markAsReturned(rental.getCar().getId(), returnLocation);

        if (rental.getActualReturnDate().isAfter(rental.getReturnDate())) {
            long lateDays = ChronoUnit.DAYS.between(rental.getReturnDate(), rental.getActualReturnDate());
            if (lateDays > 0) {
                eventPublisher.publishEvent(new RentalReturnedLateEvent(
                        rental.getId(),
                        lateDays,
                        rental.getCar().getDailyFee()
                ));
            }
        }

        return rentalMapper.toDto(rental);
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
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found with id: " + id));
        return rentalMapper.toDto(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalResponseDto getMyRentalById(Long id) {
        User currentUser = securityFacade.getCurrentUser();

        Rental rental = rentalRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental not found or access denied"));
        return rentalMapper.toDto(rental);
    }
}
