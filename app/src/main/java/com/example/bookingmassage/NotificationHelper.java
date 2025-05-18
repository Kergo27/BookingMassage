// NotificationHelper.java
package com.example.bookingmassage;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log; // Log importálása

import com.example.bookingmassage.HomeActivity; // Vagy az az Activity, amit meg akarsz nyitni
import com.example.bookingmassage.R; // Győződj meg róla, hogy az R import helyes

public class NotificationHelper {

    private static final String CHANNEL_ID = "available_slots_channel";
    private static final String CHANNEL_NAME = "Elérhető Időpontok";
    private static final String CHANNEL_DESCRIPTION = "Értesítések a legközelebbi szabad időpontokról";
    private static final int NOTIFICATION_ID_BASE = 1001;

    private Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Eredeti metódus (rövid szöveghez)
    public void showAvailableSlotsNotification(String title, String message, int notificationIdOffset) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // CSERÉLD LE EGY MEGFELELŐ IKONRA!
                .setContentTitle(title)
                .setContentText(message) // Ez a rövid, egysoros szöveg
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID_BASE + notificationIdOffset, builder.build());
            Log.d("NotificationHelper", "Értesítés megjelenítve (rövid): ID=" + (NOTIFICATION_ID_BASE + notificationIdOffset) + ", Cím=" + title);
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "Hiba az értesítés megjelenítésekor: " + e.getMessage() + ". Hiányzik a POST_NOTIFICATIONS engedély Android 13+ esetén?");
        }
    }

    /**
     * ÚJ METÓDUS: Értesítést jelenít meg BigTextStyle használatával a hosszabb szöveghez.
     * @param title Az értesítés címe (összecsukott és kibontott állapotban is ez lesz a főcím)
     * @param shortContentText Rövid szöveg, ami az összecsukott értesítésben látszik, és a kibontott alatt is.
     * @param bigText Hosszú szöveg, ami a kibontott értesítésben jelenik meg.
     * @param notificationIdOffset Egyedi azonosító az értesítéshez.
     */
    public void showAvailableSlotsNotificationWithBigText(String title, String shortContentText, String bigText, int notificationIdOffset) {
        Intent intent = new Intent(context, HomeActivity.class); // Cél Activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                .bigText(bigText);
        // .setBigContentTitle("Részletes Időpontok") // Opcionális: Külön cím a kibontott nézethez
        // .setSummaryText("Összefoglaló..."); // Opcionális: Összefoglaló szöveg

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // CSERÉLD LE EGY MEGFELELŐ IKONRA! (pl. res/drawable/ic_stat_calendar.xml)
                .setContentTitle(title) // Cím, ami mindig látszik
                .setContentText(shortContentText) // Rövid szöveg, ami összecsukva látszik
                .setStyle(bigTextStyle) // Itt alkalmazzuk a BigTextStyle-t
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // Mi történjen kattintáskor
                .setAutoCancel(true); // Tűnjön el kattintás után

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID_BASE + notificationIdOffset, builder.build());
            Log.d("NotificationHelper", "Értesítés megjelenítve (BigText): ID=" + (NOTIFICATION_ID_BASE + notificationIdOffset) + ", Cím=" + title);
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "Hiba az értesítés megjelenítésekor: " + e.getMessage() + ". Hiányzik a POST_NOTIFICATIONS engedély Android 13+ esetén?");
        }
    }
}
