package com.example.bookingmassage; // Győződj meg róla, hogy a csomagnév helyes

import android.os.Bundle;
import android.util.Log; // Hozzáadva logoláshoz
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Importáld a saját modell és helper osztályaidat
import com.example.bookingmassage.Appointment;
import com.example.bookingmassage.AppointmentAdapter;
import com.example.bookingmassage.FirebaseHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query; // Query importálása
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsActivity extends AppCompatActivity {
    private static final String TAG = "AppointmentsActivity"; // Logoláshoz TAG

    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private FirebaseHelper dbHelper;
    private ValueEventListener appointmentsListener; // Listener referencia a későbbi eltávolításhoz
    private Query userAppointmentsQuery; // Query referencia a listener eltávolításához

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments); // Győződj meg róla, hogy az ID helyes: R.id.rvAppointments

        // Helyes ID használata a RecyclerView-hoz az XML alapján
        recyclerView = findViewById(R.id.rvAppointments);
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView (rvAppointments) nem található a layoutban!");
            Toast.makeText(this, "Hiba történt az elrendezés betöltésekor.", Toast.LENGTH_LONG).show();
            finish(); // Lépjünk ki, ha a RecyclerView nincs meg
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true); // Teljesítményjavítás, ha az elemek mérete nem változik

        dbHelper = new FirebaseHelper();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadAppointments(userId);
        } else {
            Toast.makeText(this, "Felhasználó nincs bejelentkezve.", Toast.LENGTH_LONG).show();
            // Ideális esetben visszanavigálna a bejelentkezési képernyőre
            finish();
        }
    }

    private void loadAppointments(String userId) {
        List<Appointment> appointmentsList = new ArrayList<>();
        adapter = new AppointmentAdapter(appointmentsList, this); // Context átadása, ha szükséges az adapterben
        recyclerView.setAdapter(adapter);

        userAppointmentsQuery = dbHelper.getUserAppointments(userId); // Query referencia elmentése

        appointmentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentsList.clear(); // Lista törlése az új adatok előtt
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Appointment appointment = ds.getValue(Appointment.class);
                        if (appointment != null) {
                            // Opcionális: Ellenőrizzük, hogy az appointmentId a Firebase kulcsával egyezik-e
                            // Ha a FirebaseHelperben a push().getKey()-t használod és beállítod, akkor jó lesz.
                            // appointment.setId(ds.getKey()); // Ha az ID-t a Firebase kulcsból akarod venni
                            appointmentsList.add(appointment);
                        }
                    }
                } else {
                    Log.d(TAG, "Nincsenek foglalások ehhez a felhasználóhoz.");
                    Toast.makeText(AppointmentsActivity.this, "Nincsenek foglalásaid.", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged(); // Adapter frissítése
                if (appointmentsList.isEmpty()) {
                    // Kezelheted itt, ha a lista üres a feldolgozás után
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Adatbázis hiba: " + error.getMessage());
                Toast.makeText(AppointmentsActivity.this, "Hiba az adatok lekérdezésekor: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        userAppointmentsQuery.addValueEventListener(appointmentsListener); // Listener hozzáadása a query-hez
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Fontos: Távolítsd el a listenert, hogy elkerüld a memóriaszivárgást,
        // ha az Activity megsemmisül, de a listener még mindig figyel.
        if (userAppointmentsQuery != null && appointmentsListener != null) {
            userAppointmentsQuery.removeEventListener(appointmentsListener);
            Log.d(TAG, "Appointments listener eltávolítva.");
        }
    }
}
