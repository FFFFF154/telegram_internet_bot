package com.dorm.internetbot.telegram;

import com.dorm.internetbot.config.BotConfig;
import com.dorm.internetbot.states.BotState;
import com.dorm.internetbot.states.UserState;
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
import java.net.URL;

@Component
public class InternetBot extends TelegramLongPollingBot {

    private static final String ID_OUR_CHANNEL = "-1002208721735";
    private static final String ID_SPAM_CHANNEL = "-1002150155712";
    private static final String OK = "OK";
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
                if (checkMessage(message)) {
                    redirect(username, messageId, chatId);
                    sendAnswer(chatId, OK);
                    userState.setStateMap(chatId, BotState.DEFAULT);
                } else {
                    sendAnswer(chatId, "Пожалуйста, введите корректный запрос " +
                            "Например: 312(а) У меня проблемы с интернетом");
                    spam(messageId, chatId);
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
                                "Например: 312(а) У меня проблемы с подключением интернета");
                        break;
                    case "/guides":
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
                "/guides - гайды по подключению интернета";
        sendAnswer(chatId, answer);
    }

    private void redirect(String username, Integer messageId, Long chatId) {
        //String answer = "@" + username + "\n";
        forwardMessage.setChatId(ID_OUR_CHANNEL);
        forwardMessage.setFromChatId(chatId);
        forwardMessage.setMessageId(messageId);

        try {
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
                "/guides - гайды по подключению интернета";
        sendMessage.setChatId(chatId);
        sendMessage.setText(answer);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }

    private boolean checkMessage(String message) {
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
                return ((roomNumber >= 301 && roomNumber <= 1515)
                        && (message.charAt(message.indexOf("(") + 1) == 'а'
                        || message.charAt(message.indexOf("(") + 1) == 'б'
                        || message.charAt(message.indexOf("(") + 1) == 'a'
                        || message.charAt(message.indexOf("(") + 1) == 'b')
                        && (roomNumber % 100 > 0)
                        && (roomNumber % 100 <= 15));
            } catch (Exception e) {
                return false;
            }

        }


        return false;
    }

    private void sendGuides(Long chatId) {
        sendDocument.setChatId(chatId);

        ClassLoader classLoader=getClass().getClassLoader();
        URL resource = classLoader.getResource("Kak_podklyuchit_INET.pdf");

        sendDocument.setDocument(new InputFile(new File(resource.getFile())));
        //sendDocument.setDocument(new InputFile("C:\\Users\\dns\\Documents\\java\\internetBot\\src\\main\\resources\\gde_vzyat_parol_i_login_dlya_podklyuchenia.pdf"));

        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
        }
        resource = classLoader.getResource("gde_vzyat_parol_i_login_dlya_podklyuchenia.pdf");
        sendDocument.setDocument(new InputFile(new File(resource.getFile())));
        try{
            execute(sendDocument);
        } catch (TelegramApiException e){

        }
    }


}
