package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bookingmassage.User; // User modell importálása
import com.example.bookingmassage.FirebaseHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest; // Név frissítéséhez

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputLayout tilNameRegister, tilEmailRegister, tilPasswordRegister, tilConfirmPasswordRegister;
    private TextInputEditText etNameRegister, etEmailRegister, etPasswordRegister, etConfirmPasswordRegister;
    private Button btnRegister, btnGoToLogin;
    private ProgressBar progressBarRegister;
    private MaterialToolbar toolbarRegister;

    private FirebaseAuth mAuth;
    private FirebaseHelper firebaseHelper;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Győződj meg róla, hogy a layout neve helyes
        Log.d(TAG, "onCreate elindult.");

        mAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        // UI Elemek inicializálása
        toolbarRegister = findViewById(R.id.toolbarRegister);
        tilNameRegister = findViewById(R.id.tilNameRegister); // ÚJ
        etNameRegister = findViewById(R.id.etNameRegister);   // ÚJ
        tilEmailRegister = findViewById(R.id.tilEmailRegister);
        etEmailRegister = findViewById(R.id.etEmailRegister);
        tilPasswordRegister = findViewById(R.id.tilPasswordRegister);
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        tilConfirmPasswordRegister = findViewById(R.id.tilConfirmPasswordRegister);
        etConfirmPasswordRegister = findViewById(R.id.etConfirmPasswordRegister);
        btnRegister = findViewById(R.id.btnRegisterAction);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        progressBarRegister = findViewById(R.id.progressBarRegister);

        setSupportActionBar(toolbarRegister);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        btnRegister.setOnClickListener(v -> registerUser());

        btnGoToLogin.setOnClickListener(v -> {
            // startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            // Jobb, ha finish()-t hívunk, hogy ne maradjon a back stack-ben, ha a LoginActivity-ből jöttünk.
            onBackPressed();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void registerUser() {
        Log.d(TAG, "registerUser metódus elindult.");
        // Hibák törlése
        tilNameRegister.setError(null);
        tilEmailRegister.setError(null);
        tilPasswordRegister.setError(null);
        tilConfirmPasswordRegister.setError(null);

        String name = etNameRegister.getText() != null ? etNameRegister.getText().toString().trim() : "";
        String email = etEmailRegister.getText() != null ? etEmailRegister.getText().toString().trim() : "";
        String password = etPasswordRegister.getText() != null ? etPasswordRegister.getText().toString().trim() : "";
        String confirmPassword = etConfirmPasswordRegister.getText() != null ? etConfirmPasswordRegister.getText().toString().trim() : "";

        // Validáció
        boolean isValid = true;
        if (TextUtils.isEmpty(name)) {
            tilNameRegister.setError("A név megadása kötelező!");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmailRegister.setError("Az e-mail cím megadása kötelező!");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmailRegister.setError("Érvénytelen e-mail cím formátum!");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPasswordRegister.setError("A jelszó megadása kötelező!");
            isValid = false;
        } else if (password.length() < 6) {
            tilPasswordRegister.setError("A jelszónak legalább 6 karakter hosszúnak kell lennie!");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPasswordRegister.setError("A jelszó megerősítése kötelező!");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPasswordRegister.setError("A két jelszó nem egyezik!");
            isValid = false;
        }

        if (!isValid) {
            Log.w(TAG, "Validációs hibák a regisztráció során.");
            return;
        }

        Log.d(TAG, "Validáció sikeres, regisztráció Firebase-be...");
        progressBarRegister.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Név frissítése a Firebase Auth profilban
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "Felhasználói profil (név) sikeresen frissítve Firebase Auth-ban.");
                                        } else {
                                            Log.w(TAG, "Hiba a felhasználói profil (név) frissítésekor Firebase Auth-ban.", profileTask.getException());
                                        }
                                    });

                            // Felhasználó mentése a Firestore-ba a FirebaseHelper segítségével
                            User newUser = new User(firebaseUser.getUid(), email, name); // Átadjuk a nevet
                            firebaseHelper.createUser(newUser, new FirebaseHelper.OnUserCreationListener() {
                                @Override
                                public void onSuccess() {
                                    progressBarRegister.setVisibility(View.GONE);
                                    btnRegister.setEnabled(true);
                                    Log.i(TAG, "Felhasználó sikeresen létrehozva és mentve Firestore-ba.");
                                    Toast.makeText(RegisterActivity.this, "Sikeres regisztráció!", Toast.LENGTH_SHORT).show();
                                    // Navigálás a HomeActivity-re, és a Login/Register Activity-k bezárása
                                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    progressBarRegister.setVisibility(View.GONE);
                                    btnRegister.setEnabled(true);
                                    Log.e(TAG, "Hiba a felhasználó Firestore-ba mentésekor.", e);
                                    Toast.makeText(RegisterActivity.this, "Hiba a felhasználói adatok mentésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    // Itt a felhasználó már létrejött Firebase Auth-ban, de Firestore-ba nem sikerült menteni.
                                    // Kezelni kellene ezt az esetet (pl. próbálja újra, vagy töröljük az Auth usert).
                                    // Egyelőre csak hibaüzenetet jelenítünk meg.
                                }
                            });
                        } else {
                            // Ez nem szabadna, hogy megtörténjen, ha task.isSuccessful() true
                            progressBarRegister.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            Log.e(TAG, "FirebaseUser null a sikeres createUserWithEmailAndPassword után.");
                            Toast.makeText(RegisterActivity.this, "Regisztráció sikertelen (belső hiba).", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Ha a regisztráció sikertelen, jelenítsd meg az üzenetet.
                        progressBarRegister.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Regisztráció sikertelen: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Ismeretlen hiba."),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
