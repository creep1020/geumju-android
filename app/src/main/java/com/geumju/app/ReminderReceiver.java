package com.geumju.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL = "daily_reminder_v1";
    static final String PREFS = "reminder";
    static boolean enabled(Context c) { return c.getSharedPreferences(PREFS, 0).getBoolean("enabled", false); }
    static void schedule(Context c) {
        AlarmManager alarm = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = PendingIntent.getBroadcast(c, 19, new Intent(c, ReminderReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarm.cancel(pending);
        if (!enabled(c)) return;
        Calendar when = Calendar.getInstance();
        when.set(Calendar.HOUR_OF_DAY, 19); when.set(Calendar.MINUTE, 0); when.set(Calendar.SECOND, 0); when.set(Calendar.MILLISECOND, 0);
        if (when.getTimeInMillis() <= System.currentTimeMillis()) when.add(Calendar.DAY_OF_YEAR, 1);
        // Doze/battery policy may delay this inexact alarm.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pending);
    }
    @Override public void onReceive(Context c, Intent intent) {
        if (!enabled(c)) return;
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "저녁 7시 금주 알림", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent tap = PendingIntent.getActivity(c, 20, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("오늘도 금주")
         .setContentText("오늘 하루 기록을 확인하고, 내일의 나를 응원해 줘!")
         .setAutoCancel(true).setContentIntent(tap);
        try { nm.notify(19, b.build()); } catch (SecurityException ignored) {}
        schedule(c);
    }
}
