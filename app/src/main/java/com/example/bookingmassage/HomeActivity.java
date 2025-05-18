package com.example.bookingmassage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
// import android.widget.TextView; // Ezt most nem használjuk az eredmény közvetlen megjelenítésére
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookingmassage.TimeSlot;
import com.example.bookingmassage.FirebaseHelper;
import com.example.bookingmassage.NotificationHelper; // ÚJ IMPORT
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseHelper firebaseHelper;
    private NotificationHelper notificationHelper; // ÚJ

    private Button btnBookAppointment, btnMyBookings, btnLogout;
    private Button btnFindNextSlots;
    // private TextView tvNextAvailableSlotsResult; // Ezt most nem használjuk

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        Log.d(TAG, "onCreate elindult.");

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firebaseHelper = new FirebaseHelper();
        notificationHelper = new NotificationHelper(this); // NotificationHelper inicializálása

        if (currentUser == null) {
            // ... (átirányítás)
            return;
        }

        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnMyBookings = findViewById(R.id.btnMyBookings);
        btnLogout = findViewById(R.id.btnLogout);
        btnFindNextSlots = findViewById(R.id.btnFindNextSlots); // Győződj meg róla, hogy ez az ID létezik az XML-ben
        // tvNextAvailableSlotsResult = findViewById(R.id.tvNextAvailableSlotsResult); // Ezt most nem használjuk

        // ... (gomb listenerek a többi gombhoz)

        if (btnFindNextSlots != null) {
            btnFindNextSlots.setOnClickListener(v -> {
                Log.d(TAG, "btnFindNextSlots gomb megnyomva.");
                Toast.makeText(this, "Legközelebbi szabad időpontok keresése...", Toast.LENGTH_SHORT).show();

                // Mai naptól kezdve az első 3 szabad időpont lekérése
                firebaseHelper.getNextXAvailableTimeSlots(null, 3, new FirebaseHelper.OnTimeSlotsLoadedListener() {
                    @Override
                    public void onLoaded(List<TimeSlot> timeSlots) {
                        if (timeSlots.isEmpty()) {
                            Log.i(TAG, "Nincsenek elérhető szabad időpontok a közeljövőben.");
                            notificationHelper.showAvailableSlotsNotification(
                                    "Szabad Időpontok",
                                    "Jelenleg nincsenek közeli szabad időpontok.",
                                    0 // Egyedi ID offset az értesítéshez
                            );
                        } else {
                            Log.i(TAG, "Talált szabad időpontok (" + timeSlots.size() + " db):");
                            StringBuilder notificationMessage = new StringBuilder("Legközelebbi szabad időpontok:\n");
                            for (int i = 0; i < timeSlots.size(); i++) {
                                TimeSlot slot = timeSlots.get(i);
                                Log.d(TAG, " - " + slot.getDate() + " " + slot.getTime());
                                notificationMessage.append(slot.getDate()).append(" ").append(slot.getTime()).append("\n");
                            }
                            // Csak egy értesítést küldünk a listával
                            notificationHelper.showAvailableSlotsNotification(
                                    "Talált Szabad Időpontok!",
                                    notificationMessage.toString().trim(), // Levágjuk a felesleges utolsó newline-t
                                    1 // Egyedi ID offset az értesítéshez
                            );
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Hiba a legközelebbi szabad időpontok lekérdezésekor: ", e);
                        notificationHelper.showAvailableSlotsNotification(
                                "Hiba történt",
                                "Nem sikerült lekérdezni a szabad időpontokat.",
                                2 // Egyedi ID offset az értesítéshez
                        );
                        Toast.makeText(HomeActivity.this, "Hiba: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }
    // ... (többi HomeActivity kód)
}
