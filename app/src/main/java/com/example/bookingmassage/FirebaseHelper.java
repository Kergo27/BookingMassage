package com.example.bookingmassage;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
// Nincs szükség a HashMap-re a multi-path update-hez a Firestore-ban,
// helyette WriteBatch vagy Transaction használatos.

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private final FirebaseFirestore db; // Firestore adatbázis referencia

    // Kollekció nevek
    private static final String USERS_COLLECTION = "users";
    private static final String TIMESLOTS_COLLECTION = "time_slots";
    private static final String APPOINTMENTS_COLLECTION = "appointments"; // Új, központi appointments kollekció

    // Callback interfacek (ezek maradhatnak)
    public interface OnUserCreationListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnTimeSlotsLoadedListener { void onLoaded(List<TimeSlot> timeSlots); void onError(Exception e); } // Exception a DatabaseError helyett
    public interface OnBookingCompleteListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnOperationCompleteListener { void onSuccess(); void onFailure(Exception e); }

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance(); // Firestore inicializálása
        Log.d(TAG, "FirebaseHelper (Firestore) inicializálva.");
    }

    public void createUser(User user, final OnUserCreationListener listener) {
        if (user == null || user.getUid() == null || user.getUid().isEmpty()) {
            if (listener != null) listener.onFailure(new IllegalArgumentException("User vagy UID nem lehet null/üres."));
            return;
        }
        db.collection(USERS_COLLECTION).document(user.getUid()).set(user)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e);
                });
    }

    public void generateTimeSlotsForMonths(int numberOfMonths) {
        Log.i(TAG, "Firestore: generateTimeSlotsForMonths elindult " + numberOfMonths + " hónapra.");
        if (numberOfMonths <= 0) return;

        Calendar calendar = Calendar.getInstance();
        Calendar endDateCalendar = Calendar.getInstance();
        endDateCalendar.add(Calendar.MONTH, numberOfMonths);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        WriteBatch batch = db.batch(); // Batch írás a hatékonyságért

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
                    // Firestore-ban a .set() felülírja, ha létezik, vagy létrehozza, ha nem.
                    // Ha csak akkor akarod létrehozni, ha nem létezik, először egy .get()-et kellene csinálni,
                    // de batch írásnál ez nem triviális. Egyszerűsítésként most feltételezzük,
                    // hogy ez egy inicializáló feltöltés, vagy az ID-k egyediek lesznek (pl. dátummal bővítve).
                    // Vagy a .set(newSlot, SetOptions.merge()) használható, ha csak bizonyos mezőket akarsz frissíteni, ha már létezik.
                    // Most egyszerűen .set()-et használunk, ami felülír, ha az ID már létezik.
                    // Jobb megoldás lenne egy Cloud Function, ami ellenőrzi a meglétet.
                    batch.set(slotDocRef, newSlot);
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Firestore: " + numberOfMonths + " hónapnyi időpont (batch) sikeresen generálva/frissítve."))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore: Hiba a " + numberOfMonths + " hónapnyi időpont generálása során.", e));
    }

    public void getTimeSlotsForDate(String dateString, final OnTimeSlotsLoadedListener listener) {
        Log.d(TAG, "Firestore: Időpontok lekérdezése: " + dateString);
        db.collection(TIMESLOTS_COLLECTION)
                .whereEqualTo("date", dateString) // Szűrés a "date" mezőre
                .orderBy("time") // Rendezés az "time" mező alapján (ehhez index kellhet)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<TimeSlot> slots = new ArrayList<>();
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                TimeSlot slot = document.toObject(TimeSlot.class);
                                // Az ID-t a Firestore dokumentum ID-jából is beállíthatjuk, ha a modellben van rá mező
                                // slot.setId(document.getId()); // De a mi TimeSlot modellünkben az ID-t mi magunk generáljuk
                                slots.add(slot);
                            }
                        }
                        Log.d(TAG, "Firestore: " + dateString + " dátumra " + slots.size() + " időpont betöltve.");
                        if (listener != null) listener.onLoaded(slots);
                    } else {
                        Log.e(TAG, "Firestore: Hiba az időpontok lekérdezésekor " + dateString, task.getException());
                        if (listener != null) listener.onError(task.getException());
                    }
                });
    }


    public void bookSlot(String timeSlotId, String userId, String massageType, String date, String time, final OnBookingCompleteListener listener) {
        Log.d(TAG, "Firestore: bookSlot hívva. TimeSlotId: " + timeSlotId + ", UserId: " + userId);
        final DocumentReference slotDocRef = db.collection(TIMESLOTS_COLLECTION).document(timeSlotId);
        final DocumentReference newAppointmentRef = db.collection(APPOINTMENTS_COLLECTION).document(); // Automatikusan generált ID az új foglalásnak

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot slotSnapshot = transaction.get(slotDocRef);
            TimeSlot currentSlot = slotSnapshot.toObject(TimeSlot.class);

            if (currentSlot == null) {
                throw new FirebaseFirestoreException("Időpont nem található: " + timeSlotId,
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!currentSlot.isAvailable()) {
                throw new FirebaseFirestoreException("Az időpont (" + timeSlotId + ") már foglalt.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            // Időpont lefoglalása
            transaction.update(slotDocRef, "available", false);
            transaction.update(slotDocRef, "bookedByUserId", userId);
            transaction.update(slotDocRef, "bookedMassageType", massageType);

            // Új Appointment dokumentum létrehozása
            Appointment newAppointment = new Appointment(userId, timeSlotId, date, time, massageType);
            newAppointment.setAppointmentId(newAppointmentRef.getId()); // Beállítjuk a Firestore által generált ID-t
            transaction.set(newAppointmentRef, newAppointment);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Firestore: Tranzakció sikeres, időpont lefoglalva: " + timeSlotId);
            if (listener != null) listener.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore: Tranzakció sikertelen az időpont foglalásakor: " + timeSlotId, e);
            if (listener != null) listener.onFailure(e);
        });
    }


    public Query getUserBookedAppointments(String userId) {
        Log.d(TAG, "Firestore: Felhasználó (" + userId + ") foglalásainak lekérdezése.");
        return db.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo("userId", userId) // Szűrés a felhasználó ID-jára
                .orderBy("bookingTimestamp", Query.Direction.DESCENDING); // Rendezés (ehhez index kell)
    }

    public void cancelUserAppointment(String appointmentId, String timeSlotId, final OnOperationCompleteListener listener) {
        Log.d(TAG, "Firestore: cancelUserAppointment. AppointmentId: " + appointmentId + ", TimeSlotId: " + timeSlotId);

        WriteBatch batch = db.batch();

        // Appointment dokumentum törlése
        DocumentReference appointmentRef = db.collection(APPOINTMENTS_COLLECTION).document(appointmentId);
        batch.delete(appointmentRef);

        // TimeSlot dokumentum frissítése (felszabadítás)
        DocumentReference timeSlotRef = db.collection(TIMESLOTS_COLLECTION).document(timeSlotId);
        batch.update(timeSlotRef, "available", true);
        batch.update(timeSlotRef, "bookedByUserId", null);
        batch.update(timeSlotRef, "bookedMassageType", null);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore: Foglalás (" + appointmentId + ") törölve, TimeSlot (" + timeSlotId + ") felszabadítva.");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore: Hiba a foglalás törlésekor.", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    public void modifyAppointmentMassageType(String appointmentId, String timeSlotId, String newMassageType, final OnOperationCompleteListener listener) {
        Log.d(TAG, "Firestore: modifyAppointmentMassageType. AppId: " + appointmentId + ", SlotId: " + timeSlotId + ", NewType: " + newMassageType);

        WriteBatch batch = db.batch();

        // Appointment dokumentum frissítése
        DocumentReference appointmentRef = db.collection(APPOINTMENTS_COLLECTION).document(appointmentId);
        batch.update(appointmentRef, "massageType", newMassageType);

        // TimeSlot dokumentum frissítése
        DocumentReference timeSlotRef = db.collection(TIMESLOTS_COLLECTION).document(timeSlotId);
        batch.update(timeSlotRef, "bookedMassageType", newMassageType);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore: Masszázs típus módosítva (" + appointmentId + ").");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore: Hiba a masszázs típus módosításakor.", e);
                    if (listener != null) listener.onFailure(e);
                });
    }
}
