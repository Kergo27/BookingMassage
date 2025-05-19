package com.example.bookingmassage; // Ellenőrizd a csomagnevet

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem; // FONTOS IMPORT a vissza gomb kezeléséhez
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull; // FONTOS IMPORT a MenuItem-hez
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bookingmassage.FirebaseHelper;
import com.google.android.material.appbar.MaterialToolbar; // Győződj meg róla, hogy ez az import megvan
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AdminPanelActivity extends AppCompatActivity {
    private static final String TAG = "AdminPanelActivity";

    private Button btnAdminPickStartDate, btnAdminGenerateSlots;
    private TextView tvAdminSelectedStartDate;
    private TextInputLayout tilAdminWeeksToGenerate;
    private TextInputEditText etAdminWeeksToGenerate;
    private ProgressBar progressBarAdmin;
    private MaterialToolbar toolbarAdmin; // Toolbar referencia

    private FirebaseHelper firebaseHelper;
    private Calendar selectedStartDateCalendar;
    private SimpleDateFormat dateFormatForDisplay = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));
    // private SimpleDateFormat dateFormatForFirebase = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Ezt a FirebaseHelper használja

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel); // Győződj meg róla, hogy a layout neve helyes

        firebaseHelper = new FirebaseHelper();
        selectedStartDateCalendar = Calendar.getInstance();

        // UI Elemek inicializálása
        toolbarAdmin = findViewById(R.id.toolbarAdmin); // Toolbar inicializálása
        btnAdminPickStartDate = findViewById(R.id.btnAdminPickStartDate);
        tvAdminSelectedStartDate = findViewById(R.id.tvAdminSelectedStartDate);
        tilAdminWeeksToGenerate = findViewById(R.id.tilAdminWeeksToGenerate);
        etAdminWeeksToGenerate = findViewById(R.id.etAdminWeeksToGenerate);
        btnAdminGenerateSlots = findViewById(R.id.btnAdminGenerateSlots);
        progressBarAdmin = findViewById(R.id.progressBarAdmin);

        // Toolbar beállítása Action Barként
        setSupportActionBar(toolbarAdmin);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Vissza gomb (nyíl) megjelenítése
            getSupportActionBar().setDisplayShowHomeEnabled(true); // Vissza gomb engedélyezése
            // getSupportActionBar().setTitle("Admin Panel"); // Cím beállítása, ha az XML-ben nincs, vagy felül akarod írni
        }
        // A Toolbar címét az XML-ben (app:title) is beállíthatod, ahogy korábban tettük.

        updateSelectedDateDisplay();

        btnAdminPickStartDate.setOnClickListener(v -> showDatePickerDialog());
        btnAdminGenerateSlots.setOnClickListener(v -> generateSlots());
    }

    // Vissza gomb kezelése a Toolbaron
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Az android.R.id.home az alapértelmezett ID a Toolbar "home" (vissza) gombjához
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Ugyanazt csinálja, mint a rendszer vissza gombja
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDatePickerDialog() {
        // ... (a DatePickerDialog kódja változatlan)
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedStartDateCalendar.set(year, month, dayOfMonth);
                    updateSelectedDateDisplay();
                },
                selectedStartDateCalendar.get(Calendar.YEAR),
                selectedStartDateCalendar.get(Calendar.MONTH),
                selectedStartDateCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateSelectedDateDisplay() {
        // ... (a kód változatlan)
        tvAdminSelectedStartDate.setText("Kiválasztott kezdőnap: " + dateFormatForDisplay.format(selectedStartDateCalendar.getTime()));
    }

    private void generateSlots() {
        // ... (a generáló logika kódja változatlan)
        String weeksStr = etAdminWeeksToGenerate.getText() != null ? etAdminWeeksToGenerate.getText().toString() : "";
        if (TextUtils.isEmpty(weeksStr)) {
            tilAdminWeeksToGenerate.setError("Add meg a hetek számát!");
            return;
        }
        int weeksToGenerate;
        try {
            weeksToGenerate = Integer.parseInt(weeksStr);
            if (weeksToGenerate <= 0 || weeksToGenerate > 52) {
                tilAdminWeeksToGenerate.setError("A hetek száma 1 és 52 között legyen!");
                return;
            }
        } catch (NumberFormatException e) {
            tilAdminWeeksToGenerate.setError("Hibás számformátum!");
            return;
        }
        tilAdminWeeksToGenerate.setError(null);

        Log.i(TAG, "Generálás indítása: " +
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedStartDateCalendar.getTime()) +
                " naptól, " + weeksToGenerate + " hétre.");
        progressBarAdmin.setVisibility(View.VISIBLE);
        btnAdminGenerateSlots.setEnabled(false);

        firebaseHelper.generateSlotsForDateRange(selectedStartDateCalendar, weeksToGenerate, new FirebaseHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                progressBarAdmin.setVisibility(View.GONE);
                btnAdminGenerateSlots.setEnabled(true);
                Toast.makeText(AdminPanelActivity.this, weeksToGenerate + " hétnyi időpont sikeresen generálva/elindítva.", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Időpont generálás (callback): Siker.");
            }

            @Override
            public void onFailure(Exception e) {
                progressBarAdmin.setVisibility(View.GONE);
                btnAdminGenerateSlots.setEnabled(true);
                Toast.makeText(AdminPanelActivity.this, "Hiba az időpontok generálása közben: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Időpont generálás (callback): Hiba.", e);
            }
        });
    }
}
