package com.example.bookingmassage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem; // ÚJ IMPORT a vissza gombhoz
import android.view.View;
// import android.widget.ImageButton; // Régi import, már nem kell
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Szükséges a MenuItem-hez és a Firestore listenerhez
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// ÚJ IMPORT a MaterialToolbarhoz
import com.google.android.material.appbar.MaterialToolbar;

// Győződj meg róla, hogy a saját modelljeid és adaptered importálva vannak a helyes csomagból
import com.example.bookingmassage.Appointment; // Feltételezve, hogy a modellek itt vannak
// import com.example.bookingmassage.AppointmentAdapter; // Az adaptered importja
// import com.example.bookingmassage.FirebaseHelper; // A FirebaseHelper importja

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener; // Hozzáadva, ha hiányzott
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
// import java.util.Collections; // Nincs rá szükség, ha a query rendez
// import java.util.Comparator; // Nincs rá szükség, ha a query rendez
import java.util.List;

public class AppointmentsActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentActionListener {

    private static final String TAG = "AppointmentsActivity";
    public static final int EDIT_APPOINTMENT_REQUEST_CODE = 101;

    private RecyclerView rvUserAppointments;
    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> userAppointmentsList = new ArrayList<>(); // Inicializálás itt
    private ProgressBar progressBarAppointments;
    private TextView tvNoUserAppointmentsMessage;
    private MaterialToolbar toolbarAppointments; // KICSERÉLVE: ImageButton -> MaterialToolbar

    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth; // mAuth használata a currentUser helyett a FirebaseAuth példányhoz
    private FirebaseUser currentUser;

    private ListenerRegistration appointmentsListenerRegistration;
    private Query userAppointmentsQuery;

    // @SuppressLint({"MissingInflatedId", "WrongViewCast"}) // A WrongViewCast valószínűleg a régi ImageButton miatt volt
    @SuppressLint({"MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Győződj meg róla, hogy az XML fájl neve helyes és a módosított verziót használod
        setContentView(R.layout.activity_appointments);
        Log.d(TAG, "onCreate elindult.");

        // UI Elemek inicializálása
        toolbarAppointments = findViewById(R.id.toolbarAppointments); // ÚJ: Toolbar inicializálása
        rvUserAppointments = findViewById(R.id.rvUserAppointments);
        progressBarAppointments = findViewById(R.id.progressBarAppointments);
        tvNoUserAppointmentsMessage = findViewById(R.id.tvNoUserAppointmentsMessage);
        // A btnBackAppointments inicializálása eltávolítva

        // Toolbar beállítása Action Barként
        if (toolbarAppointments != null) {
            setSupportActionBar(toolbarAppointments);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Vissza gomb megjelenítése
                getSupportActionBar().setDisplayShowHomeEnabled(true); // Vissza gomb engedélyezése
                // A címet az XML-ben (app:title) állítottuk be a MaterialToolbaron
            }
        } else {
            Log.e(TAG, "HIBA: toolbarAppointments nem található az activity_appointments.xml-ben!");
        }

        // Null ellenőrzés a kritikus UI elemekre
        if (rvUserAppointments == null || progressBarAppointments == null || tvNoUserAppointmentsMessage == null || toolbarAppointments == null) { // toolbarAppointments ellenőrzése
            Log.e(TAG, "Hiba: Egy vagy több UI elem null a findViewById után az AppointmentsActivity-ben! Ellenőrizd az XML ID-kat.");
            Toast.makeText(this, "Hiba a felület inicializálásakor.", Toast.LENGTH_LONG).show();
            finish(); // Ha kritikus elemek hiányoznak, lépjünk ki
            return;
        }

        // Firebase inicializálás
        firebaseHelper = new FirebaseHelper();
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

        // A régi btnBackAppointments.setOnClickListener(...) rész eltávolítva,
        // mert a Toolbar vissza gombját az onOptionsItemSelected kezeli.

        setupRecyclerView();
        // A foglalások betöltése (listener csatolása) az onStart()-ban történik.
    }

    // Vissza gomb kezelése a Toolbaron
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Ugyanaz, mint a rendszer vissza gombja
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        rvUserAppointments.setLayoutManager(new LinearLayoutManager(this));
        // Az userAppointmentsList-et már az osztály szintjén inicializáltuk
        appointmentAdapter = new AppointmentAdapter(userAppointmentsList, this, this); // Átadva a context és a listener
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

        // Töltés jelzése (ProgressBar megjelenítése, lista és "nincs adat" üzenet elrejtése)
        if (progressBarAppointments != null) progressBarAppointments.setVisibility(View.VISIBLE);
        if (rvUserAppointments != null) rvUserAppointments.setVisibility(View.GONE);
        if (tvNoUserAppointmentsMessage != null) tvNoUserAppointmentsMessage.setVisibility(View.GONE);


        if (appointmentsListenerRegistration != null) {
            appointmentsListenerRegistration.remove();
            Log.d(TAG, "Régi Firestore listener eltávolítva.");
        }

        userAppointmentsQuery = firebaseHelper.getUserBookedAppointments(userIdForQuery);
        if (userAppointmentsQuery == null) {
            Log.e(TAG, "firebaseHelper.getUserBookedAppointments null Query-t adott vissza!");
            updateUIVisibility(true, "Hiba a lekérdezés előkészítésekor.");
            return;
        }
        Log.d(TAG, "Firestore Query objektum lekérve a valós idejű figyeléshez.");

