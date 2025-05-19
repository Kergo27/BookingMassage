package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull; // Szükséges a Task<QuerySnapshot> @NonNull-hez
import androidx.appcompat.app.AppCompatActivity;
// Szükséges lehet, ha MaterialToolbart használsz action barként
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.MaterialToolbar;

import com.example.bookingmassage.TimeSlot; // A modell importja (módosítottam a csomagnevet a konzisztencia érdekében)
import com.example.bookingmassage.FirebaseHelper; // FirebaseHelper importja
import com.example.bookingmassage.NotificationHelper; // NotificationHelper importja

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseHelper firebaseHelper;
    private NotificationHelper notificationHelper;

    // UI Elemek
    private Button btnBookAppointment;
    private Button btnMyBookings;
    private Button btnLogout;
    private Button btnFindNextSlots;
    private TextView tvNextAvailableSlotsResult;
    private Button btnAdminPanel; // ÚJ: Admin panel gomb

    // Callback interfész az admin ellenőrzéshez
    public interface OnAdminCheckCompleteListener {
        void onResult(boolean isAdmin);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Győződj meg róla, hogy a layout neve helyes és tartalmazza az összes gombot
        setContentView(R.layout.activity_homepage); // Az előző kérésben activity_homepage volt, most activity_home
        Log.d(TAG, "onCreate elindult.");

        // Firebase inicializálás
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firebaseHelper = new FirebaseHelper();
        notificationHelper = new NotificationHelper(this);

        // Felhasználó ellenőrzése
        if (currentUser == null) {
            Log.w(TAG, "Nincs bejelentkezett felhasználó, átirányítás a LoginActivity-re.");
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Log.d(TAG, "Bejelentkezett felhasználó: " + currentUser.getEmail() + " (UID: " + currentUser.getUid() + ")");

        // UI Elemek inicializálása
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnMyBookings = findViewById(R.id.btnMyBookings);
        btnLogout = findViewById(R.id.btnLogout);
        btnFindNextSlots = findViewById(R.id.btnFindNextSlots);
        tvNextAvailableSlotsResult = findViewById(R.id.tvNextAvailableSlotsResult);
        btnAdminPanel = findViewById(R.id.btnAdminPanel); // Admin gomb inicializálása

        // Null ellenőrzések (opcionális, de hasznos)
        if (btnBookAppointment == null || btnMyBookings == null || btnLogout == null || btnFindNextSlots == null) {
            Log.e(TAG, "Hiba: Egy vagy több fő gomb nem található az activity_home.xml-ben!");
        }
        if (btnAdminPanel == null) {
            Log.e(TAG, "HIBA: btnAdminPanel nem található az activity_home.xml-ben!");
        }

        // Admin gomb alapértelmezett elrejtése és listener beállítása
        if (btnAdminPanel != null) {
            btnAdminPanel.setVisibility(View.GONE); // Alapból rejtett
            btnAdminPanel.setOnClickListener(v -> {
                Log.d(TAG, "Admin Panel gomb megnyomva.");
                startActivity(new Intent(HomeActivity.this, AdminPanelActivity.class));
            });
        }

        // Admin státusz ellenőrzése és a UI frissítése
        checkIfUserIsAdminAndSetupUI(currentUser);


        // IDEIGLENES: Időpontok generálása
        // firebaseHelper.generateTimeSlotsForMonths(1); // KOMMENTELD KI HASZNÁLAT UTÁN


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
                mAuth.signOut();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (btnFindNextSlots != null) {
            btnFindNextSlots.setOnClickListener(v -> {
                Log.d(TAG, "Legközelebbi szabad időpontok keresése gomb megnyomva.");
                if (tvNextAvailableSlotsResult != null) {
                    tvNextAvailableSlotsResult.setText("Keresés folyamatban...");
                } else {
                    Toast.makeText(this, "Keresés folyamatban...", Toast.LENGTH_SHORT).show();
                }

                firebaseHelper.getNextXAvailableTimeSlots(null, 3, new FirebaseHelper.OnTimeSlotsLoadedListener() {
                    @Override
                    public void onLoaded(List<TimeSlot> timeSlots) {
                        if (timeSlots.isEmpty()) {
                            Log.i(TAG, "Nincsenek elérhető szabad időpontok.");
                            if (tvNextAvailableSlotsResult != null) {
                                tvNextAvailableSlotsResult.setText("Nincsenek közeli szabad időpontok.");
                            }
                            notificationHelper.showAvailableSlotsNotification(
                                    "Szabad Időpontok",
                                    "Jelenleg nincsenek közeli szabad időpontok.",
                                    0
                            );
                        } else {
                            Log.i(TAG, "Talált szabad időpontok (" + timeSlots.size() + " db).");
                            StringBuilder resultText = new StringBuilder("Legközelebbi szabadok:\n");
                            for (int i = 0; i < timeSlots.size(); i++) {
                                TimeSlot slot = timeSlots.get(i);
                                resultText.append(slot.getDate()).append(" ").append(slot.getTime());
                                if (i < timeSlots.size() - 1) {
                                    resultText.append("\n");
                                }
                            }
                            if (tvNextAvailableSlotsResult != null) {
                                tvNextAvailableSlotsResult.setText(resultText.toString());
                            }
                            notificationHelper.showAvailableSlotsNotificationWithBigText(
                                    "Talált Szabad Időpontok!",
                                    "Legközelebbi szabadok:",
                                    resultText.toString(),
                                    1
                            );
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Hiba a legközelebbi szabad időpontok lekérdezésekor: ", e);
                        if (tvNextAvailableSlotsResult != null) {
                            tvNextAvailableSlotsResult.setText("Hiba az időpontok keresésekor.");
                        }
                        notificationHelper.showAvailableSlotsNotification(
                                "Hiba történt",
                                "Nem sikerült lekérdezni a szabad időpontokat.",
                                2
                        );
                        Toast.makeText(HomeActivity.this, "Hiba: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }

    private void checkIfUserIsAdmin(FirebaseUser user, final OnAdminCheckCompleteListener listener) {
        if (user == null) {
            Log.w(TAG, "checkIfUserIsAdmin: user null, nem lehet ellenőrizni.");
            if (listener != null) listener.onResult(false);
            return;
        }
        String userUid = user.getUid();
        Log.d(TAG, "Admin státusz ellenőrzése a felhasználóhoz: " + userUid);

        FirebaseFirestore.getInstance().collection("admins").document(userUid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() { // Explicit típusmegadás
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                Log.i(TAG, "Admin ellenőrzés: Felhasználó (" + userUid + ") admin (dokumentum létezik).");
                                if (listener != null) listener.onResult(true);
                            } else {
                                Log.i(TAG, "Admin ellenőrzés: Felhasználó (" + userUid + ") NEM admin (nincs dokumentum).");
                                if (listener != null) listener.onResult(false);
                            }
                        } else {
                            Log.e(TAG, "Hiba az admin státusz Firestore lekérdezésekor: ", task.getException());
                            if (listener != null) listener.onResult(false);
                        }
                    }
                });
    }

    private void checkIfUserIsAdminAndSetupUI(FirebaseUser user) {
        if (btnAdminPanel == null) {
            Log.w(TAG, "checkIfUserIsAdminAndSetupUI: btnAdminPanel null, nem lehet beállítani a láthatóságot.");
            return;
        }
        checkIfUserIsAdmin(user, isAdmin -> {
            if (isAdmin) {
                Log.i(TAG, "Felhasználó admin, admin panel gomb MEGJELENÍTÉSE.");
                btnAdminPanel.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "Felhasználó NEM admin, admin panel gomb REJTVE MARAD.");
                btnAdminPanel.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart meghívva.");
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null && !isFinishing()) {
            Log.w(TAG, "onStart: Nincs bejelentkezett felhasználó, átirányítás a LoginActivity-re.");
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (currentUser != null) {
            // Ha a felhasználó be van jelentkezve, újra ellenőrizzük az admin státuszt,
            // hátha az onCreate-ben valamiért nem futott le helyesen vagy az adatok változtak.
            // De ez csak akkor kell, ha az admin státusz dinamikusan változhat az app futása közben.
            // Egyszeri ellenőrzés az onCreate-ben általában elég.
            // checkIfUserIsAdminAndSetupUI(currentUser); // Ezt csak akkor, ha szükséges az onStart-ban is frissíteni
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
