package com.example.carrental.notification;

import com.example.carrental.exception.notification.TelegramBotException;
import com.example.carrental.properties.BotProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class CarRentalBot extends TelegramLongPollingBot {

    private final BotProperties botProperties;

    public CarRentalBot(BotProperties botProperties) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                log.info("New user started bot. Chat ID: {}", chatId);
                sendMessage(chatId, "Hello! Your Chat ID is: " + chatId + "\nCopy this to your application.yaml!");
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new TelegramBotException("Can't send Telegram message", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }
}
