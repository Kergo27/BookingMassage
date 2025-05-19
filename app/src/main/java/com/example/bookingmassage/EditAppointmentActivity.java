package com.example.bookingmassage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
// import android.widget.ArrayAdapter; // Már nem kell az AutoCompleteTextView-hez
import android.widget.Button;
import android.widget.LinearLayout; // Szükséges a RadioButton LayoutParams-hoz
import android.widget.ProgressBar;
import android.widget.RadioGroup; // RadioGroup import
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Színhez

// FirebaseHelper import (győződj meg róla, hogy a csomagnév helyes)
import com.example.bookingmassage.FirebaseHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.radiobutton.MaterialRadioButton; // MaterialRadioButton import
// import com.google.android.material.textfield.TextInputLayout; // Már nem kell
// import android.widget.AutoCompleteTextView; // Már nem kell


public class EditAppointmentActivity extends AppCompatActivity {

    private static final String TAG = "EditAppointmentAct";

    private TextView tvCurrentAppointmentDetails;
    private RadioGroup radioGroupMassageType; // KICSERÉLVE: AutoCompleteTextView -> RadioGroup
    private Button btnSaveChanges;
    private ProgressBar progressBarEdit;
    private MaterialToolbar toolbarEditAppointment;

    private FirebaseHelper firebaseHelper;

    private String appointmentId;
    private String timeSlotId;
    private String currentMassageType;
    private String appointmentDate;
    private String appointmentTime;

