package com.dorm.internetbot.states;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserState {

    private Map<Long, BotState> stateMap = new HashMap<>();

    public Map<Long, BotState> getStateMap() {
        return stateMap;
    }

    public void setStateMap(Long chatId, BotState state) {
        stateMap.put(chatId, state);
    }
}
