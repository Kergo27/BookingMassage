package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

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

import com.example.bookingmassage.FirebaseHelper; // Győződj meg róla, hogy a csomagnév helyes
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.Locale; // Szükséges a String.format Locale-jához

public class EditAppointmentActivity extends AppCompatActivity {

    private static final String TAG = "EditAppointmentActivity";

    // UI Elemek
    private TextView tvCurrentAppointmentDetails;
    private Spinner spinnerNewMassageType;
    private Button btnSaveChanges;
    private ImageButton btnBackEditAppointment;
    private ProgressBar progressBarEdit;

    // Firebase és Adat Változók
    private FirebaseHelper firebaseHelper;
    private FirebaseUser currentUser;

    // Intent-ből kapott adatok tárolására
    private String receivedAppointmentId;    // Az 'appointments' kollekció dokumentum ID-ja
    private String receivedUserId;           // A foglalást végző felhasználó ID-ja
    private String receivedTimeSlotId;       // A 'time_slots' kollekció dokumentum ID-ja (pl. "2025-05-19_0800")
    private String receivedCurrentMassageType; // A jelenlegi masszázs típus
    private String receivedDate;             // A foglalás dátuma
    private String receivedTime;             // A foglalás ideje

    private String[] massageTypesArray; // A Spinner tartalmához

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_appointment);
        Log.d(TAG, "onCreate elindult.");

        // Firebase inicializálás
        firebaseHelper = new FirebaseHelper();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Hiba: Nincs bejelentkezett felhasználó. Kérjük, jelentkezzen be újra.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Bejelentkezett felhasználó: " + currentUser.getEmail());

        // Adatok átvétele az Intent-ből
        Intent intent = getIntent();
        receivedAppointmentId = intent.getStringExtra("APPOINTMENT_ID");
        receivedUserId = intent.getStringExtra("USER_ID");
        receivedTimeSlotId = intent.getStringExtra("TIME_SLOT_ID");
        receivedCurrentMassageType = intent.getStringExtra("CURRENT_MASSAGE_TYPE");
        receivedDate = intent.getStringExtra("DATE");
        receivedTime = intent.getStringExtra("TIME");

        // Logoljuk az átvett értékeket alapos ellenőrzéshez
        Log.i(TAG, "Intent adatok átvéve az EditAppointmentActivity-ben:");
        Log.i(TAG, "  APPOINTMENT_ID (foglalás ID): " + receivedAppointmentId);
        Log.i(TAG, "  USER_ID (felhasználó ID): " + receivedUserId);
        Log.i(TAG, "  TIME_SLOT_ID (idősáv ID, pl. YYYY-MM-DD_HHMM): " + receivedTimeSlotId);
        Log.i(TAG, "  CURRENT_MASSAGE_TYPE (jelenlegi típus): " + receivedCurrentMassageType);
        Log.i(TAG, "  DATE (dátum): " + receivedDate);
        Log.i(TAG, "  TIME (idő): " + receivedTime);

        // Ellenőrizzük, hogy minden szükséges adat megérkezett-e
        if (receivedAppointmentId == null || receivedUserId == null || receivedTimeSlotId == null ||
                receivedCurrentMassageType == null || receivedDate == null || receivedTime == null) {
            Toast.makeText(this, "Hiba: Hiányzó adatok a foglalás módosításához.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Egy vagy több kritikus Intent extra hiányzik. Az Activity befejeződik.");
            finish();
            return;
        }

        // UI Elemek inicializálása
        tvCurrentAppointmentDetails = findViewById(R.id.tvCurrentAppointmentDetails);
        spinnerNewMassageType = findViewById(R.id.spinnerNewMassageType);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnBackEditAppointment = findViewById(R.id.btnBackEditAppointment);
        progressBarEdit = findViewById(R.id.progressBarEdit);

        // Null ellenőrzés a UI elemekre
        if (tvCurrentAppointmentDetails == null || spinnerNewMassageType == null ||
                btnSaveChanges == null || btnBackEditAppointment == null || progressBarEdit == null) {
            Log.e(TAG, "HIBA: Egy vagy több UI elem null a findViewById után! Ellenőrizd az activity_edit_appointment.xml ID-kat.");
            Toast.makeText(this, "Hiba a szerkesztő felület betöltésekor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Jelenlegi foglalás adatainak megjelenítése
        tvCurrentAppointmentDetails.setText(String.format(Locale.getDefault(), "Időpont: %s %s\nJelenlegi típus: %s",
                receivedDate, receivedTime, receivedCurrentMassageType));

        // Spinner beállítása a masszázs típusokkal
        massageTypesArray = getResources().getStringArray(R.array.services_array);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, massageTypesArray);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNewMassageType.setAdapter(spinnerAdapter);

        // Előre kiválasztjuk a jelenlegi masszázs típust a Spinnerben
        int currentTypePosition = -1; // Alapértelmezett, ha nem található
        if (receivedCurrentMassageType != null) {
            currentTypePosition = Arrays.asList(massageTypesArray).indexOf(receivedCurrentMassageType);
        }

        if (currentTypePosition >= 0) {
            spinnerNewMassageType.setSelection(currentTypePosition);
        } else {
            Log.w(TAG, "A jelenlegi masszázs típus (" + receivedCurrentMassageType + ") nem található a spinner listájában. Az első elem lesz kiválasztva.");
            if (massageTypesArray.length > 0) {
                spinnerNewMassageType.setSelection(0); // Első elem kiválasztása, ha a jelenlegi nem található
            }
        }

        // Vissza gomb listener
        btnBackEditAppointment.setOnClickListener(v -> {
            Log.d(TAG, "Vissza gomb megnyomva az EditAppointmentActivity-ből.");
            onBackPressed();
        });

        // Változtatások Mentése gomb listener
        btnSaveChanges.setOnClickListener(v -> {
            // Győződjünk meg róla, hogy a spinnernek van kiválasztott eleme
            if (spinnerNewMassageType.getSelectedItem() == null) {
                Toast.makeText(EditAppointmentActivity.this, "Kérjük, válasszon masszázs típust.", Toast.LENGTH_SHORT).show();
                return;
            }
            String newSelectedMassageTypeFromSpinner = spinnerNewMassageType.getSelectedItem().toString();

            if (newSelectedMassageTypeFromSpinner.equals(receivedCurrentMassageType)) {
                Toast.makeText(EditAppointmentActivity.this, "Nem történt változás a masszázs típusában.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Logoljuk a FirebaseHelper hívás előtti paramétereket NAGYON ALAPOSAN
            Log.i(TAG, "FirebaseHelper.modifyAppointmentMassageType hívása a következő paraméterekkel fog történni:");
            Log.i(TAG, "  AppointmentId (az 'appointments' dokumentum ID-ja): " + receivedAppointmentId);
            Log.i(TAG, "  TimeSlotId (a 'time_slots' dokumentum ID-ja): " + receivedTimeSlotId); // ENNEK KELL lennie pl. "YYYY-MM-DD_HHMM"
            Log.i(TAG, "  NewMassageType (az új típus a spinnerből): " + newSelectedMassageTypeFromSpinner); // ENNEK KELL lennie pl. "Svéd masszázs"

            progressBarEdit.setVisibility(View.VISIBLE);
            btnSaveChanges.setEnabled(false);

            firebaseHelper.modifyAppointmentMassageType(
                    receivedAppointmentId,          // Az 'appointments' dokumentum ID-ja
                    receivedTimeSlotId,             // A 'time_slots' dokumentum ID-ja (pl. "2025-05-19_0800")
                    newSelectedMassageTypeFromSpinner, // Az új masszázs típus a Spinnerből
                    new FirebaseHelper.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess() {
                            progressBarEdit.setVisibility(View.GONE);
                            btnSaveChanges.setEnabled(true);
                            Toast.makeText(EditAppointmentActivity.this, "Masszázs típusa sikeresen módosítva.", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Módosítás sikeres. Visszatérés az előző Activity-re RESULT_OK-val.");
                            setResult(RESULT_OK);
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            progressBarEdit.setVisibility(View.GONE);
                            btnSaveChanges.setEnabled(true);
                            Toast.makeText(EditAppointmentActivity.this, "Hiba a módosítás során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Hiba a masszázs típus módosításakor FirebaseHelper callback-ben: ", e);
                        }
                    }
            );
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }
}
