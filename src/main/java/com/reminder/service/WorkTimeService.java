package com.reminder.service;

import com.reminder.holiday.DefaultHolidays;
import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.storage.HolidayRepository;
import com.reminder.storage.StorageException;
import com.reminder.util.Hours;
import com.reminder.util.Log;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Бизнес-логика учёта рабочего времени: лимиты, итоги и агрегация недели.
 * Праздники и события хранятся в holidays.json.
 */
public class WorkTimeService {

    public static final double MAX_DAILY_HOURS = 8.0;
    public static final double MAX_WEEKLY_HOURS = 48.0;

    private List<Holiday> holidays = new ArrayList<>();
    private final HolidayRepository repository;

    public WorkTimeService(HolidayRepository repository) {
        this.repository = repository;
        loadHolidays();
    }

    // ==================== Праздники ====================

    public enum HolidayResult {
        ADDED,
        UPDATED,
        NAME_REQUIRED,
        DUPLICATE,
        NOT_FOUND
    }

    public record HolidayPreview(int added, int skipped) {
    }

    public record HolidayImportResult(int added, int skipped) {
    }

    public List<Holiday> getHolidayEntries() {
        List<Holiday> result = new ArrayList<>(holidays);
        result.sort(Comparator
                .comparing(Holiday::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(h -> h.getName() == null ? "" : h.getName(),
                        String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /** Все события конкретного дня: ежегодные по месяцу/дню, разовые по точной дате. */
    public List<Holiday> getHolidayEvents(LocalDate date) {
        if (date == null) {
            return List.of();
        }
        List<Holiday> result = holidays.stream()
                .filter(holiday -> holiday.getDate() != null)
                .filter(holiday -> holiday.isAnnual()
                        ? holiday.getDate().getMonth() == date.getMonth()
                        && holiday.getDate().getDayOfMonth() == date.getDayOfMonth()
                        : holiday.getDate().equals(date))
                .sorted(Comparator
                        .comparingInt((Holiday h) -> h.getType().ordinal())
                        .thenComparing(h -> h.getName() == null ? "" : h.getName(),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ArrayList<>(result);
    }

    public boolean isHoliday(LocalDate date) {
        return !getHolidayEvents(date).isEmpty();
    }

    public Holiday findHoliday(String id) {
        if (id == null) {
            return null;
        }
        return holidays.stream().filter(h -> id.equals(h.getId())).findFirst().orElse(null);
    }

    public HolidayResult addHoliday(LocalDate date, String name, boolean annual, HolidayType type) {
        String cleanName = name == null ? "" : name.trim();
        if (date == null || cleanName.isEmpty()) {
            return HolidayResult.NAME_REQUIRED;
        }
        Holiday candidate = new Holiday(date, cleanName, annual, type, HolidaySource.MANUAL);
        List<Holiday> next = new ArrayList<>(holidays);
        if (isDuplicate(candidate, next, null)) {
            return HolidayResult.DUPLICATE;
        }
        next.add(candidate);
        persistHolidays(next);
        holidays = next;
        return HolidayResult.ADDED;
    }

    public HolidayResult updateHoliday(Holiday updated) {
        if (updated == null || updated.getId() == null) {
            return HolidayResult.NOT_FOUND;
        }
        List<Holiday> next = new ArrayList<>(holidays);
        int idx = -1;
        for (int i = 0; i < next.size(); i++) {
            if (updated.getId().equals(next.get(i).getId())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return HolidayResult.NOT_FOUND;
        }
        Holiday existing = next.get(idx);
        String cleanName = updated.getName() == null ? "" : updated.getName().trim();
        if (updated.getDate() == null || cleanName.isEmpty()) {
            return HolidayResult.NAME_REQUIRED;
        }
        Holiday candidate = new Holiday(updated.getDate(), cleanName, updated.isAnnual(),
                updated.getType(), existing.getSource());
        candidate.setId(existing.getId());
        if (isDuplicate(candidate, next, existing.getId())) {
            return HolidayResult.DUPLICATE;
        }
        next.set(idx, candidate);
        persistHolidays(next);
        holidays = next;
        return HolidayResult.UPDATED;
    }

    public boolean deleteHoliday(String id) {
        if (id == null) {
            return false;
        }
        List<Holiday> next = new ArrayList<>(holidays);
        boolean removed = next.removeIf(h -> id.equals(h.getId()));
        if (!removed) {
            return false;
        }
        persistHolidays(next);
        holidays = next;
        return true;
    }

    /** Предпросмотр импорта: сколько добавится и сколько совпадёт с текущими/внутри файла. */
    public HolidayPreview previewImport(List<Holiday> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new HolidayPreview(0, 0);
        }
        List<Holiday> accepted = new ArrayList<>(holidays);
        int added = 0;
        int skipped = 0;
        for (Holiday candidate : candidates) {
            if (candidate == null || candidate.getDate() == null
                    || candidate.getName() == null || candidate.getName().isBlank()) {
                skipped++;
                continue;
            }
            if (isDuplicate(candidate, accepted, null)) {
                skipped++;
            } else {
                accepted.add(candidate);
                added++;
            }
        }
        return new HolidayPreview(added, skipped);
    }

    /** Добавляет праздники из файла; дубликаты пропускаются. */
    public HolidayImportResult importHolidays(List<Holiday> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new HolidayImportResult(0, 0);
        }
        List<Holiday> next = new ArrayList<>(holidays);
        int added = 0;
        int skipped = 0;
        for (Holiday candidate : candidates) {
            if (candidate == null || candidate.getDate() == null
                    || candidate.getName() == null || candidate.getName().isBlank()) {
                skipped++;
                continue;
            }
            if (isDuplicate(candidate, next, null)) {
                skipped++;
                continue;
            }
            Holiday holiday = new Holiday(candidate.getDate(), candidate.getName().trim(),
                    candidate.isAnnual(), candidate.getType(), HolidaySource.IMPORTED);
            holiday.setId(UUID.randomUUID().toString());
            next.add(holiday);
            added++;
        }
        if (added > 0) {
            persistHolidays(next);
            holidays = next;
        }
        return new HolidayImportResult(added, skipped);
    }

    private boolean isDuplicate(Holiday candidate, List<Holiday> existing, String excludeId) {
        String name = candidate.getName() == null ? "" : candidate.getName().trim();
        if (name.isEmpty() || candidate.getDate() == null) {
            return false;
        }
        for (Holiday holiday : existing) {
            if (holiday == null || holiday.getDate() == null) {
                continue;
            }
            if (excludeId != null && excludeId.equals(holiday.getId())) {
                continue;
            }
            String existingName = holiday.getName() == null ? "" : holiday.getName().trim();
            if (!name.equalsIgnoreCase(existingName)) {
                continue;
            }
            boolean sameMonthDay = holiday.getDate().getMonth() == candidate.getDate().getMonth()
                    && holiday.getDate().getDayOfMonth() == candidate.getDate().getDayOfMonth();
            if (holiday.isAnnual() && candidate.isAnnual()) {
                if (sameMonthDay) {
                    return true;
                }
            } else if (!holiday.isAnnual() && !candidate.isAnnual()) {
                if (holiday.getDate().equals(candidate.getDate())) {
                    return true;
                }
            } else if (sameMonthDay) {
                return true;
            }
        }
        return false;
    }

    private void loadHolidays() {
        if (!repository.exists()) {
            seedDefaultHolidays();
            persistHolidaysBestEffort();
            return;
        }
        holidays.addAll(repository.load());
        if (normalizeDefaultHolidays()) {
            persistHolidaysBestEffort();
        }
    }

    private void seedDefaultHolidays() {
        int year = LocalDate.now().getYear();
        for (DefaultHolidays.Entry entry : DefaultHolidays.all()) {
            holidays.add(DefaultHolidays.create(entry.day().atYear(year), entry));
        }
    }

    private boolean normalizeDefaultHolidays() {
        Map<String, Holiday> seen = new LinkedHashMap<>();
        List<Holiday> result = new ArrayList<>(holidays.size());
        boolean changed = false;
        for (Holiday holiday : holidays) {
            if (holiday == null) {
                changed = true;
                continue;
            }
            if (holiday.getDate() == null) {
                result.add(holiday);
                continue;
            }
            String name = holiday.getName() == null ? "" : holiday.getName().trim();
            DefaultHolidays.Entry entry = DefaultHolidays.find(holiday.getDate()).orElse(null);
            if (entry != null && name.isEmpty()) {
                holiday.setName(entry.name());
                holiday.setAnnual(true);
                holiday.setType(HolidayType.OFFICIAL);
                holiday.setSource(HolidaySource.DEFAULT);
                name = entry.name();
                changed = true;
            }
            if (holiday.isAnnual() && !name.isEmpty()) {
                String key = name.toLowerCase(Locale.ROOT) + "|" + MonthDay.from(holiday.getDate());
                if (seen.putIfAbsent(key, holiday) != null) {
                    changed = true;
                    continue;
                }
            }
            result.add(holiday);
        }
        if (changed) {
            holidays = result;
        }
        return changed;
    }

    private void persistHolidays(List<Holiday> next) {
        repository.save(next);
    }

    private void persistHolidaysBestEffort() {
        try {
            persistHolidays(new ArrayList<>(holidays));
        } catch (StorageException e) {
            Log.log("Holidays", "cannot save seeded/legacy holidays: " + e.getMessage(), e);
        }
    }

    // ==================== Итоги ====================

    public double getDayTotal(List<WorkEntry> entries, LocalDate date) {
        if (entries == null || date == null) return 0;
        return entries.stream()
                .filter(e -> date.equals(e.getDate()))
                .mapToDouble(WorkEntry::getHours)
                .sum();
    }

    public double getRemainingForDay(List<WorkEntry> entries, LocalDate date) {
        return MAX_DAILY_HOURS - getDayTotal(entries, date);
    }

    public double getWeekTotal(List<WeekEntry> weekEntries) {
        if (weekEntries == null) return 0;
        return weekEntries.stream().mapToDouble(WeekEntry::getTotal).sum();
    }

    public boolean wouldExceedDayLimit(List<WorkEntry> entries, LocalDate date, double additionalHours) {
        return getDayTotal(entries, date) + additionalHours > MAX_DAILY_HOURS;
    }

    // ==================== Агрегация недели ====================

    /**
     * Группирует рабочие записи недели по задаче (проект + задача + тип)
     * и собирает часы по дням в {@link WeekEntry}.
     */
    public List<WeekEntry> aggregateWeek(List<WorkEntry> weekEntries) {
        Map<String, WeekEntry> map = new LinkedHashMap<>();
        for (WorkEntry entry : weekEntries) {
            if (entry == null || entry.getDate() == null || !Hours.validEntry(entry.getHours())) {
                continue;
            }
            String key = entry.getProject() + "|" + entry.getTaskName() + "|" + entry.getType();
            WeekEntry weekEntry = map.get(key);
            if (weekEntry == null) {
                weekEntry = new WeekEntry(entry.getProject(), entry.getTaskName(), entry.getType());
                map.put(key, weekEntry);
            }
            int hours = (int) entry.getHours();
            weekEntry.getDayHours().merge(entry.getDate(), hours, Integer::sum);
            if (entry.getComment() != null && !entry.getComment().isEmpty()) {
                weekEntry.setComment(entry.getComment());
            }
        }
        return new ArrayList<>(map.values());
    }

    /** Проверка, что на день после добавления не превышен лимит 8 часов. */
    public boolean dayTotalWithinLimit(List<WeekEntry> weekEntries, LocalDate day, int newValue, int oldValue) {
        int currentDayTotal = 0;
        for (WeekEntry we : weekEntries) {
            currentDayTotal += we.getDayHours().getOrDefault(day, 0);
        }
        return currentDayTotal - oldValue + newValue <= MAX_DAILY_HOURS;
    }
}
