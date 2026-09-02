package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.ScheduledRecurringOccurrence
import com.example.ui.components.IndianCurrencyFormatter

object NotificationHelper {
  private const val CHANNEL_ID = "paisa_payment_reminders"
  private const val CHANNEL_NAME = "Payment Reminders"
  private const val CHANNEL_DESC = "Notifications for due and scheduled recurring payments"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_DESC
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun showDuePaymentReminder(context: Context, occurrence: ScheduledRecurringOccurrence) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    createNotificationChannel(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra("navigate_to", "recurring")
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      occurrence.ruleId.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val amountStr = IndianCurrencyFormatter.format(occurrence.amount)
    val title = if (occurrence.daysDiff < 0) {
      "Overdue payment: ${occurrence.ruleTitle}"
    } else {
      "Payment due today"
    }

    val body = if (occurrence.daysDiff < 0) {
      "Your ${occurrence.ruleTitle} payment of $amountStr was due on ${occurrence.scheduledDateKey}. Tap to review and mark as paid."
    } else {
      "Your ${occurrence.ruleTitle} payment of $amountStr is scheduled for today. Pay it and mark it as done."
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    try {
      NotificationManagerCompat.from(context).notify(
        (occurrence.ruleId * 1000 + occurrence.scheduledDateKey.hashCode()).toInt(),
        builder.build()
      )
    } catch (ignored: SecurityException) {}
  }
}
