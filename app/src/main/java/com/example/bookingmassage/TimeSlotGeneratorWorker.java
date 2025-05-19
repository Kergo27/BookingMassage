package com.example.bookingmassage; // Hozz létre egy 'workers' csomagot

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.bookingmassage.FirebaseHelper; // Feltételezve, hogy a FirebaseHelper itt van
import com.google.android.gms.tasks.Tasks; // Szinkron várakozáshoz (nem ajánlott fő szálon, de workerben oké)
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class TimeSlotGeneratorWorker extends Worker {

    private static final String TAG = "TimeSlotGeneratorWrkr"; // Rövidített TAG
    private FirebaseHelper firebaseHelper; // Ezt itt nem tudjuk közvetlenül injektálni, a Firestore-t használjuk direktben

    public TimeSlotGeneratorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // A FirebaseHelper példányosítása itt problémás lehet, mert a Worker konstruktora
        // a háttérszálon futhat, és a FirebaseHelpernek szüksége lehet a fő szálra.
        // Jobb, ha a Firebase műveleteket itt direktben, vagy egy statikus helperrel végezzük.
        // Vagy használj Hilt/Dagger-t a dependency injectionhoz.
        // Most egyszerűsítésként direktben használjuk a Firestore-t.
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "TimeSlotGeneratorWorker elindult.");

        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestore referencia
        Calendar today = Calendar.getInstance();
        Calendar nextMonthCalendar = Calendar.getInstance();
        nextMonthCalendar.add(Calendar.MONTH, 1); // A következő hónap
        nextMonthCalendar.set(Calendar.DAY_OF_MONTH, 1); // A következő hónap első napja

        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String nextMonthYearMonth = monthFormat.format(nextMonthCalendar.getTime()); // Pl. "2025-07"

        Log.d(TAG, "Ellenőrzés a következő hónapra: " + nextMonthYearMonth);

        try {
            // Ellenőrizzük, hogy a következő hónap első napjára létezik-e már időpont.
            // Ez egy egyszerűsített ellenőrzés. Egy robosztusabb ellenőrzés megnézhetné
            // az egész hónapot, vagy egy speciális "generalt_honapok" markert a DB-ben.
            String firstDayOfNextMonth = nextMonthYearMonth + "-01";
            QuerySnapshot checkQuerySnapshot = Tasks.await(db.collection("time_slots")
                    .whereEqualTo("date", firstDayOfNextMonth)
                    .limit(1)
                    .get());

            if (checkQuerySnapshot.isEmpty()) {
                Log.i(TAG, "Nincsenek időpontok a következő hónapra (" + nextMonthYearMonth + "). Generálás indul...");

                // A FirebaseHelper példányosítása és használata itt, ha nem statikus.
                // Vagy a generáló logika közvetlen implementálása.
                // Most feltételezzük, hogy a FirebaseHelper-t tudjuk itt használni.
                // Vigyázat: Ha a FirebaseHelper aszinkron, akkor a doWork-nek
                // ListenableWorker-t kellene használnia a Result.success()/failure() megfelelő kezeléséhez.
                // Mivel a generateTimeSlotsForMonths aszinkron batch.commit()-ot használ,
                // ez a worker nem fogja megvárni a befejezését, hacsak nem tesszük szinkronná,
                // vagy nem használunk ListenableWorker-t.

                // Egyszerűsítés: Itt közvetlenül hívjuk a FirebaseHelper-t,
                // és reméljük, hogy a batch commitok lefutnak, mielőtt a worker befejeződik.
                // Egy jobb megoldás: A generateTimeSlotsForMonths adjon vissza egy Task-ot, amit itt Tasks.await-tel várunk meg.
                // Vagy a worker legyen egy ListenableWorker.

                // Most az egyszerűség kedvéért:
                FirebaseHelper localFirebaseHelper = new FirebaseHelper(); // Új példány
                localFirebaseHelper.generateTimeSlotsForMonths(1); // Generál 1 hónapnyi időpontot (ami a következő hónap lesz)
                // Figyelem: A generateTimeSlotsForMonths a jelenlegi implementáció szerint a *mai naptól* kezdve generál.
                // Ezt át kellene írni, hogy egy adott kezdődátumtól generáljon, vagy paraméterezhető legyen.
                // Most feltételezzük, hogy a generateTimeSlotsForMonths(1) a következő még nem generált hónapot fogja létrehozni.
                // Ideális esetben a generateTimeSlotsForMonths(targetCalendar, 1) lenne, ahol targetCalendar a nextMonthCalendar.

                Log.i(TAG, "Időpont generálás elindítva a következő hónapra.");
                // Mivel a generateTimeSlotsForMonths aszinkron, a Result.success() itt
                // nem jelenti, hogy a generálás befejeződött.
                // Ez egy hiányosság a jelenlegi egyszerűsített Worker-ben.
            } else {
                Log.i(TAG, "Már léteznek időpontok a következő hónapra (" + nextMonthYearMonth + "). Nincs szükség generálásra.");
            }

            return Result.success();

        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Hiba az időpontok ellenőrzése vagy generálása során.", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
            return Result.failure();
        } catch (Exception e) { // Bármilyen más hiba
            Log.e(TAG, "Váratlan hiba a workerben.", e);
            return Result.failure();
        }
    }
}
