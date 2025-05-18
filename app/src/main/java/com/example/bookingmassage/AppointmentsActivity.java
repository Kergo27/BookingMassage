package com.example.bookingmassage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Győződj meg róla, hogy a saját modelljeid és adaptered importálva vannak a helyes csomagból
import com.example.bookingmassage.AppointmentAdapter;
import com.example.bookingmassage.Appointment;
import com.example.bookingmassage.FirebaseHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// Firestore importok
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppointmentsActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentActionListener {

    private static final String TAG = "AppointmentsActivity";
    public static final int EDIT_APPOINTMENT_REQUEST_CODE = 101; // Request kód az EditActivity-hez

    private RecyclerView rvUserAppointments;
    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> userAppointmentsList = new ArrayList<>();
    private ProgressBar progressBarAppointments;
    private TextView tvNoUserAppointmentsMessage;
    private ImageButton btnBackAppointments;

    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Firestore real-time listenerhez
    private ListenerRegistration appointmentsListenerRegistration;
    private Query userAppointmentsQuery; // Firestore Query

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);
        Log.d(TAG, "onCreate elindult.");

        // UI Elemek inicializálása
        rvUserAppointments = findViewById(R.id.rvUserAppointments);
        progressBarAppointments = findViewById(R.id.progressBarAppointments);
        tvNoUserAppointmentsMessage = findViewById(R.id.tvNoUserAppointmentsMessage);
        btnBackAppointments = findViewById(R.id.btnBackAppointments);

        if (rvUserAppointments == null || progressBarAppointments == null || tvNoUserAppointmentsMessage == null || btnBackAppointments == null) {
            Log.e(TAG, "Hiba: Egy vagy több UI elem null a findViewById után az AppointmentsActivity-ben! Ellenőrizd az XML ID-kat.");
            Toast.makeText(this, "Hiba a felület inicializálásakor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Firebase inicializálás
        firebaseHelper = new FirebaseHelper(); // Ennek most Firestore logikát kell tartalmaznia
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó. Kérjük, jelentkezzen be.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Log.d(TAG, "Bejelentkezett felhasználó: " + currentUser.getEmail() + ", UID: " + currentUser.getUid());

        // Vissza gomb listener
        btnBackAppointments.setOnClickListener(v -> {
            Log.d(TAG, "Vissza gomb megnyomva az AppointmentsActivity-ből.");
            onBackPressed();
        });

        setupRecyclerView();
        // A foglalások betöltése az onStart()-ban fog történni, hogy a listener megfelelően frissüljön
    }

    private void setupRecyclerView() {
        rvUserAppointments.setLayoutManager(new LinearLayoutManager(this));
        // Az adapter konstruktorának most már az Activity-t kell kapnia listenerként
        appointmentAdapter = new AppointmentAdapter(userAppointmentsList, this, this);
        rvUserAppointments.setAdapter(appointmentAdapter);
        Log.d(TAG, "RecyclerView és AppointmentAdapter beállítva.");
    }

    private void loadUserAppointmentsRealtime() {
        if (currentUser == null) {
            Log.e(TAG, "loadUserAppointmentsRealtime: currentUser null!");
            updateUIVisibility(true, "Hiba: Nincs felhasználó az adatok lekéréséhez.");
            return;
        }
        final String userIdForQuery = currentUser.getUid();
        Log.i(TAG, "Firestore: loadUserAppointmentsRealtime elindult. UID: " + userIdForQuery);

        updateUIVisibility(false, null); // Töltés jelzése

        // Eltávolítjuk a régi listenert, ha létezik, mielőtt újat adnánk hozzá
        if (appointmentsListenerRegistration != null) {
            appointmentsListenerRegistration.remove();
            Log.d(TAG, "Régi Firestore listener eltávolítva.");
        }

        userAppointmentsQuery = firebaseHelper.getUserBookedAppointments(userIdForQuery); // Ez most Firestore Query-t ad vissza
        if (userAppointmentsQuery == null) {
            Log.e(TAG, "firebaseHelper.getUserBookedAppointments null Query-t adott vissza!");
            updateUIVisibility(true, "Hiba a lekérdezés előkészítésekor.");
            return;
        }
        Log.d(TAG, "Firestore Query objektum lekérve.");


        appointmentsListenerRegistration = userAppointmentsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Firestore: Hiba a valós idejű figyelés során.", e);
                    updateUIVisibility(true, "Hiba a foglalások frissítésekor.");
                    return;
                }

                userAppointmentsList.clear(); // Lista törlése minden frissítéskor
                if (snapshots != null && !snapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Appointment appointment = doc.toObject(Appointment.class);
                        // Az ID-t a Firestore dokumentum ID-jából vesszük, ha a modellben nincs setAppointmentId,
                        // vagy ha a Firestore generálta az ID-t és a modellben is tárolni akarjuk.
                        // A mi Appointment modellünknek van appointmentId mezője, amit a bookSlot-ban a Firestore ID-jával töltünk fel.
                        // Tehát az appointment.getAppointmentId() már a helyes ID-t tartalmazza.
                        userAppointmentsList.add(appointment);
                        Log.v(TAG, "Firestore: Betöltött/frissített foglalás: " + appointment.toString());
                    }
                    // Rendezés (a query már rendezi bookingTimestamp szerint csökkenőbe)
                    // Ha a query nem rendezné, itt kellene:
                    // Collections.sort(userAppointmentsList, Comparator.comparingLong(Appointment::getBookingTimestamp).reversed());
                    Log.d(TAG, "Firestore: " + userAppointmentsList.size() + " foglalás feldolgozva és hozzáadva a listához.");
                } else {
                    Log.d(TAG, "Firestore: Nincsenek foglalások ehhez a felhasználóhoz (snapshots üres vagy null).");
                }

                appointmentAdapter.updateAppointments(userAppointmentsList);
                updateUIVisibility(userAppointmentsList.isEmpty(), "Nincsenek aktuális foglalásaid.");
            }
        });
        Log.d(TAG, "Firestore: Valós idejű listener hozzáadva a userAppointmentsQuery-hez.");
    }

    /**
     * Segédfüggvény a UI elemek láthatóságának kezelésére.
     * @param showNoAppointmentsMessage Ha true, a "nincs foglalás" üzenet látszik, egyébként a lista.
     * @param customMessage A "nincs foglalás" üzenet szövege, ha null, az alapértelmezett használatos.
     */
    private void updateUIVisibility(boolean showNoAppointmentsMessage, @Nullable String customMessage) {
        progressBarAppointments.setVisibility(View.GONE); // A progressbart mindig elrejtjük, ha ez lefut
        if (showNoAppointmentsMessage) {
            tvNoUserAppointmentsMessage.setText(customMessage != null ? customMessage : "Nincsenek aktuális foglalásaid.");
            tvNoUserAppointmentsMessage.setVisibility(View.VISIBLE);
            rvUserAppointments.setVisibility(View.GONE);
            Log.d(TAG, "UI frissítve: 'Nincs foglalás' üzenet látható (" + (customMessage != null ? customMessage : "alapértelmezett") + ").");
        } else {
            tvNoUserAppointmentsMessage.setVisibility(View.GONE);
            rvUserAppointments.setVisibility(View.VISIBLE);
            Log.d(TAG, "UI frissítve: Foglalások listája látható.");
        }
    }


    // --- Implementáció az AppointmentAdapter.OnAppointmentActionListener interfészhez ---

    @Override
    public void onEditClicked(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentId() == null) {
            Log.e(TAG, "onEditClicked: Appointment vagy Appointment ID null!");
            Toast.makeText(this, "Hiba a foglalás kiválasztásakor.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Szerkesztés gomb megnyomva: " + appointment.toString());
        Intent intent = new Intent(this, EditAppointmentActivity.class);
        intent.putExtra("APPOINTMENT_ID", appointment.getAppointmentId());
        intent.putExtra("USER_ID", appointment.getUserId());
        intent.putExtra("TIME_SLOT_ID", appointment.getTimeSlotId());
        intent.putExtra("CURRENT_MASSAGE_TYPE", appointment.getMassageType());
        intent.putExtra("DATE", appointment.getDate());
        intent.putExtra("TIME", appointment.getTime());
        startActivityForResult(intent, EDIT_APPOINTMENT_REQUEST_CODE);
    }

    @Override
    public void onDeleteClicked(final Appointment appointment) {
        if (appointment == null || appointment.getAppointmentId() == null || appointment.getTimeSlotId() == null) {
            Log.e(TAG, "onDeleteClicked: Appointment, Appointment ID vagy TimeSlot ID null!");
            Toast.makeText(this, "Hiba a foglalás kiválasztásakor a törléshez.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Törlés gomb megnyomva: " + appointment.toString());
        new AlertDialog.Builder(this)
                .setTitle("Foglalás Törlése")
                .setMessage("Biztosan törölni szeretnéd ezt a foglalást?\n" +
                        (appointment.getDate() != null ? appointment.getDate() : "") + " " +
                        (appointment.getTime() != null ? appointment.getTime() : "") + "\n" +
                        (appointment.getMassageType() != null ? appointment.getMassageType() : ""))
                .setPositiveButton("Törlés", (dialog, which) -> {
                    if (currentUser == null) {
                        Toast.makeText(this, "Hiba: Nincs bejelentkezett felhasználó a törléshez.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    progressBarAppointments.setVisibility(View.VISIBLE); // Töltés jelzése a törlés alatt
                    firebaseHelper.cancelUserAppointment( // A Firestore verziót hívjuk
                            // A cancelUserAppointment-nek most már csak appointmentId és timeSlotId kell
                            appointment.getAppointmentId(),
                            appointment.getTimeSlotId(),
                            new FirebaseHelper.OnOperationCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    // A progressBarAppointments-t az onEvent listener fogja elrejteni, amikor a lista frissül
                                    Toast.makeText(AppointmentsActivity.this, "Foglalás sikeresen törölve.", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Foglalás (" + appointment.getAppointmentId() + ") törölve, a lista frissülni fog a Firestore listener által.");
                                    // A Firestore listener automatikusan frissíti a UI-t.
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    progressBarAppointments.setVisibility(View.GONE); // Hiba esetén manuálisan elrejtjük
                                    Toast.makeText(AppointmentsActivity.this, "Hiba a foglalás törlésekor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Hiba a foglalás törlésekor (" + appointment.getAppointmentId() + "): ", e);
                                }
                            }
                    );
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    // Kezeli az EditAppointmentActivity eredményét (ha van)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_APPOINTMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d(TAG, "EditAppointmentActivity RESULT_OK, a lista frissülni fog a Firestore listener által.");
            Toast.makeText(this, "Foglalás sikeresen módosítva.", Toast.LENGTH_SHORT).show();
            // A Firestore listener miatt a lista automatikusan frissül.
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - Foglalások valós idejű figyelésének indítása.");
        loadUserAppointmentsRealtime(); // Listener csatolása, amikor az Activity láthatóvá válik
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appointmentsListenerRegistration != null) {
            appointmentsListenerRegistration.remove(); // Listener eltávolítása, amikor az Activity már nem látható
            Log.d(TAG, "onStop - Firestore listener eltávolítva.");
        }
    }
}
