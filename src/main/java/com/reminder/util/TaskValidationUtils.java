package com.reminder.util;

import java.util.List;
import java.util.stream.Collectors;

public final class TaskValidationUtils {
    public static final String VISUAL_EMPTY = "   ";

    private TaskValidationUtils(){
        //Просто конструктор без которого не будет работать
    }
    //Возвращаем тект задачи, если он пустой, возвращаем пустоту (зрительно красивее)
    public static String getSafeTaskName(String rawInput){
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return VISUAL_EMPTY;
        }
        return rawInput.trim();
    }

    public static List<String> filterTaskNames(List<String> taskNames){
        if (taskNames == null) return List.of();
        return taskNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());

    }
}
