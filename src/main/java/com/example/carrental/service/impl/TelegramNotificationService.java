package com.example.carrental.service.impl;

import com.example.carrental.notification.CarRentalBot;
import com.example.carrental.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramNotificationService implements NotificationService {

    private final CarRentalBot carRentalBot;

    @Value("${bot.admin-chat-id}")
    private Long adminChatId;

    @Override
    public void sendNotification(String message) {
        carRentalBot.sendMessage(adminChatId, message);
    }
}
