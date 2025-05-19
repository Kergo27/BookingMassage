// MyApplication.java
package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.app.Application;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.bookingmassage.TimeSlotGeneratorWorker; // Importáld a Workert
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static final String TIME_SLOT_GENERATOR_WORK_TAG = "timeSlotGeneratorWork";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyApplication onCreate - WorkManager ütemezése.");
        scheduleTimeSlotGeneration();
    }

    private void scheduleTimeSlotGeneration() {
        // Kényszerek (Constraints) a feladathoz
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)       // Csak akkor fusson, ha az eszköz töltőn van
                .setRequiresDeviceIdle(true)     // Csak akkor fusson, ha az eszköz tétlen
                // .setRequiredNetworkType(NetworkType.CONNECTED) // Hálózati kapcsolat szükséges a Firestore-ba íráshoz
                .build();

        // Periodikus WorkRequest létrehozása
        // A WorkManager minimum ismétlődési ideje 15 perc. Havi ismétlődéshez a Workernek
        // magának kell ellenőriznie, hogy valóban a hónap utolsó hetében van-e.
        // Itt egy gyakoribb (pl. napi) ismétlődést állítunk be, és a Worker dönti el, hogy csinál-e valamit.
        // Vagy használhatsz OneTimeWorkRequest-et, amit az AlarmManager indít el havonta.
        // Most egy napi periodikus kérést készítünk, ami ellenőrzi a feltételeket.
        PeriodicWorkRequest timeSlotGeneratorRequest =
                new PeriodicWorkRequest.Builder(TimeSlotGeneratorWorker.class,
                        1, TimeUnit.DAYS) // Minimum 15 perc, itt napi ismétlődés
                        // 7, TimeUnit.DAYS) // Vagy heti ismétlődés
                        .setConstraints(constraints)
                        .addTag(TIME_SLOT_GENERATOR_WORK_TAG) // Opcionális tag a munka azonosításához
                        .build();

        // A munka ütemezése
        // Az ExistingPeriodicWorkPolicy.KEEP biztosítja, hogy ha már van ilyen ütemezett munka,
        // akkor az új kérés nem írja felül, hanem az eredeti marad.
        // Ha azt akarod, hogy mindig újraütemezze (pl. új feltételekkel), használj .REPLACE-t.
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                TIME_SLOT_GENERATOR_WORK_TAG, // Egyedi név a munkához
                ExistingPeriodicWorkPolicy.KEEP, // Megtartja a meglévőt, ha már létezik
                timeSlotGeneratorRequest
        );

        Log.i(TAG, "TimeSlotGeneratorWorker periodikus munka ütemezve (napi, KEEP policy).");
    }
}
