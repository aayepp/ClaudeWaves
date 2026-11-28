package com.lastwave.app.data.update

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lastwave.app.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.TimeUnit

/**
 * Announces a new GitHub release in the notification shade, so sideloaded installs
 * hear about updates without opening the app. GitHub cannot push to devices, so this
 * polls. No boot receiver (see AndroidManifest): after a reboot the schedule resumes
 * the next time the app starts.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun appUpdateManager(): AppUpdateManager
        fun broadcastManager(): BroadcastManager
        fun appStatsManager(): AppStatsManager
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val manager = deps.appUpdateManager()
        val version = try {
            manager.versionToAnnounce()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            null // Offline or GitHub hiccup: the next period tries again.
        }
        if (version != null) {
            notify(version)
            // Tapping the notification opens the app; the dialog is waiting there.
            manager.showDialog()
        }

        val broadcasts = deps.broadcastManager()
        val announcement = try {
            broadcasts.messageToNotify()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            null
        }
        if (announcement != null) {
            notifyBroadcast(announcement)
            broadcasts.showDialog()
        }
        if (broadcasts.hasOwnerToken()) {
            runCatching { deps.appStatsManager().checkNewDeveloperFeedbackToNotify() }
                .getOrNull()
                ?.let(::notifyNewFeedback)
        }
        runCatching { deps.appStatsManager().sendHeartbeatIfDue() }
        return Result.success()
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled() covers the runtime grant.
    private fun notify(version: String) {
        val context = applicationContext
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        // The vivo build ships under another package, so never hardcode it.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val open = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wave)
            .setContentTitle(context.getString(R.string.update_available))
            .setContentText(context.getString(R.string.update_ready_to_install, version))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        notifications.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled() covers the runtime grant.
    private fun notifyBroadcast(message: BroadcastMessage) {
        val context = applicationContext
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(BROADCAST_CHANNEL_ID, context.getString(R.string.broadcast_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val open = PendingIntent.getActivity(context, 1, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val body = message.message.ifBlank { message.type.label }
        val notification = NotificationCompat.Builder(context, BROADCAST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wave)
            .setContentTitle("${message.type.emoji} ${message.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        notifications.notify(BROADCAST_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun notifyNewFeedback(alert: NewFeedbackAlert) {
        val context = applicationContext
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(INBOX_ALERT_CHANNEL_ID, "Inbox Alerts", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val open = PendingIntent.getActivity(context, 2, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val catLabel = when (alert.latestCategory) {
            FeedbackCategory.BUG -> "Bug"
            FeedbackCategory.SUGGESTION -> "Cadangan"
            FeedbackCategory.FEEDBACK -> "Feedback"
        }
        val title = if (alert.newCount > 1) {
            "${alert.newCount} laporan baru ($catLabel)"
        } else {
            "Laporan baru: $catLabel"
        }
        val notification = NotificationCompat.Builder(context, INBOX_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wave)
            .setContentTitle(title)
            .setContentText(alert.latestTitle)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        notifications.notify(INBOX_ALERT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 4210
        private const val BROADCAST_CHANNEL_ID = "app_broadcasts"
        private const val BROADCAST_NOTIFICATION_ID = 4211
        private const val INBOX_ALERT_CHANNEL_ID = "dev_inbox_alerts"
        private const val INBOX_ALERT_NOTIFICATION_ID = 4212
        // Renamed with the period: KEEP would leave existing installs on the old 12 h
        // schedule, and WorkManager 2.7 has no UPDATE policy.
        private const val WORK_NAME = "app_update_check_hourly"
        private const val LEGACY_WORK_NAME = "app_update_check"

        /** Idempotent: KEEP leaves an existing schedule and its timing alone. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(LEGACY_WORK_NAME)
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
