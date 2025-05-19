package com.example.bookingmassage;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem; // FONTOS IMPORT
import android.view.View;
import android.widget.Button;
// import android.widget.ImageButton; // MÁR NEM KELL
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // FONTOS IMPORT
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// FONTOS IMPORT
import com.google.android.material.appbar.MaterialToolbar;

// Győződj meg róla, hogy a saját modelljeid és adaptered importálva vannak a helyes csomagból
import com.example.bookingmassage.TimeSlot; // Módosítottam a csomagnevet
import com.example.bookingmassage.TimeSlotAdapter; // Feltételezve, hogy a TimeSlotAdapter itt van
import com.example.bookingmassage.FirebaseHelper; // Feltételezve, hogy a FirebaseHelper itt van

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
// import java.util.Collections; // Nincs rá szükség, ha a query rendez
// import java.util.Comparator; // Nincs rá szükség, ha a query rendez
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity implements TimeSlotAdapter.OnTimeSlotClickListener {

    private static final String TAG = "BookingActivity";

    private TextView tvSelectedDate, tvNoSlotsMessage;
    private Button btnPickDate;
    // private ImageButton btnBackBooking; // ELTÁVOLÍTVA
    private MaterialToolbar toolbarBooking; // ÚJ: Toolbar referencia
    private RecyclerView rvTimeSlots;
    private ProgressBar progressBarBooking;
    private TimeSlotAdapter timeSlotAdapter;

    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private List<TimeSlot> currentTimeSlotsList = new ArrayList<>(); // Inicializálás itt
    private Calendar selectedCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatForDisplay = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));
    private SimpleDateFormat dateFormatForFirebase = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // @SuppressLint({"WrongViewCast", "MissingInflatedId"}) // A WrongViewCast valószínűleg a régi ImageButton miatt volt
    @SuppressLint({"MissingInflatedId"}) // Ezt az annotációt érdemes lehet eltávolítani, ha minden ID stimmel
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Győződj meg róla, hogy az XML fájl neve helyes és a módosított MD3 verziót használod
        setContentView(R.layout.activity_booking);
        Log.d(TAG, "onCreate elindult.");

        // UI Elemek inicializálása
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        toolbarBooking = findViewById(R.id.toolbarBooking); // ÚJ: Toolbar inicializálása
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        progressBarBooking = findViewById(R.id.progressBarBooking);
        tvNoSlotsMessage = findViewById(R.id.tvNoSlotsMessage);

        // Toolbar beállítása Action Barként
        if (toolbarBooking != null) {
            setSupportActionBar(toolbarBooking);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Vissza gomb megjelenítése
                getSupportActionBar().setDisplayShowHomeEnabled(true); // Vissza gomb engedélyezése
                // A címet az XML-ben (app:title) állítottuk be a MaterialToolbaron
            }
        } else {
            Log.e(TAG, "HIBA: toolbarBooking nem található az activity_booking.xml-ben!");
            // Itt akár finish()-t is hívhatsz, ha a Toolbar kritikus
        }

        // Null ellenőrzés a kritikus UI elemekre
        if (tvSelectedDate == null || btnPickDate == null || toolbarBooking == null || /* btnBackBooking helyett toolbarBooking */
                rvTimeSlots == null || progressBarBooking == null || tvNoSlotsMessage == null) {
            Log.e(TAG, "Egy vagy több UI elem null a findViewById után! Ellenőrizd az activity_booking.xml ID-kat.");
            Toast.makeText(this, "Hiba a felület betöltésekor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Firebase inicializálás
        firebaseHelper = new FirebaseHelper();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Hiba: Nincs bejelentkezett felhasználó! Kérjük, jelentkezzen be újra.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Log.d(TAG, "Bejelentkezett felhasználó: " + currentUser.getEmail());

        setupRecyclerView();
        setInitialDateAndLoadSlots();

        btnPickDate.setOnClickListener(v -> showDatePickerDialog());
        // A btnBackBooking.setOnClickListener(...) rész eltávolítva,
        // mert a Toolbar vissza gombját az onOptionsItemSelected kezeli.
    }

    // Vissza gomb kezelése a Toolbaron
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Ugyanaz, mint a rendszer vissza gombja
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        // ... (a kódod itt változatlan maradhat)
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        // Az timeSlotAdapter inicializálásakor győződj meg róla, hogy a this (OnTimeSlotClickListener) helyes
        timeSlotAdapter = new TimeSlotAdapter(currentTimeSlotsList, this);
        rvTimeSlots.setAdapter(timeSlotAdapter);
        Log.d(TAG, "RecyclerView és TimeSlotAdapter beállítva.");
    }

    private void setInitialDateAndLoadSlots() {
        // ... (a kódod itt változatlan maradhat)
        Log.d(TAG, "setInitialDateAndLoadSlots elindult.");
        Calendar today = Calendar.getInstance();
        selectedCalendar.setTime(today.getTime());

        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY) {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 2);
        } else if (dayOfWeek == Calendar.SUNDAY) {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        updateDateLabel();
        loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime()));
    }

    private void showDatePickerDialog() {
        // ... (a kódod itt változatlan maradhat)
        Log.d(TAG, "showDatePickerDialog megnyitva.");
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, monthOfYear);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                    String newDateString = dateFormatForFirebase.format(selectedCalendar.getTime());
                    Log.d(TAG, "DatePickerDialog: Új dátum kiválasztva: " + newDateString);
                    loadTimeSlotsForDate(newDateString);
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        // ... (a kódod itt változatlan maradhat)
        tvSelectedDate.setText(dateFormatForDisplay.format(selectedCalendar.getTime()));
        Log.d(TAG, "Dátum címke frissítve: " + tvSelectedDate.getText().toString());
    }

    private void loadTimeSlotsForDate(String dateString) {
        // ... (a kódod itt változatlan maradhat, de a hibakezelést és a logolást érdemes lehet finomítani)
        Log.i(TAG, "Firestore: loadTimeSlotsForDate hívva a következő dátummal: " + dateString);
        progressBarBooking.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
        tvNoSlotsMessage.setVisibility(View.GONE);

        Calendar tempCal = Calendar.getInstance();
        try {
            Date parsedDate = dateFormatForFirebase.parse(dateString);
            if (parsedDate == null) throw new ParseException("Sikertelen parse",0); // Plusz ellenőrzés
            tempCal.setTime(parsedDate);
        } catch (ParseException e) {
            Log.e(TAG, "Hiba a dátum ('" + dateString + "') parse-olásakor a hétvége ellenőrzéshez.", e);
            progressBarBooking.setVisibility(View.GONE);
            updateUIVisibility(true, "Hibás dátum formátum.");
            return;
        }

        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            Log.d(TAG, "Kiválasztott nap (" + dateString + ") hétvége, nem töltünk be időpontokat.");
            Toast.makeText(this, "Hétvégén nincs időpontfoglalás.", Toast.LENGTH_LONG).show();
            currentTimeSlotsList.clear();
            timeSlotAdapter.updateTimeSlots(currentTimeSlotsList); // Adapter értesítése
            progressBarBooking.setVisibility(View.GONE);
            updateUIVisibility(true, "Hétvégén nincs időpontfoglalás. Kérjük, válasszon hétköznapot.");
            return;
        }

        firebaseHelper.getTimeSlotsForDate(dateString, new FirebaseHelper.OnTimeSlotsLoadedListener() {
            @Override
            public void onLoaded(List<TimeSlot> timeSlotsFromFirestore) {
                Log.d(TAG, "Firestore OnTimeSlotsLoadedListener.onLoaded: " +
                        (timeSlotsFromFirestore != null ? timeSlotsFromFirestore.size() : "null lista") +
                        " időpont (nyers) betöltve " + dateString + " napra.");
                currentTimeSlotsList.clear();

                if (timeSlotsFromFirestore != null && !timeSlotsFromFirestore.isEmpty()) {
                    for (TimeSlot slot : timeSlotsFromFirestore) {
                        try {
                            if (slot != null && slot.getTime() != null && slot.getTime().matches("\\d{2}:\\d{2}")) {
                                int hour = Integer.parseInt(slot.getTime().substring(0, 2));
                                if (hour >= 8 && hour < 18) {
                                    currentTimeSlotsList.add(slot);
                                } else {
                                    Log.w(TAG, "Kiszűrt időpont (órán kívül esik): " + slot.getTime() + " a " + dateString + " napon.");
                                }
                            } else {
                                Log.w(TAG, "Hibás idő formátum vagy null idő/slot a Firestore időpontban: " + (slot != null ? slot.toString() : "null slot"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Hiba az időpont feldolgozásakor: " + (slot != null ? slot.toString() : "null slot"), e);
                        }
                    }
                }

                timeSlotAdapter.updateTimeSlots(currentTimeSlotsList);
                progressBarBooking.setVisibility(View.GONE);
                updateUIVisibility(currentTimeSlotsList.isEmpty(), "Nincsenek elérhető időpontok ezen a napon.");
                Log.d(TAG, currentTimeSlotsList.size() + " időpont jelenik meg a RecyclerView-ban a " + dateString + " napra.");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Hiba az időpontok lekérdezésekor a FirebaseHelperből: ", e);
                Toast.makeText(BookingActivity.this, "Hiba az időpontok lekérdezésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                progressBarBooking.setVisibility(View.GONE);
                updateUIVisibility(true, "Hiba történt az időpontok betöltése közben.");
            }
        });
    }

    private void updateUIVisibility(boolean showNoSlotsMessage, @Nullable String message) {
        // ... (a kódod itt változatlan maradhat)
        if (showNoSlotsMessage) {
            if(tvNoSlotsMessage != null) { // Plusz null check
                tvNoSlotsMessage.setText(message != null ? message : "Nincsenek elérhető időpontok ezen a napon.");
                tvNoSlotsMessage.setVisibility(View.VISIBLE);
            }
            if(rvTimeSlots != null) rvTimeSlots.setVisibility(View.GONE); // Plusz null check
        } else {
            if(tvNoSlotsMessage != null) tvNoSlotsMessage.setVisibility(View.GONE);
            if(rvTimeSlots != null) rvTimeSlots.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "updateUIVisibility: showNoSlotsMessage=" + showNoSlotsMessage + ", rvVisible=" + (rvTimeSlots != null && rvTimeSlots.getVisibility() == View.VISIBLE));
    }


    @Override
    public void onTimeSlotClicked(TimeSlot timeSlot) {
        // ... (a kódod itt változatlan maradhat, de a hibakezelést érdemes lehet finomítani)
        Log.d(TAG, "Kattintott időpont: " + timeSlot.getDate() + " " + timeSlot.getTime() + (timeSlot.isAvailable() ? " (Szabad)" : " (Foglalt)"));
        if (currentUser == null) {
            Toast.makeText(this, "Hiba: Kérjük, jelentkezzen be újra.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!timeSlot.isAvailable()) {
            Toast.makeText(this, "Ez az időpont már foglalt.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        final String[] massageTypes = getResources().getStringArray(R.array.services_array);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Foglalás: " + timeSlot.getDate() + " " + timeSlot.getTime());
        builder.setSingleChoiceItems(massageTypes, -1, (dialog, which) -> {
            // A választást a "Lefoglalom" gomb fogja kezelni
        });

        builder.setPositiveButton("Lefoglalom", (dialog, whichButton) -> {
            int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
            if (selectedPosition == -1) {
                Toast.makeText(BookingActivity.this, "Kérjük, válasszon masszázs típust!", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedMassageType = massageTypes[selectedPosition];
            Log.d(TAG, "Foglalás megerősítve: " + selectedMassageType + " az időpontra: " + timeSlot.getId());
            if (progressBarBooking != null) progressBarBooking.setVisibility(View.VISIBLE); // Null check

            firebaseHelper.bookSlot(
                    timeSlot.getId(),
                    userId,
                    selectedMassageType,
                    timeSlot.getDate(),
                    timeSlot.getTime(),
                    new FirebaseHelper.OnBookingCompleteListener() {
                        @Override
                        public void onSuccess() {
                            if (progressBarBooking != null) progressBarBooking.setVisibility(View.GONE);
                            Log.d(TAG, "Firestore: Sikeres foglalás (callback). Időpont: " + timeSlot.getId());
                            Toast.makeText(BookingActivity.this, "Időpont sikeresen lefoglalva!", Toast.LENGTH_LONG).show();
                            loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime()));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (progressBarBooking != null) progressBarBooking.setVisibility(View.GONE);
                            Log.e(TAG, "Firestore: Foglalás sikertelen (callback). Időpont: " + timeSlot.getId(), e);
                            Toast.makeText(BookingActivity.this, "Hiba a foglalás során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime()));
                        }
                    }
            );
        });
        builder.setNegativeButton("Mégse", (dialog, whichButton) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Az onCreate-ben és a dátumválasztáskor már töltünk, itt általában nem kell újra,
        // hacsak nincs valami specifikus ok (pl. visszatérés másik Activity-ből, ami módosíthatott adatokat)
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }
}