    private String[] massageTypesArray;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_appointment);
        Log.d(TAG, "onCreate elindult.");

        firebaseHelper = new FirebaseHelper();

        // UI Elemek inicializálása
        toolbarEditAppointment = findViewById(R.id.toolbarEditAppointment);
        tvCurrentAppointmentDetails = findViewById(R.id.tvCurrentAppointmentDetails);
        radioGroupMassageType = findViewById(R.id.radioGroupMassageType); // ÚJ inicializálás
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        progressBarEdit = findViewById(R.id.progressBarEdit);

        // Toolbar beállítása
        if (toolbarEditAppointment != null) {
            setSupportActionBar(toolbarEditAppointment);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        } else {
            Log.e(TAG, "HIBA: toolbarEditAppointment nem található!");
        }

        // Null ellenőrzés a kritikus UI elemekre
        if (tvCurrentAppointmentDetails == null || radioGroupMassageType == null || btnSaveChanges == null || progressBarEdit == null || toolbarEditAppointment == null) {
            Log.e(TAG, "HIBA: Egy vagy több UI elem null az EditAppointmentActivity-ben! Ellenőrizd az XML ID-kat.");
            Toast.makeText(this, "Hiba a szerkesztő felületének betöltésekor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Intent adatok lekérése
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("APPOINTMENT_ID") && intent.hasExtra("TIME_SLOT_ID")) {
            appointmentId = intent.getStringExtra("APPOINTMENT_ID");
            timeSlotId = intent.getStringExtra("TIME_SLOT_ID");
            currentMassageType = intent.getStringExtra("CURRENT_MASSAGE_TYPE");
            appointmentDate = intent.getStringExtra("DATE");
            appointmentTime = intent.getStringExtra("TIME");

            Log.d(TAG, "Fogadott adatok: AppointmentID=" + appointmentId + ", TimeSlotID=" + timeSlotId +
                    ", CurrentType=" + currentMassageType + ", Date=" + appointmentDate + ", Time=" + appointmentTime);

            if (TextUtils.isEmpty(appointmentId) || TextUtils.isEmpty(timeSlotId) || TextUtils.isEmpty(currentMassageType)) {
                Log.e(TAG, "HIBA: Hiányzó vagy üres kritikus adatok az Intentből!");
                Toast.makeText(this, "Hiba a szerkesztő betöltésekor: Hiányzó adatok.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            populateUI();
        } else {
            Log.e(TAG, "HIBA: Nem érkeztek meg a szükséges adatok az Intentben a foglalás szerkesztéséhez!");
            Toast.makeText(this, "Hiba a szerkesztő betöltésekor: Nincsenek adatok.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void populateUI() {
        Log.d(TAG, "populateUI: Adatok megjelenítése és RadioGroup feltöltése.");
        if (tvCurrentAppointmentDetails != null) {
            String details = "Időpont: " + (appointmentDate != null ? appointmentDate : "N/A") + " " +
                    (appointmentTime != null ? appointmentTime : "N/A") + "\n" +
                    "Jelenlegi típus: " + (currentMassageType != null ? currentMassageType : "N/A");
            tvCurrentAppointmentDetails.setText(details);
        }

        massageTypesArray = getResources().getStringArray(R.array.services_array);
        if (massageTypesArray == null || massageTypesArray.length == 0) {
            Log.e(TAG, "HIBA: massageTypesArray (R.array.services_array) üres vagy null!");
            Toast.makeText(this, "Hiba: Masszázs típusok listája nem érhető el.", Toast.LENGTH_LONG).show();
            if (radioGroupMassageType != null) radioGroupMassageType.setEnabled(false); // Csak akkor, ha nem null
            return;
        }
        Log.d(TAG, "Masszázs típusok betöltve a tömbből, elemszám: " + massageTypesArray.length);

        // RadioGroup feltöltése
        if (radioGroupMassageType != null) {
            radioGroupMassageType.removeAllViews(); // Esetleges korábbi gombok törlése
            for (String type : massageTypesArray) {
                MaterialRadioButton radioButton = new MaterialRadioButton(this);
                radioButton.setText(type);
                radioButton.setId(View.generateViewId()); // Dinamikus ID generálása minden rádiógombnak

                // Stílus és térköz beállítása (opcionális, de ajánlott)
                // radioButton.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
                // A MaterialRadioButton alapból jó stílust használ, a textColor-t a téma adja.
                // Ha egyedi színt akarsz:
                // radioButton.setTextColor(ContextCompat.getColor(this, R.color.your_radio_button_text_color));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                // Térköz a rádiógombok között (pl. 8dp)
                int marginInDp = 8;
                int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
                params.setMargins(0, 0, 0, marginInPx);
                radioButton.setLayoutParams(params);

                radioGroupMassageType.addView(radioButton);

                // Aktuális típus kiválasztása (bejelölése)
                if (currentMassageType != null && currentMassageType.equals(type)) {
                    radioButton.setChecked(true);
                    Log.d(TAG, "Kezdő masszázs típus bejelölve a RadioButton-on: " + type);
                }
            }
        } else {
            Log.e(TAG, "populateUI: radioGroupMassageType null!");
        }
    }

    private void saveChanges() {
        if (radioGroupMassageType == null) {
            Log.e(TAG, "saveChanges: radioGroupMassageType null!");
            Toast.makeText(this, "Hiba a mentés előkészítésekor.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRadioButtonId = radioGroupMassageType.getCheckedRadioButtonId();
        String newSelectedMassageType = "";

        if (selectedRadioButtonId != -1) { // Ellenőrizzük, hogy van-e kiválasztott
            MaterialRadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
            if (selectedRadioButton != null) {
                newSelectedMassageType = selectedRadioButton.getText().toString();
            } else {
                Log.e(TAG, "HIBA: A kiválasztott RadioButton (ID: " + selectedRadioButtonId + ") null!");
                Toast.makeText(this, "Hiba a kiválasztott típus olvasásakor.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(this, "Kérjük, válasszon egy masszázs típust!", Toast.LENGTH_SHORT).show();
            return; // Nincs kiválasztott elem
        }

        if (TextUtils.isEmpty(newSelectedMassageType)) { // Ez az ág valószínűleg már nem szükséges az előző ellenőrzés miatt
            Toast.makeText(this, "Kérjük, válasszon egy masszázs típust!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newSelectedMassageType.equals(currentMassageType)) {
            Toast.makeText(this, "Nem történt változás a masszázs típusában.", Toast.LENGTH_SHORT).show();
            // Opcionálisan itt is visszatérhetsz az előző Activity-re, ha ez a kívánt viselkedés.
            // finish();
            return;
        }

        Log.i(TAG, "Változtatások mentése: AppointmentID=" + appointmentId + ", TimeSlotID=" + timeSlotId +
                ", Új Típus=" + newSelectedMassageType);
        if (progressBarEdit != null) progressBarEdit.setVisibility(View.VISIBLE);
        if (btnSaveChanges != null) btnSaveChanges.setEnabled(false);

        firebaseHelper.modifyAppointmentMassageType(appointmentId, timeSlotId, newSelectedMassageType,
                new FirebaseHelper.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        if (progressBarEdit != null) progressBarEdit.setVisibility(View.GONE);
                        if (btnSaveChanges != null) btnSaveChanges.setEnabled(true);
                        Log.i(TAG, "Masszázs típus sikeresen módosítva.");
                        Toast.makeText(EditAppointmentActivity.this, "Masszázs típus sikeresen módosítva!", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (progressBarEdit != null) progressBarEdit.setVisibility(View.GONE);
                        if (btnSaveChanges != null) btnSaveChanges.setEnabled(true);
                        Log.e(TAG, "Hiba a masszázs típus módosításakor.", e);
                        Toast.makeText(EditAppointmentActivity.this, "Hiba a mentés során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
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
}
