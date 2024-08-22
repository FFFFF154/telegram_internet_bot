package com.dorm.internetbot.config;

import com.dorm.internetbot.states.BotState;
import com.dorm.internetbot.states.UserState;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

@Configuration
@Data
@PropertySource(value = "classpath:application.yml")
public class BotConfig {

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String token;

    @Bean
    public SendMessage sendMessage() {
        return new SendMessage();
    }

    @Bean
    public SendPhoto sendPhoto() {
        return new SendPhoto();
    }

    @Bean
    public ForwardMessage forwardMessage(){
        return new ForwardMessage();
    }

    @Bean
    public BotState botState (){
        return BotState.DEFAULT;
    }

    @Bean
    public UserState userState (){
        return new UserState();
    }

}
