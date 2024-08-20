package com.dorm.internetbot.telegram;

import com.dorm.internetbot.config.BotConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class InternetBot extends TelegramLongPollingBot {

    private final String ID_OUR_CHANNEL = "-1002208721735";
    private final String OK = "OK";

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private SendMessage sendMessage;

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getChat().getUserName();

            switch (message){
                case "/start":
                    start(update.getMessage().getChat().getFirstName(), chatId);
                    break;
                default:
                    sendAnswer(chatId, OK);
                    redirect(username, message);
            }
        }

    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    private void sendAnswer(Long chatId, String message){
        //String message = "OK";
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e){

        }
    }

    private void start(String username, Long chatId){
        String answer = "Hello, " + username + "!";
        sendAnswer(chatId, answer);
    }

    private void redirect(String username, String message){
        String answer = "@" + username + "\n" + message;
        sendMessage.setChatId(ID_OUR_CHANNEL);
        sendMessage.setText(answer);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e){

        }
    }




}
