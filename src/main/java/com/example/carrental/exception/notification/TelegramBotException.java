package com.example.carrental.exception.notification;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBotException extends RuntimeException {
    public TelegramBotException(String message, TelegramApiException e) {
        super(message, e);
    }
}
