package com.reminder.services;

import com.reminder.models.WorkEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ExportService {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void exportToExcel(List<WorkEntry> entries, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Рабочее время");

            // Создаем стиль для заголовков
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Дата", "День недели", "Проект", "Задача", "Тип", "Часы", "Комментарий"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Заполняем данные
            int rowNum = 1;
            for (WorkEntry entry : entries) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getDate().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(entry.getDayOfWeek());
                row.createCell(2).setCellValue(entry.getProject());
                row.createCell(3).setCellValue(entry.getTaskName());
                row.createCell(4).setCellValue(entry.getType());
                row.createCell(5).setCellValue(entry.getHours());
                row.createCell(6).setCellValue(entry.getComment() != null ? entry.getComment() : "");
            }

            // Автоширина колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            // Создаем итоговую строку
            Row totalRow = sheet.createRow(rowNum + 1);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Итого часов:");
            totalLabel.setCellStyle(headerStyle);

            double totalHours = entries.stream().mapToDouble(WorkEntry::getHours).sum();
            Cell totalHoursCell = totalRow.createCell(5);
            totalHoursCell.setCellValue(totalHours);
            totalHoursCell.setCellStyle(headerStyle);

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    public void exportToJson(List<WorkEntry> entries, String filePath) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class,
                        (com.google.gson.JsonSerializer<LocalDate>)
                                (src, typeOfSrc, context) ->
                                        context.serialize(src.format(DATE_FORMATTER)))
                .create();

        JsonObject root = new JsonObject();
        root.addProperty("exportDate", LocalDate.now().format(DATE_FORMATTER));
        root.addProperty("totalEntries", entries.size());
        root.addProperty("totalHours", entries.stream().mapToDouble(WorkEntry::getHours).sum());

        JsonArray entriesArray = new JsonArray();
        for (WorkEntry entry : entries) {
            JsonObject entryObj = new JsonObject();
            entryObj.addProperty("date", entry.getDate().format(DATE_FORMATTER));
            entryObj.addProperty("dayOfWeek", entry.getDayOfWeek());
            entryObj.addProperty("project", entry.getProject());
            entryObj.addProperty("task", entry.getTaskName());
            entryObj.addProperty("type", entry.getType());
            entryObj.addProperty("hours", entry.getHours());
            entryObj.addProperty("comment", entry.getComment() != null ? entry.getComment() : "");
            entriesArray.add(entryObj);
        }
        root.add("entries", entriesArray);

        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(root, writer);
        }
    }

    public void exportToCsv(List<WorkEntry> entries, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Дата,День недели,Проект,Задача,Тип,Часы,Комментарий");

            for (WorkEntry entry : entries) {
                writer.printf("%s,%s,%s,%s,%s,%.1f,%s%n",
                        entry.getDate().format(DATE_FORMATTER),
                        entry.getDayOfWeek(),
                        escapeCsv(entry.getProject()),
                        escapeCsv(entry.getTaskName()),
                        escapeCsv(entry.getType()),
                        entry.getHours(),
                        escapeCsv(entry.getComment() != null ? entry.getComment() : "")
                );
            }

            double totalHours = entries.stream().mapToDouble(WorkEntry::getHours).sum();
            writer.printf("%nИтого часов: %.1f%n", totalHours);
        }
    }

    // Новый метод для экспорта в YAML
    public void exportToYaml(List<WorkEntry> entries, String filePath) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        data.put("totalEntries", entries.size());
        data.put("totalHours", entries.stream().mapToDouble(WorkEntry::getHours).sum());

        List<Map<String, Object>> entryList = new ArrayList<>();
        for (WorkEntry entry : entries) {
            Map<String, Object> entryMap = new LinkedHashMap<>();
            entryMap.put("date", entry.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            entryMap.put("dayOfWeek", entry.getDayOfWeek());
            entryMap.put("project", entry.getProject());
            entryMap.put("task", entry.getTaskName());
            entryMap.put("type", entry.getType());
            entryMap.put("hours", entry.getHours());
            entryMap.put("comment", entry.getComment() != null ? entry.getComment() : "");
            entryList.add(entryMap);
        }
        data.put("entries", entryList);

        Yaml yaml = new Yaml();
        try (Writer writer = new FileWriter(filePath)) {
            yaml.dump(data, writer);
        }
    }

    // Новый метод для экспорта в XML
    public void exportToXml(List<WorkEntry> entries, String filePath) throws IOException {
        try {
            javax.xml.parsers.DocumentBuilderFactory docFactory =
                    javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.newDocument();

            org.w3c.dom.Element rootElement = doc.createElement("workReport");
            doc.appendChild(rootElement);

            org.w3c.dom.Element info = doc.createElement("info");
            info.setAttribute("exportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            info.setAttribute("totalEntries", String.valueOf(entries.size()));
            info.setAttribute("totalHours", String.valueOf(entries.stream().mapToDouble(WorkEntry::getHours).sum()));
            rootElement.appendChild(info);

            org.w3c.dom.Element entriesElement = doc.createElement("entries");
            for (WorkEntry entry : entries) {
                org.w3c.dom.Element entryElement = doc.createElement("entry");
                entryElement.setAttribute("date", entry.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                entryElement.setAttribute("dayOfWeek", entry.getDayOfWeek());

                addXmlElement(doc, entryElement, "project", entry.getProject());
                addXmlElement(doc, entryElement, "task", entry.getTaskName());
                addXmlElement(doc, entryElement, "type", entry.getType());
                addXmlElement(doc, entryElement, "hours", String.valueOf(entry.getHours()));
                addXmlElement(doc, entryElement, "comment", entry.getComment() != null ? entry.getComment() : "");

                entriesElement.appendChild(entryElement);
            }
            rootElement.appendChild(entriesElement);

            javax.xml.transform.TransformerFactory transformerFactory =
                    javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
            javax.xml.transform.stream.StreamResult result =
                    new javax.xml.transform.stream.StreamResult(new File(filePath));
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new IOException("Ошибка экспорта в XML: " + e.getMessage(), e);
        }
    }

    // Вспомогательный метод для XML
    private void addXmlElement(org.w3c.dom.Document doc, org.w3c.dom.Element parent, String name, String value) {
        org.w3c.dom.Element element = doc.createElement(name);
        element.appendChild(doc.createTextNode(value != null ? value : ""));
        parent.appendChild(element);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}