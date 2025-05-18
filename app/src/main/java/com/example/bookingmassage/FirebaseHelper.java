package com.example.bookingmassage;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.bookingmassage.Appointment;
import com.example.bookingmassage.TimeSlot;
import com.example.bookingmassage.User;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private final DatabaseReference databaseRootRef = FirebaseDatabase.getInstance().getReference();
    private final DatabaseReference usersRef = databaseRootRef.child("users");
    private final DatabaseReference timeSlotsRef = databaseRootRef.child("time_slots");
    private final DatabaseReference userAppointmentsRef = databaseRootRef.child("user_appointments");

    public Query getUserAppointments(String userId) {
        return null;
    }

    public interface OnUserCreationListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnTimeSlotsLoadedListener {
        void onLoaded(List<TimeSlot> timeSlots);
        void onError(DatabaseError databaseError);
    }

    public interface OnBookingCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnOperationCompleteListener { // Általános listener törléshez, módosításhoz
        void onSuccess();
        void onFailure(Exception e);
    }


    public FirebaseHelper() {
        Log.d(TAG, "FirebaseHelper inicializálva.");
        // generateInitialTimeSlotsForUpcomingWeek(); // Ezt csak egyszer kellene futtatni, vagy egy admin felületről
    }

    public void createUser(User user, final OnUserCreationListener listener) {
        // ... (a korábbi createUser kódod, ami már jó volt)
        if (user == null || user.getUid() == null || user.getUid().isEmpty()) {
            Log.e(TAG, "createUser: User vagy UID null/üres.");
            if (listener != null) listener.onFailure(new IllegalArgumentException("User vagy UID nem lehet null/üres."));
            return;
        }
        usersRef.child(user.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User sikeresen mentve: " + user.getUid());
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "User mentése sikertelen: " + user.getUid(), e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    /**
     * Generál időpontokat a következő hétre (H-P, 8-17).
     * Ezt óvatosan használd, nehogy felülírd a meglévő foglalásokat.
     * Ideális esetben egy admin felületen vagy csak egyszer futtatnád.
     */
    public void generateInitialTimeSlotsForUpcomingWeek() {
        Log.d(TAG, "generateInitialTimeSlotsForUpcomingWeek elindult.");
        Calendar calendar = Calendar.getInstance();
        // Előre a következő hétfőre
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < 5; i++) { // Hétfőtől péntekig
            String currentDate = dateFormat.format(calendar.getTime());
            for (int hour = 8; hour < 18; hour++) { // 8:00-tól 17:00-ig (a 18:00 már nem)
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, 0);
                String currentTime = timeFormat.format(calendar.getTime());
                String slotId = currentDate + "_" + currentTime.replace(":", ""); // Pl. "2025-05-26_0800"

                TimeSlot newSlot = new TimeSlot(slotId, currentDate, currentTime);
                // Ellenőrizzük, hogy létezik-e már, hogy ne írjuk felül a foglalásokat
                timeSlotsRef.child(slotId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            timeSlotsRef.child(slotId).setValue(newSlot)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Időpont létrehozva: " + slotId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Hiba az időpont létrehozásakor: " + slotId, e));
                        } else {
                            Log.d(TAG, "Időpont már létezik, nem írom felül: " + slotId);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Hiba az időpont ellenőrzésekor: " + slotId, error.toException());
                    }
                });
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1); // Következő nap
        }
        Log.d(TAG, "generateInitialTimeSlotsForUpcomingWeek befejeződött.");
    }

    /**
     * Lekérdezi az elérhető (és foglalt) időpontokat egy adott napra.
     */
    public void getTimeSlotsForDate(String dateString, final OnTimeSlotsLoadedListener listener) {
        Log.d(TAG, "FirebaseHelper: Időpontok lekérdezése a következő dátumra: " + dateString);
        Query query = timeSlotsRef.orderByChild("date").equalTo(dateString); // A "date" mező alapján keresünk

        query.addListenerForSingleValueEvent(new ValueEventListener() { // SingleValueEvent, hogy ne figyeljen folyamatosan
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<TimeSlot> slots = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot slotSnapshot : dataSnapshot.getChildren()) {
                        TimeSlot slot = slotSnapshot.getValue(TimeSlot.class);
                        if (slot != null) {
                            // Az ID-t is beállíthatjuk a Firebase kulcsából, ha a TimeSlot modellben
                            // a generáláskor nem azonos az ID-ja a kulccsal.
                            // De a mi esetünkben a slotId-t mi generáljuk és az a kulcs.
                            // slot.setId(slotSnapshot.getKey()); // Csak ha szükséges
                            slots.add(slot);
                            Log.v(TAG, "FirebaseHelper: Betöltött TimeSlot: " + slot.toString()); // Részletes log minden slotról
                        } else {
                            Log.w(TAG, "FirebaseHelper: Null TimeSlot objektum a snapshotban: " + slotSnapshot.getKey());
                        }
                    }
                    Log.d(TAG, "FirebaseHelper: " + dateString + " dátumra " + slots.size() + " időpont objektum betöltve az adatbázisból.");
                } else {
                    Log.d(TAG, "FirebaseHelper: " + dateString + " dátumra nem található időpont az adatbázisban (dataSnapshot nem létezik).");
                }
                if (listener != null) {
                    listener.onLoaded(slots); // Átadjuk a listát, még ha üres is
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "FirebaseHelper: Hiba az időpontok lekérdezésekor " + dateString + " dátumra.", databaseError.toException());
                if (listener != null) {
                    listener.onError(databaseError);
                }
            }
        });
    }

    /**
     * Lefoglal egy időpontot.
     * Atomikus műveletként frissíti a time_slots-ot és létrehoz egy user_appointment bejegyzést.
     */
    public void bookSlot(String timeSlotId, String userId, String massageTypeFromParam, String date, String time, final OnBookingCompleteListener listener) {
        Log.d(TAG, "bookSlot hívva. TimeSlotId: " + timeSlotId + ", UserId: " + userId + ", MassageTypeParam: " + massageTypeFromParam + ", Date: " + date + ", Time: " + time);

        DatabaseReference slotRef = timeSlotsRef.child(timeSlotId);

        slotRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TimeSlot currentSlot = snapshot.getValue(TimeSlot.class);
                if (currentSlot != null && currentSlot.isAvailable()) {
                    String appointmentPushKey = userAppointmentsRef.child(userId).push().getKey();
                    if (appointmentPushKey == null) {
                        Log.e(TAG, "Nem sikerült kulcsot generálni a user_appointments-hez.");
                        if (listener != null) listener.onFailure(new Exception("Hiba a foglalási kulcs generálásakor."));
                        return;
                    }

                    // Appointment objektum létrehozása
                    Appointment newAppointment = new Appointment(userId, timeSlotId, date, time, massageTypeFromParam);
                    newAppointment.setAppointmentId(appointmentPushKey);

                    // LOGOLJUK A LÉTREHOZOTT Appointment OBJEKTUMOT ÉS ANNAK MASSZÁZS TÍPUSÁT
                    Log.d(TAG, "Létrehozott newAppointment objektum: " + newAppointment.toString());
                    Log.d(TAG, "newAppointment.getMassageType() értéke: " + newAppointment.getMassageType());

                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/time_slots/" + timeSlotId + "/available", false);
                    childUpdates.put("/time_slots/" + timeSlotId + "/bookedByUserId", userId);
                    childUpdates.put("/time_slots/" + timeSlotId + "/bookedMassageType", massageTypeFromParam); // Itt is a paramétert használjuk
                    childUpdates.put("/user_appointments/" + userId + "/" + appointmentPushKey, newAppointment); // Itt a newAppointment objektumot

                    // LOGOLJUK A childUpdates MAP-OT (a newAppointment részét)
                    Log.d(TAG, "childUpdates map a Firebase-be írás előtt (user_appointments rész): " + newAppointment.toString()); // Vagy a teljes map, ha kell

                    databaseRootRef.updateChildren(childUpdates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Időpont sikeresen lefoglalva és user_appointment létrehozva. TimeSlotId: " + timeSlotId);
                                if (listener != null) listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Hiba az időpont foglalása során (updateChildren). TimeSlotId: " + timeSlotId, e);
                                if (listener != null) listener.onFailure(e);
                            });

                } else {
                    Log.w(TAG, "Az időpont (" + timeSlotId + ") már nem elérhető vagy nem létezik.");
                    if (listener != null) listener.onFailure(new Exception("Az időpont már foglalt vagy nem létezik."));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Hiba az időpont állapotának ellenőrzésekor. TimeSlotId: " + timeSlotId, error.toException());
                if (listener != null) listener.onFailure(error.toException());
            }
        });
    }

    /**
     * Lekérdezi a felhasználó összes foglalását.
     */
    public Query getUserBookedAppointments(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "getUserBookedAppointments hívva null vagy üres userId-val.");
            return userAppointmentsRef.child("invalid_user_for_query").orderByKey(); // Üres eredményt ad
        }
        Log.d(TAG, "Felhasználó (" + userId + ") foglalásainak lekérdezése a 'user_appointments' csomópontból.");
        return userAppointmentsRef.child(userId).orderByChild("bookingTimestamp"); // Időrendben
    }

    /**
     * Törli a felhasználó egy foglalását és felszabadítja a hozzá tartozó TimeSlot-ot.
     */
    public void cancelUserAppointment(String userId, String appointmentId, String timeSlotId, final OnOperationCompleteListener listener) {
        Log.d(TAG, "cancelUserAppointment hívva. UserId: " + userId + ", AppointmentId: " + appointmentId + ", TimeSlotId: " + timeSlotId);

        if (userId == null || appointmentId == null || timeSlotId == null) {
            Log.e(TAG,"Érvénytelen paraméterek a cancelUserAppointment-ben.");
            if(listener != null) listener.onFailure(new IllegalArgumentException("Érvénytelen paraméterek."));
            return;
        }

        Map<String, Object> childUpdates = new HashMap<>();
        // TimeSlot frissítése (felszabadítás)
        childUpdates.put("/time_slots/" + timeSlotId + "/available", true);
        childUpdates.put("/time_slots/" + timeSlotId + "/bookedByUserId", null);
        childUpdates.put("/time_slots/" + timeSlotId + "/bookedMassageType", null);
        // User_appointment bejegyzés törlése
        childUpdates.put("/user_appointments/" + userId + "/" + appointmentId, null); // null értékkel törlünk

        databaseRootRef.updateChildren(childUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Foglalás sikeresen törölve és TimeSlot felszabadítva. AppointmentId: " + appointmentId);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Hiba a foglalás törlése során. AppointmentId: " + appointmentId, e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    /**
     * Időpont módosítása (pl. csak a masszázs típusának megváltoztatása).
     * Ha az időpont (dátum/idő) is változik, az egy törlés és új foglalás művelet lenne.
     */
    public void modifyAppointmentMassageType(String userId, String appointmentId, String timeSlotId, String newMassageType, final OnOperationCompleteListener listener) {
        Log.d(TAG, "modifyAppointmentMassageType hívva. UserId: " + userId + ", AppId: " + appointmentId + ", SlotId: " + timeSlotId + ", NewType: " + newMassageType);

        if (userId == null || appointmentId == null || timeSlotId == null || newMassageType == null) {
            Log.e(TAG,"Érvénytelen paraméterek a modifyAppointmentMassageType-ben.");
            if(listener != null) listener.onFailure(new IllegalArgumentException("Érvénytelen paraméterek."));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("/user_appointments/" + userId + "/" + appointmentId + "/massageType", newMassageType);
        updates.put("/time_slots/" + timeSlotId + "/bookedMassageType", newMassageType);

        databaseRootRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Masszázs típus sikeresen módosítva. AppointmentId: " + appointmentId);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Hiba a masszázs típus módosítása során. AppointmentId: " + appointmentId, e);
                    if (listener != null) listener.onFailure(e);
                });
    }
}
