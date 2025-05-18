package com.example.bookingmassage; // Győződj meg róla, hogy a csomagnév helyes

import android.content.Intent;
import android.content.SharedPreferences; // Ha használod a felhasználónév mentéséhez
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Fontos a hibakereséshez
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar; // Ha használsz ProgressBar-t
import android.widget.TextView; // A tvRegister miatt
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getName();
    // ... (a többi konstansod, ha vannak)

    private EditText etEmail, etPassword; // Korábban usernameET, passwordET volt, az XML-hez igazítva
    private Button btnLogin; // Korábban loginB volt
    private TextView tvRegister; // A regisztrációs TextView
    private ImageView ivLogo; // Korábban headerImage volt
    // private ProgressBar progressBar; // Ha van ProgressBar a layoutban

    private FirebaseAuth mAuth;
    // private SharedPreferences preferences; // Ha használod

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Győződj meg róla, hogy ez a layoutod neve

        mAuth = FirebaseAuth.getInstance();

        // View-k inicializálása
        etEmail = findViewById(R.id.etEmail); // Az XML-ben lévő ID alapján
        etPassword = findViewById(R.id.etPassword); // Az XML-ben lévő ID alapján
        btnLogin = findViewById(R.id.btnLogin); // Az XML-ben lévő ID alapján
        tvRegister = findViewById(R.id.tvRegister); // Az XML-ben lévő ID alapján
        ivLogo = findViewById(R.id.ivLogo); // Az XML-ben lévő ID alapján
        // progressBar = findViewById(R.id.progressBarLogin); // Ha van

        // preferences = getSharedPreferences(PREF_KEY, MODE_PRIVATE); // Ha használod

        // OnClickListener a bejelentkezés gombra
        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Bejelentkezés gomb megnyomva.");
                    loginUser(); // Vagy a loginNormal() metódusod, ha azt használod
                }
            });
        } else {
            Log.e(LOG_TAG, "btnLogin (Bejelentkezés gomb) nem található!");
        }

        // OnClickListener a regisztráció TextView-ra
        if (tvRegister != null) {
            tvRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Regisztráció link/gomb megnyomva."); // Log üzenet hibakereséshez
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            Log.e(LOG_TAG, "tvRegister (Regisztráció link/gomb) nem található!");
            Toast.makeText(this, "Hiba: Regisztrációs link nem található.", Toast.LENGTH_SHORT).show();
        }


        // Animációk (ha vannak és a view-k léteznek)
        if (btnLogin != null) {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            btnLogin.startAnimation(fadeIn);
        }
        if (ivLogo != null) {
            ivLogo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink));
        }
    }

    // A bejelentkezési logikád (loginUser vagy loginNormal)
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email megadása kötelező.");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Érvénytelen email formátum.");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Jelszó megadása kötelező.");
            etPassword.requestFocus();
            return;
        }

        // setInProgress(true); // Ha van progressbar
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // setInProgress(false); // Ha van progressbar
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "Sikeres bejelentkezés.");
                            // Felhasználónév mentése, ha kell
                            // SharedPreferences.Editor editor = preferences.edit();
                            // editor.putString(USERNAME_KEY, email);
                            // editor.apply();
                            goToHomepage();
                        } else {
                            Log.w(LOG_TAG, "Bejelentkezés sikertelen", task.getException());
                            handleLoginFailure(task.getException());
                        }
                    }
                });
    }

    private void handleLoginFailure(Exception exception) {
        String errorMessage;
        if (exception instanceof FirebaseAuthInvalidUserException) {
            errorMessage = "Nincs ilyen email címmel regisztrált felhasználó.";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            errorMessage = "Hibás jelszó.";
        } else {
            errorMessage = "Bejelentkezési hiba. Kérjük, ellenőrizze az adatait vagy próbálja meg később.";
            if (exception != null && exception.getLocalizedMessage() != null) {
                Log.e(LOG_TAG, "Login exception: " + exception.getLocalizedMessage());
            }
        }
        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }


    private void goToHomepage() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish(); // Bezárjuk a LoginActivity-t
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(LOG_TAG, "Felhasználó már bejelentkezve, átirányítás a főoldalra.");
            goToHomepage();
        }
    }

    // ... (a többi metódusod: onPause, stb.)
}
