package com.ciclismo.portugal.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ciclismo.portugal.MainActivity
import com.ciclismo.portugal.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_NEW_PROVAS = "new_provas"
        const val CHANNEL_REMINDERS = "reminders"
        private const val NOTIFICATION_ID_NEW_PROVA = 1
        private const val NOTIFICATION_ID_REMINDER = 2
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelNewProvas = NotificationChannel(
                CHANNEL_NEW_PROVAS,
                "Novas Provas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações de novas provas disponíveis"
            }

            val channelReminders = NotificationChannel(
                CHANNEL_REMINDERS,
                "Lembretes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes de provas no calendário"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channelNewProvas)
            notificationManager.createNotificationChannel(channelReminders)
        }
    }

    fun showNewProvaNotification(provaName: String, provaCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_PROVAS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nova prova disponível!")
            .setContentText("$provaCount novas provas foram adicionadas")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Foram adicionadas $provaCount novas provas. Confira agora!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NEW_PROVA, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showReminderNotification(provaName: String, daysUntil: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Lembrete de Prova")
            .setContentText("$provaName - faltam $daysUntil dias")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A prova \"$provaName\" acontece em $daysUntil dias. Prepare-se!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
