package com.example.carrental.service.impl;

import com.example.carrental.notification.CarRentalBot;
import com.example.carrental.service.interfaces.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramNotificationServiceImpl implements TelegramNotificationService {

    private final CarRentalBot carRentalBot;

    @Value("${bot.admin-chat-id}")
    private Long adminChatId;

    @Async
    @Override
    public void sendNotification(String message) {
        carRentalBot.sendMessage(adminChatId, message);
    }
}
