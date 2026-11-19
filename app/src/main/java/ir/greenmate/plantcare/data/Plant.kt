package ir.greenmate.plantcare.data

import java.util.Calendar
import java.util.concurrent.TimeUnit

data class Plant(
    val id: Long,
    val name: String,
    val species: String,
    val location: String,
    val wateringIntervalDays: Int,
    val lastWateredAt: Long,
    val emoji: String = "🪴",
    val notes: String = ""
) {
    fun dueAt(): Long = lastWateredAt + TimeUnit.DAYS.toMillis(wateringIntervalDays.toLong())

    fun daysUntilDue(now: Long = System.currentTimeMillis()): Int {
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dueDay = Calendar.getInstance().apply {
            timeInMillis = dueAt()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return ((dueDay.timeInMillis - today.timeInMillis) / TimeUnit.DAYS.toMillis(1)).toInt()
    }

    fun isDue(now: Long = System.currentTimeMillis()): Boolean = daysUntilDue(now) <= 0
}
