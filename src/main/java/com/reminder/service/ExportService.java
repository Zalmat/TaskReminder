package com.reminder.service;

import com.reminder.i18n.I18n;
import com.reminder.model.WorkEntry;
import com.reminder.settings.AppSettings;
import com.reminder.settings.DecimalSeparator;
import com.reminder.util.DateUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Экспорт данных. Весь текст пишется в кодировке UTF-8 (данные — Unicode). */
public class ExportService {

    /** Экспортирует записи в выбранном формате. */
    public void export(List<WorkEntry> entries, ExportFormat format, String filePath) throws IOException {
        switch (format) {
            case EXCEL -> exportToExcel(entries, filePath);
            case CSV -> exportToCsv(entries, filePath);
            case JSON -> exportToJson(entries, filePath);
            case YAML -> exportToYaml(entries, filePath);
            case XML -> exportToXml(entries, filePath);
        }
    }

    public void exportToExcel(List<WorkEntry> entries, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(I18n.get("export.sheet"));

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

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    I18n.get("column.date"), I18n.get("column.day"), I18n.get("column.project"),
                    I18n.get("column.task"), I18n.get("column.type"), I18n.get("column.hours"),
                    I18n.get("column.comment")};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (WorkEntry entry : entries) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(DateUtils.formatDate(entry.getDate()));
                row.createCell(1).setCellValue(entry.getDayOfWeek());
                row.createCell(2).setCellValue(entry.getProject());
                row.createCell(3).setCellValue(entry.getTaskName());
                row.createCell(4).setCellValue(entry.getType());
                row.createCell(5).setCellValue(entry.getHours());
                row.createCell(6).setCellValue(entry.getComment() != null ? entry.getComment() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue(I18n.get("export.total"));
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
                .create();

        JsonObject root = new JsonObject();
        root.addProperty("exportDate", DateUtils.formatDate(LocalDate.now()));
        root.addProperty("totalEntries", entries.size());
        root.addProperty("totalHours", entries.stream().mapToDouble(WorkEntry::getHours).sum());

        JsonArray entriesArray = new JsonArray();
        for (WorkEntry entry : entries) {
            JsonObject entryObj = new JsonObject();
            entryObj.addProperty("date", DateUtils.formatDate(entry.getDate()));
            entryObj.addProperty("dayOfWeek", entry.getDayOfWeek());
            entryObj.addProperty("project", entry.getProject());
            entryObj.addProperty("task", entry.getTaskName());
            entryObj.addProperty("type", entry.getType());
            entryObj.addProperty("hours", entry.getHours());
            entryObj.addProperty("comment", entry.getComment() != null ? entry.getComment() : "");
            entriesArray.add(entryObj);
        }
        root.add("entries", entriesArray);

        try (Writer writer = utf8Writer(filePath)) {
            gson.toJson(root, writer);
        }
    }

    public void exportToCsv(List<WorkEntry> entries, String filePath) throws IOException {
        DecimalSeparator separator = AppSettings.decimalSeparator();
        try (Writer w = utf8Writer(filePath);
             PrintWriter writer = new PrintWriter(w)) {
            // BOM для корректного открытия UTF-8 в Excel.
            writer.print('\uFEFF');
            writer.println(String.join(",",
                    csv(I18n.get("column.date")), csv(I18n.get("column.day")), csv(I18n.get("column.project")),
                    csv(I18n.get("column.task")), csv(I18n.get("column.type")), csv(I18n.get("column.hours")),
                    csv(I18n.get("column.comment"))));

            for (WorkEntry entry : entries) {
                writer.println(String.join(",",
                        csv(DateUtils.formatDate(entry.getDate())),
                        csv(entry.getDayOfWeek()),
                        csv(entry.getProject()),
                        csv(entry.getTaskName()),
                        csv(entry.getType()),
                        csv(separator.format(entry.getHours(), 1)),
                        csv(entry.getComment() != null ? entry.getComment() : "")
                ));
            }

            double totalHours = entries.stream().mapToDouble(WorkEntry::getHours).sum();
            writer.println();
            writer.println(I18n.get("export.total") + " " + separator.format(totalHours, 1));
        }
    }

    public void exportToYaml(List<WorkEntry> entries, String filePath) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        data.put("totalEntries", entries.size());
        data.put("totalHours", entries.stream().mapToDouble(WorkEntry::getHours).sum());

        List<Map<String, Object>> entryList = new ArrayList<>();
        for (WorkEntry entry : entries) {
            Map<String, Object> entryMap = new LinkedHashMap<>();
            entryMap.put("date", DateUtils.formatDate(entry.getDate()));
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
        try (Writer writer = utf8Writer(filePath)) {
            yaml.dump(data, writer);
        }
    }

    public void exportToXml(List<WorkEntry> entries, String filePath) throws IOException {
        try {
            DocumentBuilderFactory docFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("workReport");
            doc.appendChild(rootElement);

            Element info = doc.createElement("info");
            info.setAttribute("exportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            info.setAttribute("totalEntries", String.valueOf(entries.size()));
            info.setAttribute("totalHours", String.valueOf(entries.stream().mapToDouble(WorkEntry::getHours).sum()));
            rootElement.appendChild(info);

            Element entriesElement = doc.createElement("entries");
            for (WorkEntry entry : entries) {
                Element entryElement = doc.createElement("entry");
                entryElement.setAttribute("date", DateUtils.formatDate(entry.getDate()));
                entryElement.setAttribute("dayOfWeek", entry.getDayOfWeek());

                addXmlElement(doc, entryElement, "project", entry.getProject());
                addXmlElement(doc, entryElement, "task", entry.getTaskName());
                addXmlElement(doc, entryElement, "type", entry.getType());
                addXmlElement(doc, entryElement, "hours", String.valueOf(entry.getHours()));
                addXmlElement(doc, entryElement, "comment", entry.getComment() != null ? entry.getComment() : "");

                entriesElement.appendChild(entryElement);
            }
            rootElement.appendChild(entriesElement);

            TransformerFactory transformerFactory =
                    TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
                StreamResult result =
                        new StreamResult(writer);
                transformer.transform(source, result);
            }
        } catch (Exception e) {
            throw new IOException("Ошибка экспорта в XML: " + e.getMessage(), e);
        }
    }

    private void addXmlElement(Document doc, Element parent, String name, String value) {
        Element element = doc.createElement(name);
        element.appendChild(doc.createTextNode(value != null ? value : ""));
        parent.appendChild(element);
    }

    /** Экранирует поле по RFC 4180: всегда в кавычках, внутренние кавычки удваиваются. */
    private String csv(String value) {
        String v = value != null ? value : "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private Writer utf8Writer(String filePath) throws IOException {
        return new OutputStreamWriter(Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8);
    }
}