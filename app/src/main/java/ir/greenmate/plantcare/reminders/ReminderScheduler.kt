package ir.greenmate.plantcare.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.greenmate.plantcare.data.Plant
import ir.greenmate.plantcare.data.PlantRepository
import java.util.Calendar

object ReminderScheduler {
    private const val PREFS = "greenmate_settings"
    private const val KEY_ENABLED = "reminders_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"

    fun areEnabled(context: Context): Boolean = settings(context).getBoolean(KEY_ENABLED, true)
    fun setEnabled(context: Context, enabled: Boolean) {
        settings(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) scheduleAll(context) else cancelAll(context)
    }

    fun getTime(context: Context): Pair<Int, Int> {
        val prefs = settings(context)
        return prefs.getInt(KEY_HOUR, 9) to prefs.getInt(KEY_MINUTE, 0)
    }

    fun setTime(context: Context, hour: Int, minute: Int) {
        settings(context).edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
        scheduleAll(context)
    }

    fun scheduleAll(context: Context) {
        if (!areEnabled(context)) return
        PlantRepository(context).getAll().forEach { schedule(context, it) }
    }

    fun schedule(context: Context, plant: Plant) {
        if (!areEnabled(context)) return
        val (hour, minute) = getTime(context)
        val triggerAt = nextTrigger(plant, hour, minute)
        val pendingIntent = pendingIntent(context, plant.id, plant.name)
        alarmManager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun scheduleTomorrow(context: Context, plant: Plant) {
        if (!areEnabled(context)) return
        val (hour, minute) = getTime(context)
        val trigger = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        alarmManager(context).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger,
            pendingIntent(context, plant.id, plant.name)
        )
    }

    fun cancel(context: Context, plantId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plantId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(context).cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAll(context: Context) {
        PlantRepository(context).getAll().forEach { cancel(context, it.id) }
    }

    private fun nextTrigger(plant: Plant, hour: Int, minute: Int): Long {
        val due = Calendar.getInstance().apply {
            timeInMillis = plant.lastWateredAt
            add(Calendar.DAY_OF_YEAR, plant.wateringIntervalDays)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        if (due.timeInMillis <= now + 60_000L) {
            due.add(Calendar.DAY_OF_YEAR, 1)
        }
        return due.timeInMillis
    }

    private fun pendingIntent(context: Context, id: Long, name: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_PLANT_ID, id)
            putExtra(ReminderReceiver.EXTRA_PLANT_NAME, name)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmManager(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private fun settings(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