        appointmentsListenerRegistration = userAppointmentsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Firestore: Hiba a valós idejű figyelés során.", e);
                    updateUIVisibility(true, "Hiba a foglalások frissítésekor: " + e.getMessage());
                    return;
                }

                userAppointmentsList.clear();
                if (snapshots != null && !snapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Appointment appointment = doc.toObject(Appointment.class);
                        userAppointmentsList.add(appointment);
                        // Log.v(TAG, "Firestore: Betöltött/frissített foglalás: " + appointment.toString());
                    }
                    Log.d(TAG, "Firestore: " + userAppointmentsList.size() + " foglalás feldolgozva.");
                } else {
                    Log.d(TAG, "Firestore: Nincsenek foglalások ehhez a felhasználóhoz (snapshots üres vagy null).");
                }

                appointmentAdapter.updateAppointments(userAppointmentsList); // Adapter értesítése az új adatokról
                updateUIVisibility(userAppointmentsList.isEmpty(), "Nincsenek aktuális foglalásaid.");
            }
        });
        Log.d(TAG, "Firestore: Valós idejű listener hozzáadva a userAppointmentsQuery-hez.");
    }

    private void updateUIVisibility(boolean showNoAppointmentsMessage, @Nullable String customMessage) {
        if (progressBarAppointments != null) progressBarAppointments.setVisibility(View.GONE);

        if (showNoAppointmentsMessage) {
            if (tvNoUserAppointmentsMessage != null) {
                tvNoUserAppointmentsMessage.setText(customMessage != null ? customMessage : "Nincsenek aktuális foglalásaid.");
                tvNoUserAppointmentsMessage.setVisibility(View.VISIBLE);
            }
            if (rvUserAppointments != null) rvUserAppointments.setVisibility(View.GONE);
            Log.d(TAG, "UI frissítve: 'Nincs foglalás' üzenet látható.");
        } else {
            if (tvNoUserAppointmentsMessage != null) tvNoUserAppointmentsMessage.setVisibility(View.GONE);
            if (rvUserAppointments != null) rvUserAppointments.setVisibility(View.VISIBLE);
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
        startActivityForResult(intent, EDIT_APPOINTMENT_REQUEST_CODE); // Vagy csak startActivity, ha nem vársz eredményt
    }

    @Override
    public void onDeleteClicked(final Appointment appointment) {
        // ... (a onDeleteClicked kódod itt változatlanul maradhat) ...
        if (appointment == null || appointment.getAppointmentId() == null || appointment.getTimeSlotId() == null) {
            Log.e(TAG, "onDeleteClicked: Kritikus ID-k null-ok!");
            Toast.makeText(this, "Hiba a törlés előkészítésekor.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Törlés gomb megnyomva: " + appointment.getAppointmentId());
        new AlertDialog.Builder(this)
                .setTitle("Foglalás Törlése")
                .setMessage("Biztosan törölni szeretnéd ezt a foglalást?\n" +
                        (appointment.getDate() != null ? appointment.getDate() : "") + " " +
                        (appointment.getTime() != null ? appointment.getTime() : "") + "\n" +
                        (appointment.getMassageType() != null ? appointment.getMassageType() : ""))
                .setPositiveButton("Törlés", (dialog, which) -> {
                    if (currentUser == null) {
                        Toast.makeText(this, "Hiba: Nincs bejelentkezett felhasználó.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (progressBarAppointments != null) progressBarAppointments.setVisibility(View.VISIBLE);
                    firebaseHelper.cancelUserAppointment(
                            appointment.getAppointmentId(),
                            appointment.getTimeSlotId(),
                            new FirebaseHelper.OnOperationCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    // A progressBar-t az onEvent listener kezeli.
                                    Toast.makeText(AppointmentsActivity.this, "Foglalás sikeresen törölve.", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Foglalás (" + appointment.getAppointmentId() + ") törölve.");
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    if (progressBarAppointments != null) progressBarAppointments.setVisibility(View.GONE);
                                    Toast.makeText(AppointmentsActivity.this, "Hiba a törléskor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Hiba a törléskor (" + appointment.getAppointmentId() + "): ", e);
                                }
                            }
                    );
                })
                .setNegativeButton("Mégse", null)
                .setIcon(android.R.drawable.ic_dialog_alert) // Opcionális ikon
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_APPOINTMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d(TAG, "EditAppointmentActivity RESULT_OK, a lista frissülni fog a Firestore listener által.");
            Toast.makeText(this, "Foglalás sikeresen módosítva.", Toast.LENGTH_SHORT).show();
            // A lista automatikusan frissül a Firestore listener miatt, nincs szükség manuális újratöltésre.
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - Foglalások valós idejű figyelésének indítása.");
        if (currentUser != null) { // Csak akkor indítsuk a listenert, ha van felhasználó
            loadUserAppointmentsRealtime();
        } else {
            // Kezelheted itt is, ha a currentUser null, bár az onCreate-ben már van erre logika
            Log.w(TAG, "onStart: currentUser null, nem indul a listener.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appointmentsListenerRegistration != null) {
            appointmentsListenerRegistration.remove();
            Log.d(TAG, "onStop - Firestore listener eltávolítva.");
        }
    }
}
