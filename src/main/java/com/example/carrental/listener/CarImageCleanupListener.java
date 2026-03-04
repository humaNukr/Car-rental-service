package com.example.carrental.listener;

import com.example.carrental.event.CarDeletedEvent;
import com.example.carrental.service.impl.CarImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CarImageCleanupListener {
    private final CarImageService imageService;

    @Async("fileTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCarDeleted(CarDeletedEvent event) {
        log.info("Transaction committed. Starting file cleanup for car {}", event.carId());
        imageService.deleteFolder(event.carId());
    }
}
