package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // Szükséges lehet, ha OnClickListener-t implementálsz az osztály szintjén
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
// Szükséges lehet, ha MaterialToolbart használsz action barként
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.MaterialToolbar;


import com.example.bookingmassage.TimeSlot; // Szükséges a TimeSlot-hoz
import com.example.bookingmassage.FirebaseHelper;
import com.example.bookingmassage.NotificationHelper; // Ha használod
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseHelper firebaseHelper;
    private NotificationHelper notificationHelper; // Ha használtad az értesítésekhez

    // UI Elemek
    private Button btnBookAppointment;
    private Button btnMyBookings;
    private Button btnLogout;
    private Button btnFindNextSlots;
    private TextView tvNextAvailableSlotsResult; // Az eredmények megjelenítéséhez (ha nem csak értesítés van)
    // private MaterialToolbar toolbarHome; // Ha van MaterialToolbar az XML-ben, és kezelni akarod

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage); // Győződj meg róla, hogy ez a layout fájl neve
        Log.d(TAG, "onCreate elindult.");

        // Firebase inicializálás
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firebaseHelper = new FirebaseHelper(); // Ennek Firestore logikát kell tartalmaznia
        notificationHelper = new NotificationHelper(this); // Ha értesítéseket használsz

        // Felhasználó ellenőrzése
        if (currentUser == null) {
            Log.w(TAG, "Nincs bejelentkezett felhasználó, átirányítás a LoginActivity-re.");
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Fontos, hogy a HomeActivity ne maradjon a back stack-ben
            return; // Kilépünk az onCreate-ből, ha nincs felhasználó
        }
        Log.d(TAG, "Bejelentkezett felhasználó: " + currentUser.getEmail());

        // UI Elemek inicializálása
        // toolbarHome = findViewById(R.id.toolbarHome); // Ha van toolbarod az XML-ben
        // if (toolbarHome != null) {
        //    setSupportActionBar(toolbarHome);
        //    getSupportActionBar().setTitle("Főoldal"); // Vagy @string/home_title
        // }

        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnMyBookings = findViewById(R.id.btnMyBookings);
        btnLogout = findViewById(R.id.btnLogout);
        btnFindNextSlots = findViewById(R.id.btnFindNextSlots);
        tvNextAvailableSlotsResult = findViewById(R.id.tvNextAvailableSlotsResult); // Ha van ilyen TextView az XML-ben

        // Null ellenőrzés a kritikus UI elemekre (opcionális, de jó gyakorlat)
        if (btnBookAppointment == null || btnMyBookings == null || btnLogout == null || btnFindNextSlots == null) {
            Log.e(TAG, "Hiba: Egy vagy több fő gomb (btnBookAppointment, btnMyBookings, btnLogout, btnFindNextSlots) nem található az activity_home.xml-ben!");
            Toast.makeText(this, "Hiba a felület elemeinek betöltésekor.", Toast.LENGTH_LONG).show();
            // Itt dönthetsz úgy, hogy az app nem működőképes, és finish()-t hívsz, vagy csak logolsz.
        }
        if (tvNextAvailableSlotsResult == null && notificationHelper == null) {
            Log.w(TAG, "Figyelmeztetés: tvNextAvailableSlotsResult TextView és notificationHelper is null. Az eredmények nem fognak megjelenni.");
        }


        // IDEIGLENES: Időpontok generálása (ha még nem történt meg Firestore-ba)
        // FIGYELEM: EZT CSAK EGYSZER FUTTASD AZ ADATBÁZIS FELTÖLTÉSÉHEZ!
        // MIUTÁN LEFUTOTT ÉS LÁTOD AZ ADATOKAT A FIREBASE KONZOLBAN, KOMMENTELD KI VAGY TÖRÖLD!
        // ---------------------------------------------------------------------------
        // firebaseHelper.generateTimeSlotsForMonths(1); // Pl. 1 hónapra
        // ---------------------------------------------------------------------------


        // Gomb Listenerek Beállítása
        if (btnBookAppointment != null) {
            btnBookAppointment.setOnClickListener(v -> {
                Log.d(TAG, "Időpontfoglalás gomb megnyomva.");
                startActivity(new Intent(HomeActivity.this, BookingActivity.class));
            });
        }

        if (btnMyBookings != null) {
            btnMyBookings.setOnClickListener(v -> {
                Log.d(TAG, "Foglalásaim gomb megnyomva.");
                startActivity(new Intent(HomeActivity.this, AppointmentsActivity.class));
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Log.d(TAG, "Kijelentkezés gomb megnyomva.");
                mAuth.signOut(); // Firebase kijelentkezés
                // Opcionálisan: Google Sign-Out, ha azt is használtad
                // if (googleSignInClient != null) {
                //     googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                //         Log.d(TAG, "Google felhasználó kijelentkeztetve.");
                //     });
                // }
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Törli a back stack-et
                startActivity(intent);
                finish(); // Bezárja a HomeActivity-t
            });
        }

        // "Legközelebbi Szabad Időpont Keresése" gomb listener
        if (btnFindNextSlots != null) {
            btnFindNextSlots.setOnClickListener(v -> {
                Log.d(TAG, "Legközelebbi szabad időpontok keresése gomb megnyomva.");
                if (tvNextAvailableSlotsResult != null) {
                    tvNextAvailableSlotsResult.setText("Keresés folyamatban..."); // Visszajelzés a UI-n
                } else {
                    Toast.makeText(this, "Keresés folyamatban...", Toast.LENGTH_SHORT).show();
                }


                // Mai naptól kezdve az első 3 szabad időpont lekérése (vagy amennyit szeretnél)
                firebaseHelper.getNextXAvailableTimeSlots(null, 3, new FirebaseHelper.OnTimeSlotsLoadedListener() {
                    @Override
                    public void onLoaded(List<TimeSlot> timeSlots) {
                        if (timeSlots.isEmpty()) {
                            Log.i(TAG, "Nincsenek elérhető szabad időpontok a közeljövőben.");
                            if (tvNextAvailableSlotsResult != null) {
                                tvNextAvailableSlotsResult.setText("Nincsenek közeli szabad időpontok.");
                            }
                            if (notificationHelper != null) {
                                notificationHelper.showAvailableSlotsNotification(
                                        "Szabad Időpontok",
                                        "Jelenleg nincsenek közeli szabad időpontok.",
                                        0 // Notification ID offset
                                );
                            }
                        } else {
                            Log.i(TAG, "Talált szabad időpontok (" + timeSlots.size() + " db):");
                            StringBuilder resultText = new StringBuilder("Legközelebbi szabadok:\n");
                            for (int i = 0; i < timeSlots.size(); i++) {
                                TimeSlot slot = timeSlots.get(i);
                                Log.d(TAG, " - " + slot.getDate() + " " + slot.getTime());
                                resultText.append(slot.getDate()).append(" ").append(slot.getTime());
                                if (i < timeSlots.size() - 1) {
                                    resultText.append("\n");
                                }
                            }

                            if (tvNextAvailableSlotsResult != null) {
                                tvNextAvailableSlotsResult.setText(resultText.toString());
                            }
                            if (notificationHelper != null) {
                                notificationHelper.showAvailableSlotsNotificationWithBigText(
                                        "Talált Szabad Időpontok!",
                                        "Legközelebbi szabadok:", // Rövid tartalom
                                        resultText.toString(),    // Hosszú tartalom (BigTextStyle)
                                        1 // Notification ID offset
                                );
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Hiba a legközelebbi szabad időpontok lekérdezésekor: ", e);
                        if (tvNextAvailableSlotsResult != null) {
                            tvNextAvailableSlotsResult.setText("Hiba az időpontok keresésekor.");
                        }
                        if (notificationHelper != null) {
                            notificationHelper.showAvailableSlotsNotification(
                                    "Hiba történt",
                                    "Nem sikerült lekérdezni a szabad időpontokat.",
                                    2 // Notification ID offset
                            );
                        }
                        Toast.makeText(HomeActivity.this, "Hiba az időpontok keresésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart meghívva.");
        // Ellenőrizzük újra a felhasználót, ha esetleg valamiért null lett az onCreate után
        // (pl. ha a LoginActivity nem zárta be magát, és visszanavigáltunk ide)
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null && !isFinishing()) { // Ha nincs user és az activity még nem fejeződik be
            Log.w(TAG, "onStart: Nincs bejelentkezett felhasználó, átirányítás a LoginActivity-re.");
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause meghívva.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop meghívva.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy meghívva.");
    }
}
