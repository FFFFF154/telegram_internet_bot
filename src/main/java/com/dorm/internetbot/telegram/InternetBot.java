package com.dorm.internetbot.telegram;

import com.dorm.internetbot.config.BotConfig;
import com.dorm.internetbot.states.BotState;
import com.dorm.internetbot.states.UserState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class InternetBot extends TelegramLongPollingBot {

    private static final String ID_OUR_CHANNEL = "-1002208721735";
    private static final String ID_SPAM_CHANNEL = "-1002150155712";
    private static final String COMMAND_ERROR = "Неверная команда";

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private SendMessage sendMessage;

    @Autowired
    private SendPhoto sendPhoto;
    @Autowired
    private ForwardMessage forwardMessage;

    @Autowired
    private UserState userState;

    @Autowired
    private SendDocument sendDocument;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && !update.getMessage().hasPhoto()) {
            String message = update.getMessage().getText();
            Integer messageId = update.getMessage().getMessageId();
            Long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getChat().getUserName();

            if (userState.getStateMap().get(chatId) == null) { // мегоговно
                userState.setStateMap(chatId, BotState.DEFAULT);
            }

            if (userState.getStateMap().get(chatId).equals(BotState.WAIT_MESSAGE)) {
                if (message.equals("/stop")) {
                    userState.setStateMap(chatId, BotState.DEFAULT);
                } else {
                    if (checkMessage(message)) {
                        redirect(username, messageId, chatId);
                        sendAnswer(chatId, "OK\nМы с Вами свяжемся\n");
                        userState.setStateMap(chatId, BotState.DEFAULT);
                    } else {
                        sendAnswer(chatId, "Пожалуйста, введите корректный запрос\n" +
                                "Например: 312(а) У меня проблемы с интернетом\n" +
                                "Иначе введите /stop\n" +
                                "\n! Проверьте, что все пользователи могут ссылаться на Ваш аккаунт в тг!");
                        spam(messageId, chatId);
                    }
                }


            } else if (message.startsWith("/")) {
                //String command = message.substring(0, message.indexOf(" "));
                message = message.toLowerCase();
                switch (message) {
                    case "/start":
                        start(update.getMessage().getChat().getFirstName(), chatId);
                        break;
                    case "/help":
                        help(chatId);
                        break;
                    case "/contact_us":
                        userState.setStateMap(chatId, BotState.WAIT_MESSAGE);
                        sendAnswer(chatId, "Опишите свою проблему, " +
                                "начиная с номера комнаты\n" +
                                "Формат сообщения: 312(а) У меня проблемы с подключением интернета\n" +
                                "\n! Проверьте, что все пользователи могут ссылаться на Ваш аккаунт в тг!");
                        break;
                    case "/guide":
                        sendGuides(chatId);
                        break;
                    default:
                        sendAnswer(chatId, COMMAND_ERROR);
                        spam(messageId, chatId);
                }

            } else {
                sendAnswer(chatId, "Пожалуйста, отправьте команду");
                spam(messageId, chatId);
            }

        } else if (update.getMessage().hasPhoto()) { // мегахуйня
            Long chatId = update.getMessage().getChatId();
            sendAnswer(chatId, "Пожалуйста, не отправляйте фото");
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

    private void sendAnswer(Long chatId, String message) {
        //String message = "OK";
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(message);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }

    private void start(String username, Long chatId) {
        String answer = "Привет, " + username + "!\n" +
                "Это helpInternetBot\n" +
                "Ты можешь использовать эти команды:\n" +
                "/help - список команд\n" +
                "/contact_us - связаться с админами\n" +
                "/guide - гайд по подключению интернета";
        sendAnswer(chatId, answer);
    }

    private void redirect(String username, Integer messageId, Long chatId) {
        forwardMessage.setChatId(ID_OUR_CHANNEL);
        String answer = "----------------------\n" +
                "@" + username + "\n";
        sendMessage.setChatId(ID_OUR_CHANNEL);
        sendMessage.setText(answer);
        forwardMessage.setFromChatId(chatId);
        forwardMessage.setMessageId(messageId);

        try {
            execute(sendMessage);
            execute(forwardMessage);

        } catch (TelegramApiException e) {

        }
    }

    private void spam(Integer messageId, Long chatId) {
        forwardMessage.setChatId(ID_SPAM_CHANNEL);
        forwardMessage.setFromChatId(chatId);
        forwardMessage.setMessageId(messageId);
        try {
            execute(forwardMessage);
        } catch (TelegramApiException e) {

        }
    }

    private void help(Long chatId) {
        String answer = "/help - список команд\n" +
                "/contact_us - связаться с админами\n" +
                "/guide - гайд по подключению интернета";
        sendMessage.setChatId(chatId);
        sendMessage.setText(answer);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }

    public boolean checkMessage(String message) {
        if (!message.contains("(")) { // говно
            try {
                int roomNumber = Integer.parseInt(message.substring(0, message.indexOf(" ")));
                return (roomNumber % 100 == 6) || (roomNumber % 100 == 10)
                        || (roomNumber % 100 == 15) && (roomNumber / 100 <= 15)
                        && (roomNumber / 100 >= 3);
            } catch (Exception e) {
                return false;
            }

        } else if ((message.indexOf("(") == 3) || (message.indexOf("(") == 4)) {
            try {
                int roomNumber = Integer.parseInt(message.substring(0, message.indexOf("(")));
                if (roomNumber % 100 == 6 || roomNumber % 100 == 10 || roomNumber % 100 == 15) {
                    return false;
                } else {
                    return ((roomNumber >= 201 && roomNumber <= 1515)
                            && (message.charAt(message.indexOf("(") + 1) == 'а'
                            || message.charAt(message.indexOf("(") + 1) == 'б'
                            || message.charAt(message.indexOf("(") + 1) == 'a'
                            || message.charAt(message.indexOf("(") + 1) == 'b')
                            && (roomNumber % 200 > 0)
                            && (roomNumber % 100 <= 15));
                }

            } catch (Exception e) {
                return false;
            }

        }


        return false;
    }

    private void sendGuides(Long chatId) {
        sendDocument.setChatId(chatId);

        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream resource = classLoader.getResourceAsStream("гайд_по_подключению.pdf")) {
            Path tempFile = Files.createTempFile("guide_inet_", ".pdf");
            Files.copy(resource, tempFile, StandardCopyOption.REPLACE_EXISTING);
            File file = tempFile.toFile();
            sendDocument.setDocument(new InputFile(file));
            execute(sendDocument);
            Files.deleteIfExists(tempFile);
        } catch (TelegramApiException e) {
            log.warn("Проблема с telegram api");
        } catch (IOException e) {
            log.warn("Проблема с чтением файла");
        }

//        ClassLoader classLoader2 = getClass().getClassLoader();
//        try (InputStream resource = classLoader2.getResourceAsStream("gde_vzyat_parol_i_login_dlya_podklyuchenia.pdf")) {
//            Path tempFile = Files.createTempFile("password_", ".pdf");
//            Files.copy(resource, tempFile, StandardCopyOption.REPLACE_EXISTING);
//            File file = tempFile.toFile();
//            sendDocument.setDocument(new InputFile(file));
//            execute(sendDocument);
//            Files.deleteIfExists(tempFile);
//        } catch (TelegramApiException e) {
//            log.warn("Проблема с telegram api");
//        } catch (IOException e) {
//            log.warn("Проблема с чтением файла");
//        }

    }


}
