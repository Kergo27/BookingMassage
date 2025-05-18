package com.example.bookingmassage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private FirebaseAuth mAuth;
    private FirebaseHelper firebaseHelper;
    // private FirebaseHelper firebaseHelper; // Ha szükség van rá itt is

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        Log.d(TAG, "onCreate");

        mAuth = FirebaseAuth.getInstance();
        //firebaseHelper = new FirebaseHelper();
        //firebaseHelper.generateInitialTimeSlotsForUpcomingWeek(); // TESZTELÉSHEZ EGYSZER FUTTASD, MAJD KOMMENTELD KI!

        //firebaseHelper.generateInitialTimeSlotsForUpcomingWeek();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button btnBookAppointment = findViewById(R.id.btnBookAppointment); // Új ID az XML-ben
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button btnMyBookings = findViewById(R.id.btnMyBookings);       // Új ID az XML-ben
        Button btnLogout = findViewById(R.id.btnLogout);               // Új ID az XML-ben

        btnBookAppointment.setOnClickListener(v -> {
            Log.d(TAG, "Időpontfoglalás gomb megnyomva.");
            startActivity(new Intent(HomeActivity.this, BookingActivity.class));
        });

        btnMyBookings.setOnClickListener(v -> {
            Log.d(TAG, "Foglalásaim gomb megnyomva.");
            startActivity(new Intent(HomeActivity.this, AppointmentsActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Kijelentkezés gomb megnyomva.");
            mAuth.signOut();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Nincs bejelentkezett felhasználó, visszairányítás a LoginActivity-re
            Log.d(TAG, "Nincs bejelentkezett felhasználó, visszairányítás a LoginActivity-re.");
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "Felhasználó bejelentkezve: " + currentUser.getEmail());
        }
    }

    // Lifecycle Hookok (onPause, onResume) itt is lehetnek, ha szükséges
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }
}
