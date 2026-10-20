package com.reminder.catalog;

import java.util.ArrayList;
import java.util.List;

/** DTO для хранения каталога подстановок в JSON. */
public class TaskCatalogData {

    public List<String> projects = new ArrayList<>();
    public List<String> taskNames = new ArrayList<>();
    public List<String> types = new ArrayList<>();
}
