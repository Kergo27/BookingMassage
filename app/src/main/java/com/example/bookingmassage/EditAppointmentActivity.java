package com.example.bookingmassage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookingmassage.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

public class EditAppointmentActivity extends AppCompatActivity {

    private static final String TAG = "EditAppointmentActivity";

    private TextView tvCurrentAppointmentDetails;
    private Spinner spinnerNewMassageType;
    private Button btnSaveChanges;
    private ImageButton btnBackEditAppointment;
    private ProgressBar progressBarEdit;

    private FirebaseHelper firebaseHelper;
    private FirebaseUser currentUser;

    private String appointmentId, userId, timeSlotId, currentMassageType, date, time;
    private String[] massageTypesArray;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);
        Log.d(TAG, "onCreate");

        firebaseHelper = new FirebaseHelper();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Hiba: Nincs bejelentkezett felhasználó.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Adatok átvétele az Intent-ből
        Intent intent = getIntent();
        appointmentId = intent.getStringExtra("APPOINTMENT_ID");
        userId = intent.getStringExtra("USER_ID"); // Ezt is át kell adni
        timeSlotId = intent.getStringExtra("TIME_SLOT_ID");
        currentMassageType = intent.getStringExtra("CURRENT_MASSAGE_TYPE");
        date = intent.getStringExtra("DATE");
        time = intent.getStringExtra("TIME");

        if (appointmentId == null || userId == null || timeSlotId == null || currentMassageType == null || date == null || time == null) {
            Toast.makeText(this, "Hiba: Hiányzó adatok a foglalás módosításához.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Hiányzó intent extrák: apptId=" + appointmentId + ", userId=" + userId + ", slotId=" + timeSlotId + ", type=" + currentMassageType + ", date=" + date + ", time=" + time);
            finish();
            return;
        }

        // UI Elemek inicializálása
        tvCurrentAppointmentDetails = findViewById(R.id.tvCurrentAppointmentDetails);
        spinnerNewMassageType = findViewById(R.id.spinnerNewMassageType);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnBackEditAppointment = findViewById(R.id.btnBackEditAppointment);
        progressBarEdit = findViewById(R.id.progressBarEdit);

        tvCurrentAppointmentDetails.setText("Időpont: " + date + " " + time + "\nJelenlegi típus: " + currentMassageType);

        // Spinner beállítása
        massageTypesArray = getResources().getStringArray(R.array.services_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, massageTypesArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNewMassageType.setAdapter(adapter);

        // Előre kiválasztjuk a jelenlegi típust a spinnerben
        int currentTypePosition = Arrays.asList(massageTypesArray).indexOf(currentMassageType);
        if (currentTypePosition >= 0) {
            spinnerNewMassageType.setSelection(currentTypePosition);
        }

        btnBackEditAppointment.setOnClickListener(v -> onBackPressed());

        btnSaveChanges.setOnClickListener(v -> {
            String newSelectedMassageType = spinnerNewMassageType.getSelectedItem().toString();
            if (newSelectedMassageType.equals(currentMassageType)) {
                Toast.makeText(EditAppointmentActivity.this, "Nem történt változás a masszázs típusában.", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Módosítás mentése: Új típus: " + newSelectedMassageType);
            progressBarEdit.setVisibility(View.VISIBLE);
            firebaseHelper.modifyAppointmentMassageType(
                    userId,
                    appointmentId,
                    timeSlotId,
                    newSelectedMassageType,
                    new FirebaseHelper.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess() {
                            progressBarEdit.setVisibility(View.GONE);
                            Toast.makeText(EditAppointmentActivity.this, "Masszázs típusa sikeresen módosítva.", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK); // Jelezzük az AppointmentsActivity-nek, hogy frissítsen
                            finish(); // Visszatérés az előző Activity-re
                        }

                        @Override
                        public void onFailure(Exception e) {
                            progressBarEdit.setVisibility(View.GONE);
                            Toast.makeText(EditAppointmentActivity.this, "Hiba a módosítás során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Hiba a masszázs típus módosításakor: ", e);
                        }
                    }
            );
        });
    }
}
