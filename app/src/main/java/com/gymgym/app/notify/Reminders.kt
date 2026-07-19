package com.gymgym.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.gymgym.app.MainActivity
import com.gymgym.app.R
import com.gymgym.app.settings.ReminderSettings
import java.util.concurrent.TimeUnit

/**
 * Reminder notifications via WorkManager (offline-friendly). Upcoming/missed
 * workout reminders are one-time work scheduled around the next mission's planned
 * date; the body-measurement reminder is periodic. The cycle-progress alert is
 * posted directly by the ViewModel when a pass finishes below target.
 */
object Reminders {

    const val CHANNEL_ID = "gymgym_reminders"

    /** Intent extra naming the in-app screen a tapped notification should open. */
    const val EXTRA_DESTINATION = "gymgym.destination"
    /** Destination value: the Profile screen (where body measurements are entered). */
    const val DEST_PROFILE = "profile"

    private const val WORK_UPCOMING = "reminder_upcoming"
    private const val WORK_MISSED = "reminder_missed"
    private const val WORK_BODY = "reminder_body"

    const val NOTIF_UPCOMING = 1001
    const val NOTIF_MISSED = 1002
    const val NOTIF_BODY = 1003
    const val NOTIF_CYCLE = 1004

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminders_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * Post a notification now (used for the cycle-progress alert and testing).
     * When [destination] is set, tapping the notification opens that in-app screen.
     */
    fun post(context: Context, id: Int, title: String, text: String, destination: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(context, id, destination))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    /** PendingIntent that (re)opens the app, optionally deep-linking to [destination]. */
    private fun contentIntent(context: Context, id: Int, destination: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (destination != null) putExtra(EXTRA_DESTINATION, destination)
        }
        return PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** (Re)schedule upcoming/missed/body reminders from [settings] and the next mission time. */
    fun schedule(context: Context, settings: ReminderSettings, nextMissionAt: Long?, now: Long) {
        val wm = WorkManager.getInstance(context)

        scheduleOneShot(
            wm, WORK_UPCOMING,
            enabled = settings.upcomingEnabled && nextMissionAt != null,
            fireAt = nextMissionAt?.minus(settings.upcomingHours * 3_600_000L),
            now = now,
            notifId = NOTIF_UPCOMING,
            titleRes = R.string.reminder_upcoming_title,
            textRes = R.string.reminder_upcoming_text,
            context = context,
        )
        scheduleOneShot(
            wm, WORK_MISSED,
            enabled = settings.missedEnabled && nextMissionAt != null,
            fireAt = nextMissionAt?.plus(settings.missedHours * 3_600_000L),
            now = now,
            notifId = NOTIF_MISSED,
            titleRes = R.string.reminder_missed_title,
            textRes = R.string.reminder_missed_text,
            context = context,
        )

        if (settings.bodyReminderEnabled) {
            val data = notifData(
                context, NOTIF_BODY, R.string.reminder_body_title, R.string.reminder_body_text,
                destination = DEST_PROFILE,
            )
            val work = PeriodicWorkRequestBuilder<ReminderWorker>(
                settings.bodyReminderDays.coerceAtLeast(1).toLong(), TimeUnit.DAYS,
            ).setInputData(data).build()
            wm.enqueueUniquePeriodicWork(WORK_BODY, ExistingPeriodicWorkPolicy.UPDATE, work)
        } else {
            wm.cancelUniqueWork(WORK_BODY)
        }
    }

    private fun scheduleOneShot(
        wm: WorkManager,
        name: String,
        enabled: Boolean,
        fireAt: Long?,
        now: Long,
        notifId: Int,
        titleRes: Int,
        textRes: Int,
        context: Context,
    ) {
        if (!enabled || fireAt == null) {
            wm.cancelUniqueWork(name)
            return
        }
        val delay = fireAt - now
        if (delay <= 0) {
            wm.cancelUniqueWork(name)
            return
        }
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(notifData(context, notifId, titleRes, textRes))
            .build()
        wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, work)
    }

    private fun notifData(
        context: Context,
        id: Int,
        titleRes: Int,
        textRes: Int,
        destination: String? = null,
    ): Data =
        Data.Builder()
            .putInt("id", id)
            .putString("title", context.getString(titleRes))
            .putString("text", context.getString(textRes))
            .putString("destination", destination)
            .build()
}

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getInt("id", 0)
        val title = inputData.getString("title") ?: return Result.success()
        val text = inputData.getString("text").orEmpty()
        val destination = inputData.getString("destination")
        Reminders.post(context, id, title, text, destination)
        return Result.success()
    }
}
