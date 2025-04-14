package com.example.bookingmassage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity{
    private static final String PREF_KEY = Objects.requireNonNull(RegisterActivity.class.getPackage()).toString();

    EditText userEmailEditText;
    EditText passwordEditText;
    EditText passwordConfirmEditText;

    private FirebaseAuth mAuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);

        if (secret_key != 99) { finish(); }

        userEmailEditText = findViewById(R.id.userEmailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        passwordConfirmEditText = findViewById(R.id.passwordAgainEditText);

        SharedPreferences preferences = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        String email = preferences.getString("email", "");
        String password = preferences.getString("password", "");

        userEmailEditText.setText(email);
        passwordEditText.setText(password);
        passwordConfirmEditText.setText(password);

        mAuth = FirebaseAuth.getInstance();
    }

    public void register(View view) {
        String email = userEmailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String passwordConfirm = passwordConfirmEditText.getText().toString();

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(RegisterActivity.this,"A jelszavak nem egyeznek meg.",Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                Toast.makeText(RegisterActivity.this,"Sikeres Regisztracio: " + email,Toast.LENGTH_LONG).show();
                goToMainPage();
            } else {
                Toast.makeText(RegisterActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMainPage() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void cancel(View view) {
        finish();
    }

    private void startDating() {
        //Intent intent = new Intent(this, DateListActivity.class);
        //startActivity(intent);
    }

}