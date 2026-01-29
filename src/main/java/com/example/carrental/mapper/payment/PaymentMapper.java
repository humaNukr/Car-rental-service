package com.example.carrental.mapper.payment;

import com.example.carrental.config.MapperConfig;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface PaymentMapper {

    @Mapping(source = "rental.id", target = "rentalId")
    PaymentResponseDto toDto(Payment payment);
}