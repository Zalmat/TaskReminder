package com.reminder.importing;

import com.reminder.util.DailySchedule;
import com.reminder.util.Log;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Ежедневный запуск задачи импорта в указанное время. */
public class ImportScheduler {

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;

    /** Включает/выключает ежедневный запуск (пересчитывает расписание). */
    public void applySchedule(boolean enabled, int hour, int minute, Runnable job) {
        stop();
        if (!enabled || job == null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "task-import");
            thread.setDaemon(true);
            return thread;
        });
        long delay = DailySchedule.initialDelayMillis(LocalDateTime.now(), hour, minute);
        long period = Duration.ofDays(1).toMillis();
        task = scheduler.scheduleAtFixedRate(() -> {
            try {
                job.run();
            } catch (Throwable t) {
                Log.log("TaskImport", "scheduled import failed: " + t.getMessage(), t);
            }
        }, delay, period, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void shutdown() {
        stop();
    }
}
