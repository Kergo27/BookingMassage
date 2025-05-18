package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton; // HOZZÁADVA: ImageButton importálása
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Győződj meg róla, hogy a saját modelljeid és adaptered importálva vannak a helyes csomagból
import com.example.bookingmassage.TimeSlotAdapter;
import com.example.bookingmassage.TimeSlot;
import com.example.bookingmassage.FirebaseHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity implements TimeSlotAdapter.OnTimeSlotClickListener {

    private static final String TAG = "BookingActivity";

    private TextView tvSelectedDate, tvNoSlotsMessage;
    private Button btnPickDate;
    private ImageButton btnBackBooking; // HOZZÁADVA: Vissza gomb deklarálása
    private RecyclerView rvTimeSlots;
    private ProgressBar progressBarBooking;
    private TimeSlotAdapter timeSlotAdapter;

    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private List<TimeSlot> currentTimeSlotsList = new ArrayList<>();
    private Calendar selectedCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatForDisplay = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));
    private SimpleDateFormat dateFormatForFirebase = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        Log.d(TAG, "onCreate elindult.");

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnBackBooking = findViewById(R.id.btnBackBooking); // HOZZÁADVA: Vissza gomb inicializálása
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        progressBarBooking = findViewById(R.id.progressBarBooking);
        tvNoSlotsMessage = findViewById(R.id.tvNoSlotsMessage);

        // Null ellenőrzés kiegészítve a btnBackBooking-gal
        if (rvTimeSlots == null || progressBarBooking == null || tvNoSlotsMessage == null ||
                tvSelectedDate == null || btnPickDate == null || btnBackBooking == null) {
            Log.e(TAG, "Egy vagy több UI elem null a findViewById után! Ellenőrizd az XML ID-kat.");
            Toast.makeText(this, "Hiba a felület betöltésekor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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

        // HOZZÁADVA: Vissza gomb OnClickListener beállítása
        btnBackBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Vissza gomb (btnBackBooking) megnyomva.");
                onBackPressed(); // Meghívja az alapértelmezett vissza viselkedést (általában finish())
                // Vagy használhatod közvetlenül a finish()-t is:
                // finish();
            }
        });
    }

    private void setupRecyclerView() {
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        // Az adapter konstruktorát ellenőrizd, hogy a TimeSlotAdapter osztályod milyen paramétereket vár.
        // A TimeSlotAdapter.OnTimeSlotClickListener interfészt ez az Activity implementálja, ezért 'this'.
        timeSlotAdapter = new TimeSlotAdapter(currentTimeSlotsList, this);
        rvTimeSlots.setAdapter(timeSlotAdapter);
        Log.d(TAG, "RecyclerView és TimeSlotAdapter beállítva.");
    }

    private void setInitialDateAndLoadSlots() {
        Log.d(TAG, "setInitialDateAndLoadSlots elindult.");
        Calendar today = Calendar.getInstance();
        selectedCalendar.setTime(today.getTime());

        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY) {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 2);
            Log.d(TAG, "Ma szombat, a kiválasztott dátum a következő hétfőre állítva.");
        } else if (dayOfWeek == Calendar.SUNDAY) {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "Ma vasárnap, a kiválasztott dátum a következő hétfőre állítva.");
        }

        updateDateLabel();
        loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime()));
    }

    private void showDatePickerDialog() {
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
        tvSelectedDate.setText(dateFormatForDisplay.format(selectedCalendar.getTime()));
        Log.d(TAG, "Dátum címke frissítve: " + tvSelectedDate.getText().toString());
    }

    private void loadTimeSlotsForDate(String dateString) {
        Log.i(TAG, "loadTimeSlotsForDate hívva a következő dátummal: " + dateString);
        progressBarBooking.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
        tvNoSlotsMessage.setVisibility(View.GONE);

        Calendar tempCal = Calendar.getInstance();
        try {
            tempCal.setTime(dateFormatForFirebase.parse(dateString));
        } catch (ParseException e) {
            Log.e(TAG, "Hiba a dátum ('" + dateString + "') parse-olásakor a hétvége ellenőrzéshez.", e);
            progressBarBooking.setVisibility(View.GONE);
            tvNoSlotsMessage.setText("Hibás dátum formátum.");
            tvNoSlotsMessage.setVisibility(View.VISIBLE);
            return;
        }

        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            Log.d(TAG, "Kiválasztott nap (" + dateString + ") hétvége, nem töltünk be időpontokat.");
            Toast.makeText(this, "Hétvégén nincs időpontfoglalás.", Toast.LENGTH_LONG).show();
            currentTimeSlotsList.clear();
            timeSlotAdapter.notifyDataSetChanged();
            progressBarBooking.setVisibility(View.GONE);
            tvNoSlotsMessage.setText("Hétvégén nincs időpontfoglalás. Kérjük, válasszon hétköznapot.");
            tvNoSlotsMessage.setVisibility(View.VISIBLE);
            return;
        }

        firebaseHelper.getTimeSlotsForDate(dateString, new FirebaseHelper.OnTimeSlotsLoadedListener() {
            @Override
            public void onLoaded(List<TimeSlot> timeSlotsFromFirebase) {
                Log.d(TAG, "FirebaseHelper.onLoaded: " + timeSlotsFromFirebase.size() + " időpont (nyers) betöltve " + dateString + " napra az adatbázisból.");
                currentTimeSlotsList.clear();

                if (!timeSlotsFromFirebase.isEmpty()) {
                    for (TimeSlot slot : timeSlotsFromFirebase) {
                        try {
                            if (slot.getTime() != null && slot.getTime().matches("\\d{2}:\\d{2}")) {
                                int hour = Integer.parseInt(slot.getTime().substring(0, 2));
                                if (hour >= 8 && hour < 18) {
                                    currentTimeSlotsList.add(slot);
                                } else {
                                    Log.w(TAG, "Kiszűrt időpont (órán kívül esik): " + slot.getTime() + " a " + dateString + " napon.");
                                }
                            } else {
                                Log.w(TAG, "Hibás idő formátum vagy null idő az időpontban: " + slot.toString());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Hiba az időpont feldolgozásakor az óra ellenőrzésénél: " + slot.toString(), e);
                        }
                    }
                    Collections.sort(currentTimeSlotsList, Comparator.comparing(TimeSlot::getTime));
                }

                timeSlotAdapter.notifyDataSetChanged();
                progressBarBooking.setVisibility(View.GONE);

                if (currentTimeSlotsList.isEmpty()) {
                    Log.d(TAG, "Nincsenek megjeleníthető (szűrt) időpontok " + dateString + " napra.");
                    tvNoSlotsMessage.setText("Nincsenek elérhető időpontok ezen a napon.");
                    tvNoSlotsMessage.setVisibility(View.VISIBLE);
                    rvTimeSlots.setVisibility(View.GONE);
                } else {
                    tvNoSlotsMessage.setVisibility(View.GONE);
                    rvTimeSlots.setVisibility(View.VISIBLE);
                }
                Log.d(TAG, currentTimeSlotsList.size() + " időpont jelenik meg a RecyclerView-ban a " + dateString + " napra.");
            }

            @Override
            public void onError(DatabaseError databaseError) {
                Log.e(TAG, "Hiba az időpontok lekérdezésekor a FirebaseHelperből: " + databaseError.getMessage());
                Toast.makeText(BookingActivity.this, "Hiba az időpontok lekérdezésekor.", Toast.LENGTH_LONG).show();
                progressBarBooking.setVisibility(View.GONE);
                tvNoSlotsMessage.setText("Hiba történt az időpontok betöltése közben.");
                tvNoSlotsMessage.setVisibility(View.VISIBLE);
                rvTimeSlots.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onTimeSlotClicked(TimeSlot timeSlot) {
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
            // Itt nem csinálunk semmit a kiválasztáskor, a logikát a "Lefoglalom" gomb kezeli
        });

        builder.setPositiveButton("Lefoglalom", (dialog, whichButton) -> {
            int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
            if (selectedPosition == -1) {
                Toast.makeText(BookingActivity.this, "Kérjük, válasszon masszázs típust!", Toast.LENGTH_SHORT).show();
                // Nem zárjuk be a dialógust, hogy a felhasználó választhasson
                // Ha azt akarod, hogy ilyenkor se záródjon be, akkor trükközni kell a dialógus gombjainak felüldefiniálásával.
                // Egyelőre ez a Toast elég visszajelzésnek, és a felhasználó újra próbálkozhat.
                return;
            }
            String selectedMassageType = massageTypes[selectedPosition];
            Log.d(TAG, "Foglalás megerősítve: " + selectedMassageType + " az időpontra: " + timeSlot.getId());
            progressBarBooking.setVisibility(View.VISIBLE);

            firebaseHelper.bookSlot(
                    timeSlot.getId(),
                    userId,
                    selectedMassageType,
                    timeSlot.getDate(),
                    timeSlot.getTime(),
                    new FirebaseHelper.OnBookingCompleteListener() {
                        @Override
                        public void onSuccess() {
                            progressBarBooking.setVisibility(View.GONE);
                            Log.d(TAG, "Sikeres foglalás (callback). Időpont: " + timeSlot.getId());
                            Toast.makeText(BookingActivity.this, "Időpont sikeresen lefoglalva!", Toast.LENGTH_LONG).show();
                            loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime()));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            progressBarBooking.setVisibility(View.GONE);
                            Log.e(TAG, "Foglalás sikertelen (callback). Időpont: " + timeSlot.getId(), e);
                            Toast.makeText(BookingActivity.this, "Hiba a foglalás során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime()));
                        }
                    }
            );
            // A dialógus automatikusan bezáródik a setPositiveButton eseménykezelőjének lefutása után.
        });
        builder.setNegativeButton("Mégse", (dialog, whichButton) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
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
