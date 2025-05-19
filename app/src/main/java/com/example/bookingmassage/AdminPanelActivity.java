package com.example.bookingmassage; // Ellenőrizd a csomagnevet

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bookingmassage.FirebaseHelper;
import com.google.android.material.appbar.MaterialToolbar;
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
    private MaterialToolbar toolbarAdmin;

    private FirebaseHelper firebaseHelper;
    private Calendar selectedStartDateCalendar;
    private SimpleDateFormat dateFormatForDisplay = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));
    private SimpleDateFormat dateFormatForFirebase = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        firebaseHelper = new FirebaseHelper();
        selectedStartDateCalendar = Calendar.getInstance(); // Alapból mai nap

        toolbarAdmin = findViewById(R.id.toolbarAdmin);
        btnAdminPickStartDate = findViewById(R.id.btnAdminPickStartDate);
        tvAdminSelectedStartDate = findViewById(R.id.tvAdminSelectedStartDate);
        tilAdminWeeksToGenerate = findViewById(R.id.tilAdminWeeksToGenerate);
        etAdminWeeksToGenerate = findViewById(R.id.etAdminWeeksToGenerate);
        btnAdminGenerateSlots = findViewById(R.id.btnAdminGenerateSlots);
        progressBarAdmin = findViewById(R.id.progressBarAdmin);

        setSupportActionBar(toolbarAdmin);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbarAdmin.setNavigationOnClickListener(v -> onBackPressed());

        updateSelectedDateDisplay();

        btnAdminPickStartDate.setOnClickListener(v -> showDatePickerDialog());

        btnAdminGenerateSlots.setOnClickListener(v -> generateSlots());
    }

    private void showDatePickerDialog() {
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
        tvAdminSelectedStartDate.setText("Kiválasztott kezdőnap: " + dateFormatForDisplay.format(selectedStartDateCalendar.getTime()));
    }

    private void generateSlots() {
        String weeksStr = etAdminWeeksToGenerate.getText() != null ? etAdminWeeksToGenerate.getText().toString() : "";
        if (TextUtils.isEmpty(weeksStr)) {
            tilAdminWeeksToGenerate.setError("Add meg a hetek számát!");
            return;
        }
        int weeksToGenerate;
        try {
            weeksToGenerate = Integer.parseInt(weeksStr);
            if (weeksToGenerate <= 0 || weeksToGenerate > 52) { // Ésszerű korlátok
                tilAdminWeeksToGenerate.setError("A hetek száma 1 és 52 között legyen!");
                return;
            }
        } catch (NumberFormatException e) {
            tilAdminWeeksToGenerate.setError("Hibás számformátum!");
            return;
        }
        tilAdminWeeksToGenerate.setError(null); // Hiba törlése

        Log.i(TAG, "Generálás indítása: " + dateFormatForFirebase.format(selectedStartDateCalendar.getTime()) +
                " naptól, " + weeksToGenerate + " hétre.");
        progressBarAdmin.setVisibility(View.VISIBLE);
        btnAdminGenerateSlots.setEnabled(false);

        // A FirebaseHelper-ben lévő generáló metódust kellene itt hívni,
        // ami fogad egy kezdődátumot és a napok/hetek számát.
        // Például: firebaseHelper.generateTimeSlotsForPeriod(selectedStartDateCalendar, weeksToGenerate * 7, new FirebaseHelper.OnOperationCompleteListener() { ... });
        // Most egy módosított generateTimeSlotsForMonths-t szimulálunk:
        // Ezt a részt a FirebaseHelper-ben kell megfelelően implementálni!
        // Most csak logoljuk, és egy Toast-ot jelenítünk meg.
        // Tegyük fel, hogy a FirebaseHelpernek van egy ilyen metódusa:
        // firebaseHelper.generateSlotsForDateRange(selectedStartDateCalendar, weeksToGenerate, new FirebaseHelper.OnOperationCompleteListener() {
        // Itt a generateTimeSlotsForMonths-t hívjuk, de a valóságban ezt át kellene írni,
        // hogy a selectedStartDateCalendar-tól kezdjen!
        // Mivel a generateTimeSlotsForMonths mostani verziója a mai naptól számol X hónapot,
        // ez nem lesz pontos az admin panelhez.

        // ----- ÁTMENETI MEGOLDÁS A BEMUTATÁSHOZ (Ezt kellene lecserélni egy paraméterezett generálóra) -----
        int monthsToGenerate = (weeksToGenerate + 3) / 4; // Nagyjából hónapokra átszámolva
        if (monthsToGenerate == 0) monthsToGenerate = 1;
        final int finalMonths = monthsToGenerate;

        // Ideiglenesen: Azt a logikát, ami a FirebaseHelper.generateTimeSlotsForMonths-ban van,
        // át kellene alakítani, hogy egy Calendar objektumot (selectedStartDateCalendar) és
        // a generálandó napok számát (weeksToGenerate * 7) fogadja.
        // Mivel ez bonyolultabb, most csak egy üzenetet jelenítünk meg.

        // Tegyük fel, hogy van egy generateSlotsForDateRange a FirebaseHelperben:
        firebaseHelper.generateSlotsForDateRange(selectedStartDateCalendar, weeksToGenerate, new FirebaseHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                progressBarAdmin.setVisibility(View.GONE);
                btnAdminGenerateSlots.setEnabled(true);
                Toast.makeText(AdminPanelActivity.this, weeksToGenerate + " hétnyi időpont generálása sikeresen elindítva/befejezve.", Toast.LENGTH_LONG).show();
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
        // ----- ÁTMENETI MEGOLDÁS VÉGE -----
    }
}
