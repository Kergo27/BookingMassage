package com.example.bookingmassage;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookingmassage.User;
import com.example.bookingmassage.FirebaseHelper;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword, etRegisterPhoneNumber;
    private Button btnRegisterAction;
    private TextView tvBackToLogin;
    private ProgressBar progressBarRegister;

    private FirebaseAuth mAuth;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Log.d(TAG, "onCreate elindult.");

        mAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        etRegisterPhoneNumber = findViewById(R.id.etRegisterPhoneNumber);
        btnRegisterAction = findViewById(R.id.btnRegisterAction);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBarRegister = findViewById(R.id.progressBarRegister);

        if (btnRegisterAction != null) {
            btnRegisterAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Regisztráció gomb (btnRegisterAction listener) megnyomva.");
                    performRegistration();
                }
            });
        } else {
            Log.e(TAG, "btnRegisterAction (Regisztráció gomb) nem található az XML-ben!");
        }

        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Vissza a bejelentkezéshez link (tvBackToLogin listener) megnyomva.");
                    onBackToLoginClick(v);
                }
            });
        } else {
            Log.e(TAG, "tvBackToLogin (Vissza a bejelentkezéshez link) nem található az XML-ben!");
        }
    }

    public void onPerformRegistrationClick(View view) {
        Log.d(TAG, "Regisztráció gomb (onPerformRegistrationClick XML metódus) megnyomva.");
        performRegistration();
    }

    public void onBackToLoginClick(View view) {
        Log.d(TAG, "Vissza a bejelentkezéshez link (onBackToLoginClick XML metódus) megnyomva.");
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    private void performRegistration() {
        Log.d(TAG, "performRegistration metódus elindult.");
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etRegisterConfirmPassword.getText().toString().trim();
        String phoneNumber = etRegisterPhoneNumber.getText().toString().trim(); // phoneNumber változó feltételezve

        // Validációk (feltételezzük, hogy ezek rendben vannak, és hiba esetén return-nelnek)
        if (TextUtils.isEmpty(email)) {
            etRegisterEmail.setError("Email cím megadása kötelező.");
            etRegisterEmail.requestFocus();
            Log.w(TAG, "Validáció sikertelen: Email üres.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError("Kérjük, érvényes email címet adjon meg.");
            etRegisterEmail.requestFocus();
            Log.w(TAG, "Validáció sikertelen: Email formátum hibás.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError("Jelszó megadása kötelező.");
            etRegisterPassword.requestFocus();
            Log.w(TAG, "Validáció sikertelen: Jelszó üres.");
            return;
        }
        if (password.length() < 6) {
            etRegisterPassword.setError("A jelszónak legalább 6 karakter hosszúnak kell lennie.");
            etRegisterPassword.requestFocus();
            Log.w(TAG, "Validáció sikertelen: Jelszó túl rövid.");
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            etRegisterConfirmPassword.setError("Jelszó megerősítése kötelező.");
            etRegisterConfirmPassword.requestFocus();
            Log.w(TAG, "Validáció sikertelen: Jelszó megerősítése üres.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etRegisterConfirmPassword.setError("A két jelszó nem egyezik.");
            etRegisterConfirmPassword.requestFocus();
            etRegisterPassword.setText("");
            etRegisterConfirmPassword.setText("");
            Log.w(TAG, "Validáció sikertelen: Jelszavak nem egyeznek.");
            return;
        }
        if (!TextUtils.isEmpty(phoneNumber) && !Patterns.PHONE.matcher(phoneNumber).matches()) {
            etRegisterPhoneNumber.setError("Kérjük, érvényes telefonszámot adjon meg (opcionális).");
            etRegisterPhoneNumber.requestFocus();
            Log.w(TAG, "Validáció: Telefonszám formátum hibás (de opcionális, a folyamat megy tovább).");
        }

        Log.d(TAG, "Validáció sikeres. ProgressBar mutatása, Firebase Auth hívás indul.");
        setInProgress(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "Firebase Auth onComplete. Sikeres: " + task.isSuccessful());
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser(); // A felhasználó már be van jelentkezve
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();
                                Log.d(TAG, "Auth sikeres, UID: " + userId + ". FirebaseHelper.createUser hívás indul.");
                                User newUser = new User(userId, email, phoneNumber);

                                // RegisterActivity.java - a releváns rész a FirebaseHelper.OnUserCreationListener-en belül

                                firebaseHelper.createUser(newUser, new FirebaseHelper.OnUserCreationListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "RegisterActivity: FirebaseHelper.OnUserCreationListener onSuccess MEGHÍVVA.");
                                        setInProgress(false);
                                        Toast.makeText(RegisterActivity.this, "Sikeres regisztráció!", Toast.LENGTH_LONG).show();

                                        // KÖZVETLEN NAVIGÁCIÓ A HOMEACTIVITY-RE
                                        Log.d(TAG, "Navigáció indul a HomeActivity-re.");
                                        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish(); // Bezárja a RegisterActivity-t
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "RegisterActivity: FirebaseHelper.OnUserCreationListener onFailure MEGHÍVVA.", e);
                                        setInProgress(false);
                                        Toast.makeText(RegisterActivity.this, "Regisztráció sikeres (Auth), de hiba történt az adatok mentésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        // ... (felhasználó törlésének logikája Auth-ból, ha szükséges)
                                        FirebaseUser firebaseUser = mAuth.getCurrentUser(); // Újra lekérjük, hogy biztosan meglegyen
                                        if (firebaseUser != null) {
                                            firebaseUser.delete().addOnCompleteListener(deleteTask -> {
                                                if (deleteTask.isSuccessful()) {
                                                    Log.d(TAG, "Auth felhasználó sikeresen törölve adatbázis hiba után.");
                                                } else {
                                                    Log.w(TAG, "Auth felhasználó törlése sikertelen adatbázis hiba után:", deleteTask.getException());
                                                }
                                            });
                                        }
                                    }
                                });

                            } else {
                                Log.e(TAG, "Auth sikeres, de FirebaseUser null.");
                                setInProgress(false);
                                Toast.makeText(RegisterActivity.this, "Ismeretlen Auth hiba történt a felhasználó lekérésekor.", Toast.LENGTH_LONG).show();
                            }
                        } else { // Auth createUserWithEmailAndPassword sikertelen
                            Log.w(TAG, "Firebase Auth sikertelen:", task.getException());
                            setInProgress(false);
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                etRegisterEmail.setError("Ez az email cím már regisztrálva van.");
                                etRegisterEmail.requestFocus();
                                Toast.makeText(RegisterActivity.this, "Ez az email cím már foglalt.", Toast.LENGTH_LONG).show();
                            } else if (task.getException() != null) {
                                Toast.makeText(RegisterActivity.this, "Regisztráció sikertelen: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Ismeretlen regisztrációs hiba.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void setInProgress(boolean inProgress) {
        if (progressBarRegister == null || btnRegisterAction == null || tvBackToLogin == null) {
            Log.w(TAG, "setInProgress: Egy vagy több UI elem null. Ellenőrizd az XML ID-kat és a findViewById hívásokat.");
            return;
        }
        if (inProgress) {
            Log.d(TAG, "setInProgress(true) - ProgressBar mutat, gombok letiltva.");
            progressBarRegister.setVisibility(View.VISIBLE);
            btnRegisterAction.setEnabled(false);
            tvBackToLogin.setEnabled(false);
        } else {
            Log.d(TAG, "setInProgress(false) - ProgressBar elrejtve, gombok engedélyezve.");
            progressBarRegister.setVisibility(View.GONE);
            btnRegisterAction.setEnabled(true);
            tvBackToLogin.setEnabled(true);
        }
    }
}
