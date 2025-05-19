package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.util.Log;
import androidx.annotation.NonNull; // Szükséges lehet a @NonNull-hez
import androidx.annotation.Nullable; // Szükséges a @Nullable-hez

import com.example.bookingmassage.Appointment;
import com.example.bookingmassage.TimeSlot;
import com.example.bookingmassage.User;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private final FirebaseFirestore db;

    // Kollekció nevek konstansként
    private static final String USERS_COLLECTION = "users";
    private static final String TIMESLOTS_COLLECTION = "time_slots";
    private static final String APPOINTMENTS_COLLECTION = "appointments";

    // Callback interfacek
    public interface OnUserCreationListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnTimeSlotsLoadedListener { void onLoaded(List<TimeSlot> timeSlots); void onError(Exception e); }
    public interface OnBookingCompleteListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnOperationCompleteListener { void onSuccess(); void onFailure(Exception e); }

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "FirebaseHelper (Firestore) inicializálva.");
    }

    // --- Felhasználó Kezelés ---
    public void createUser(User user, final OnUserCreationListener listener) {
        if (user == null || user.getUid() == null || user.getUid().isEmpty()) {
            if (listener != null) listener.onFailure(new IllegalArgumentException("User vagy UID nem lehet null/üres a createUser-ben."));
            return;
        }
        db.collection(USERS_COLLECTION).document(user.getUid()).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Firestore: Felhasználó sikeresen létrehozva: " + user.getUid());
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore: Hiba a felhasználó létrehozásakor: " + user.getUid(), e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // --- Időpont Slotok Generálása ---
    public void generateTimeSlotsForMonths(int numberOfMonths) {
        Log.i(TAG, "Firestore: generateTimeSlotsForMonths elindult " + numberOfMonths + " hónapra.");
        if (numberOfMonths <= 0) {
            Log.w(TAG, "A hónapok száma pozitív kell legyen.");
            return;
        }

        Calendar calendar = Calendar.getInstance();
        Calendar endDateCalendar = Calendar.getInstance();
        endDateCalendar.add(Calendar.MONTH, numberOfMonths);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        WriteBatch batch = db.batch();
        int slotsInBatch = 0;
        final int MAX_BATCH_SIZE = 490; // Firestore batch limit ~500

        while (calendar.before(endDateCalendar)) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY) {
                String currentDate = dateFormat.format(calendar.getTime());
                for (int hour = 8; hour < 18; hour++) {
                    Calendar slotCalendar = (Calendar) calendar.clone();
                    slotCalendar.set(Calendar.HOUR_OF_DAY, hour);
                    slotCalendar.set(Calendar.MINUTE, 0);
                    String currentTime = timeFormat.format(slotCalendar.getTime());
                    String slotId = currentDate + "_" + currentTime.replace(":", "");

                    TimeSlot newSlot = new TimeSlot(slotId, currentDate, currentTime);
                    DocumentReference slotDocRef = db.collection(TIMESLOTS_COLLECTION).document(slotId);
                    batch.set(slotDocRef, newSlot); // Felülírja, ha létezik
                    slotsInBatch++;

                    if (slotsInBatch >= MAX_BATCH_SIZE) {
                        Log.d(TAG, "Firestore: Batch (" + slotsInBatch + ") commitolása...");
                        batch.commit().addOnSuccessListener(aVoid -> Log.i(TAG, "Firestore: Időpontok egy adagja sikeresen mentve."))
                                .addOnFailureListener(e -> Log.e(TAG, "Firestore: Hiba az időpontok egy adagjának mentésekor.", e));
                        batch = db.batch(); // Új batch
                        slotsInBatch = 0;
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (slotsInBatch > 0) {
            Log.d(TAG, "Firestore: Utolsó batch (" + slotsInBatch + " elem) commitolása.");
            batch.commit()
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "Firestore: Időpont generálás (utolsó batch) befejeződött."))
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore: Hiba az időpont generálás (utolsó batch) során.", e));
        } else {
            Log.i(TAG, "Firestore: Időpont generálás befejeződött (nem volt utolsó batch).");
        }
    }

    // --- Időpont Slotok Lekérdezése ---
    public void getTimeSlotsForDate(String dateString, final OnTimeSlotsLoadedListener listener) {
        Log.d(TAG, "Firestore: Időpontok lekérdezése a következő dátumra: " + dateString);
        db.collection(TIMESLOTS_COLLECTION)
                .whereEqualTo("date", dateString)
                .orderBy("time", Query.Direction.ASCENDING) // Növekvő sorrend
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<TimeSlot> slots = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            TimeSlot slot = document.toObject(TimeSlot.class);
                            slots.add(slot);
                        }
                        Log.d(TAG, "Firestore: " + dateString + " dátumra " + slots.size() + " időpont objektum betöltve.");
                        if (listener != null) listener.onLoaded(slots);
                    } else {
                        Log.e(TAG, "Firestore: Hiba az időpontok lekérdezésekor " + dateString, task.getException());
                        if (listener != null) listener.onError(task.getException() != null ? task.getException() : new Exception("Ismeretlen hiba"));
                    }
                });
    }

    /**
     * ÚJ METÓDUS: Lekérdezi a következő X darab szabad időpontot egy adott naptól kezdve.
     */
    public void getNextXAvailableTimeSlots(@Nullable String startDateString, int limit, final OnTimeSlotsLoadedListener listener) {
        String effectiveStartDate;
        if (startDateString == null || startDateString.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            effectiveStartDate = dateFormat.format(new Date()); // Mai nap
        } else {
            effectiveStartDate = startDateString;
        }

        Log.d(TAG, "Firestore: Következő " + limit + " szabad időpont keresése, kezdődátum: " + effectiveStartDate);

        db.collection(TIMESLOTS_COLLECTION)
                .whereGreaterThanOrEqualTo("date", effectiveStartDate)
                .whereEqualTo("available", true)
                .orderBy("date", Query.Direction.ASCENDING)
                .orderBy("time", Query.Direction.ASCENDING)
                .limit(limit)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<TimeSlot> availableSlots = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            TimeSlot slot = document.toObject(TimeSlot.class);
                            availableSlots.add(slot);
                        }
                        Log.i(TAG, "Firestore: " + availableSlots.size() + " db szabad időpont betöltve " + effectiveStartDate + " naptól (limit: " + limit + ").");
                        if (listener != null) listener.onLoaded(availableSlots);
                    } else {
                        Log.e(TAG, "Firestore: Hiba a következő szabad időpontok lekérdezésekor.", task.getException());
                        if (listener != null) listener.onError(task.getException() != null ? task.getException() : new Exception("Ismeretlen hiba"));
                    }
                });
    }

    // --- Foglalási Műveletek ---
    public void bookSlot(String timeSlotId, String userId, String massageType, String date, String time, final OnBookingCompleteListener listener) {
        Log.d(TAG, "Firestore: bookSlot hívva. TimeSlotId: " + timeSlotId + ", UserId: " + userId + ", MassageType: " + massageType);
        final DocumentReference slotDocRef = db.collection(TIMESLOTS_COLLECTION).document(timeSlotId);
        // Új foglalásnak egyedi ID-t generálunk az 'appointments' kollekcióban
        final DocumentReference newAppointmentRef = db.collection(APPOINTMENTS_COLLECTION).document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot slotSnapshot = transaction.get(slotDocRef);
            TimeSlot currentSlot = slotSnapshot.toObject(TimeSlot.class);

            if (currentSlot == null) {
                throw new FirebaseFirestoreException("Időpont (slot) nem található: " + timeSlotId, FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!currentSlot.isAvailable()) {
                throw new FirebaseFirestoreException("Az időpont (" + timeSlotId + ") már foglalt.", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(slotDocRef, "available", false);
            transaction.update(slotDocRef, "bookedByUserId", userId);
            transaction.update(slotDocRef, "bookedMassageType", massageType);

            Appointment newAppointment = new Appointment(userId, timeSlotId, date, time, massageType);
            newAppointment.setAppointmentId(newAppointmentRef.getId()); // Firestore által generált ID beállítása
            transaction.set(newAppointmentRef, newAppointment);
            Log.d(TAG, "Firestore Tranzakció: newAppointment objektum: " + newAppointment.toString());
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.i(TAG, "Firestore: Tranzakció sikeres, időpont lefoglalva: " + timeSlotId + ", Appointment ID: " + newAppointmentRef.getId());
            if (listener != null) listener.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore: Tranzakció sikertelen az időpont (" + timeSlotId + ") foglalásakor.", e);
            if (listener != null) listener.onFailure(e);
        });
    }

    public Query getUserBookedAppointments(String userId) {
        Log.d(TAG, "Firestore: Felhasználó (" + userId + ") foglalásainak lekérdezése az '" + APPOINTMENTS_COLLECTION + "' kollekcióból.");
        return db.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("bookingTimestamp", Query.Direction.DESCENDING);
    }

    public void cancelUserAppointment(String appointmentId, String timeSlotId, final OnOperationCompleteListener listener) {
        Log.i(TAG, "Firestore: cancelUserAppointment. AppointmentId: " + appointmentId + ", TimeSlotId: " + timeSlotId);
        WriteBatch batch = db.batch();
        DocumentReference appointmentRef = db.collection(APPOINTMENTS_COLLECTION).document(appointmentId);
        batch.delete(appointmentRef);
        DocumentReference timeSlotRef = db.collection(TIMESLOTS_COLLECTION).document(timeSlotId);
        batch.update(timeSlotRef, "available", true);
        batch.update(timeSlotRef, "bookedByUserId", null);
        batch.update(timeSlotRef, "bookedMassageType", null);
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Firestore: Foglalás (" + appointmentId + ") törölve, TimeSlot (" + timeSlotId + ") felszabadítva.");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore: Hiba a foglalás (" + appointmentId + ") törlésekor.", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    public void modifyAppointmentMassageType(String appointmentId, String timeSlotId, String newMassageType, final OnOperationCompleteListener listener) {
        Log.i(TAG, "Firestore: modifyAppointmentMassageType. AppId: " + appointmentId + ", SlotId: " + timeSlotId + ", NewType: " + newMassageType);
        if (appointmentId == null || timeSlotId == null || newMassageType == null) {
            Log.e(TAG, "Hiba: null paraméter a modifyAppointmentMassageType-ban.");
            if (listener != null) listener.onFailure(new IllegalArgumentException("Hiányzó paraméterek."));
            return;
        }
        WriteBatch batch = db.batch();
        DocumentReference appointmentRef = db.collection(APPOINTMENTS_COLLECTION).document(appointmentId);
        batch.update(appointmentRef, "massageType", newMassageType);
        DocumentReference timeSlotRef = db.collection(TIMESLOTS_COLLECTION).document(timeSlotId);
        batch.update(timeSlotRef, "bookedMassageType", newMassageType);
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Firestore: Masszázs típus módosítva (" + appointmentId + ").");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore: Hiba a masszázs típus (" + appointmentId + ") módosításakor.", e);
                    if (listener != null) listener.onFailure(e);
                });
    }
    public void generateTimeSlotsForTargetMonth(Calendar targetMonthStartCalendar, final OnOperationCompleteListener completionListener) {
        Log.i(TAG, "Firestore: generateTimeSlotsForTargetMonth elindult: " + targetMonthStartCalendar.getTime().toString());

        Calendar calendar = (Calendar) targetMonthStartCalendar.clone(); // A célhónap első napjával kezdünk
        calendar.set(Calendar.DAY_OF_MONTH, 1); // Biztosítjuk, hogy a hónap első napja legyen

        Calendar endDateCalendar = (Calendar) calendar.clone();
        endDateCalendar.add(Calendar.MONTH, 1); // A következő hónap első napjáig generálunk

        // ... (a többi logika ugyanaz, mint a generateTimeSlotsForMonths-ban,
        //      de a 'calendar' a targetMonthStartCalendar-ból indul) ...

        // A batch.commit() végén a completionListener-t kellene hívni
        // if (slotsInBatch > 0) {
        //     batch.commit()
        //             .addOnSuccessListener(aVoid -> {
        //                 Log.i(TAG, "Firestore: Célhónap időpont generálása (utolsó batch) befejeződött.");
        //                 if (completionListener != null) completionListener.onSuccess();
        //             })
        //             .addOnFailureListener(e -> {
        //                 Log.e(TAG, "Firestore: Hiba a célhónap időpont generálása (utolsó batch) során.", e);
        //                 if (completionListener != null) completionListener.onFailure(e);
        //             });
        // } else {
        //      Log.i(TAG, "Firestore: Célhónap időpont generálás befejeződött (nem volt utolsó batch).");
        //      if (completionListener != null) completionListener.onSuccess(); // Vagy itt is jelezhetjük a sikert
        // }
    }

    public void generateSlotsForDateRange(Calendar startDateCalendar, int numberOfWeeks, final OnOperationCompleteListener listener) {
        Log.i(TAG, "Firestore: generateSlotsForDateRange elindult. Kezdődátum: " +
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDateCalendar.getTime()) +
                ", Hetek száma: " + numberOfWeeks);

        if (numberOfWeeks <= 0) {
            Log.w(TAG, "A hetek száma pozitív kell legyen.");
            if (listener != null) listener.onFailure(new IllegalArgumentException("A hetek száma pozitív kell legyen."));
            return;
        }

        Calendar calendar = (Calendar) startDateCalendar.clone(); // A megadott kezdődátummal indulunk
        Calendar endDateCalendar = (Calendar) startDateCalendar.clone();
        endDateCalendar.add(Calendar.WEEK_OF_YEAR, numberOfWeeks); // Hozzáadjuk a generálandó hetek számát

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        WriteBatch batch = db.batch();
        int slotsInBatch = 0;
        final int MAX_BATCH_SIZE = 490;
        int generatedCount = 0;

        while (calendar.before(endDateCalendar)) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY) { // Csak hétköznapokra
                String currentDate = dateFormat.format(calendar.getTime());
                for (int hour = 8; hour < 18; hour++) { // 8:00-tól 17:00-ig
                    Calendar slotCalendar = (Calendar) calendar.clone();
                    slotCalendar.set(Calendar.HOUR_OF_DAY, hour);
                    slotCalendar.set(Calendar.MINUTE, 0);
                    String currentTime = timeFormat.format(slotCalendar.getTime());
                    String slotId = currentDate + "_" + currentTime.replace(":", "");

                    TimeSlot newSlot = new TimeSlot(slotId, currentDate, currentTime);
                    DocumentReference slotDocRef = db.collection(TIMESLOTS_COLLECTION).document(slotId);
                    batch.set(slotDocRef, newSlot);
                    slotsInBatch++;
                    generatedCount++;

                    if (slotsInBatch >= MAX_BATCH_SIZE) {
                        Log.d(TAG, "Firestore: Batch (" + slotsInBatch + ") commitolása a generálás során...");
                        batch.commit().addOnSuccessListener(aVoid -> Log.i(TAG, "Firestore: Időpontok egy adagja sikeresen mentve."))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore: Hiba az időpontok egy adagjának mentésekor.", e);
                                    if (listener != null) listener.onFailure(e); // Korai hiba jelzése
                                    // Itt meg kellene szakítani a további batch-ek feldolgozását, ha hiba van.
                                });
                        batch = db.batch(); // Új batch
                        slotsInBatch = 0;
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (slotsInBatch > 0) {
            Log.d(TAG, "Firestore: Utolsó batch (" + slotsInBatch + " elem) commitolása a generálás során.");
            int finalGeneratedCount = generatedCount;
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.i(TAG, "Firestore: Időpont generálás (utolsó batch) sikeresen befejeződött. Összesen generált: " + finalGeneratedCount);
                        if (listener != null) listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firestore: Hiba az időpont generálás (utolsó batch) során.", e);
                        if (listener != null) listener.onFailure(e);
                    });
        } else if (generatedCount > 0) { // Ha volt generálás, de az utolsó batch üres volt (mert pont a limitnél fejeződött be)
            Log.i(TAG, "Firestore: Időpont generálás sikeresen befejeződött. Összesen generált: " + generatedCount);
            if (listener != null) listener.onSuccess();
        } else {
            Log.i(TAG, "Firestore: Nem történt időpont generálás (valószínűleg a ciklus nem futott le, vagy nem volt commitolandó batch).");
            if (listener != null) listener.onSuccess(); // Vagy onFailure, ha ez hibának számít
        }
    }

}
