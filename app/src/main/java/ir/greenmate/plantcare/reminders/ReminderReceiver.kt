package ir.greenmate.plantcare.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.greenmate.plantcare.data.PlantRepository
import ir.greenmate.plantcare.ui.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderScheduler.areEnabled(context)) return
        val plantId = intent.getLongExtra(EXTRA_PLANT_ID, -1L)
        val plant = PlantRepository(context).find(plantId) ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "یادآوری آبیاری", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "یادآوری زمان رسیدگی به گیاهان"
                }
            )
        }
        val openIntent = PendingIntent.getActivity(
            context,
            plantId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
        val notification = notificationBuilder.setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle("وقت رسیدگی به ${plant.name} است 🌿")
            .setContentText("خاک گیاه را بررسی کن؛ اگر خشک است آبیاری‌اش کن.")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(plantId.toInt(), notification)
        } catch (_: SecurityException) {
            // Android 13+ notification permission may have been denied.
        }
        ReminderScheduler.scheduleTomorrow(context, plant)
    }

    companion object {
        const val EXTRA_PLANT_ID = "plant_id"
        const val EXTRA_PLANT_NAME = "plant_name"
        const val CHANNEL_ID = "watering_reminders"
    }
}
