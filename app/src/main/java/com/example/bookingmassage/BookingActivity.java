package com.example.bookingmassage;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingmassage.TimeSlotAdapter;
import com.example.bookingmassage.TimeSlot;
import com.example.bookingmassage.FirebaseHelper;
// Nincs szükség a com.google.firebase.database.DatabaseError-ra

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
    private ImageButton btnBackBooking;
    private RecyclerView rvTimeSlots;
    private ProgressBar progressBarBooking;
    private TimeSlotAdapter timeSlotAdapter;

    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private List<TimeSlot> currentTimeSlotsList = new ArrayList<>();
    private Calendar selectedCalendar = Calendar.getInstance();
    // Magyar nyelvű, napnevet is tartalmazó formátum a megjelenítéshez
    private SimpleDateFormat dateFormatForDisplay = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));
    // Formátum a Firebase-ben tárolt és query-hez használt dátumokhoz
    private SimpleDateFormat dateFormatForFirebase = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        Log.d(TAG, "onCreate elindult.");

        // UI Elemek inicializálása
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnBackBooking = findViewById(R.id.btnBackBooking);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        progressBarBooking = findViewById(R.id.progressBarBooking);
        tvNoSlotsMessage = findViewById(R.id.tvNoSlotsMessage);

        if (tvSelectedDate == null || btnPickDate == null || btnBackBooking == null ||
                rvTimeSlots == null || progressBarBooking == null || tvNoSlotsMessage == null) {
            Log.e(TAG, "Egy vagy több UI elem null a findViewById után! Ellenőrizd az activity_booking.xml ID-kat.");
            Toast.makeText(this, "Hiba a felület betöltésekor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Firebase inicializálás
        firebaseHelper = new FirebaseHelper(); // Ennek most Firestore logikát kell tartalmaznia
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
        setInitialDateAndLoadSlots(); // Kezdeti dátum beállítása és időpontok betöltése

        btnPickDate.setOnClickListener(v -> showDatePickerDialog());
        btnBackBooking.setOnClickListener(v -> {
            Log.d(TAG, "Vissza gomb (btnBackBooking) megnyomva.");
            onBackPressed();
        });
    }

    private void setupRecyclerView() {
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3)); // 3 oszlopos grid
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
        } else if (dayOfWeek == Calendar.SUNDAY) {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        // Opcionális: Ha ma már elmúlt 17:00, és hétköznap van, ugorjon a következő munkanapra
        // ... (ezt a logikát hozzáadhatod, ha szükséges)

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

        datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis() - 1000); // Mai naptól
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        tvSelectedDate.setText(dateFormatForDisplay.format(selectedCalendar.getTime()));
        Log.d(TAG, "Dátum címke frissítve: " + tvSelectedDate.getText().toString());
    }

    private void loadTimeSlotsForDate(String dateString) {
        Log.i(TAG, "Firestore: loadTimeSlotsForDate hívva a következő dátummal: " + dateString);
        progressBarBooking.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
        tvNoSlotsMessage.setVisibility(View.GONE);

        Calendar tempCal = Calendar.getInstance();
        try {
            tempCal.setTime(dateFormatForFirebase.parse(dateString));
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
            timeSlotAdapter.notifyDataSetChanged();
            progressBarBooking.setVisibility(View.GONE);
            updateUIVisibility(true, "Hétvégén nincs időpontfoglalás. Kérjük, válasszon hétköznapot.");
            return;
        }

        // A FirebaseHelper.getTimeSlotsForDate Firestore verzióját hívjuk
        firebaseHelper.getTimeSlotsForDate(dateString, new FirebaseHelper.OnTimeSlotsLoadedListener() {
            @Override
            public void onLoaded(List<TimeSlot> timeSlotsFromFirestore) {
                Log.d(TAG, "Firestore OnTimeSlotsLoadedListener.onLoaded: " + timeSlotsFromFirestore.size() + " időpont (nyers) betöltve " + dateString + " napra.");
                currentTimeSlotsList.clear();

                if (!timeSlotsFromFirestore.isEmpty()) {
                    for (TimeSlot slot : timeSlotsFromFirestore) {
                        // A Firestore query már tartalmazza a rendezést 'time' alapján,
                        // és a helper a 'date' alapján szűr.
                        // Az órák (8-17) szűrését a time_slots generálásának kell biztosítania.
                        // De egy plusz ellenőrzés itt nem árt, ha a generálás nem tökéletes.
                        try {
                            if (slot.getTime() != null && slot.getTime().matches("\\d{2}:\\d{2}")) {
                                int hour = Integer.parseInt(slot.getTime().substring(0, 2));
                                if (hour >= 8 && hour < 18) {
                                    currentTimeSlotsList.add(slot);
                                } else {
                                    Log.w(TAG, "Kiszűrt időpont (órán kívül esik Firestore-ból): " + slot.getTime() + " a " + dateString + " napon.");
                                }
                            } else {
                                Log.w(TAG, "Hibás idő formátum vagy null idő a Firestore időpontban: " + slot.toString());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Hiba az időpont feldolgozásakor (Firestore): " + slot.toString(), e);
                        }
                    }
                    // Ha a Firestore query nem rendezne, itt kellene:
                    // Collections.sort(currentTimeSlotsList, Comparator.comparing(TimeSlot::getTime));
                }

                timeSlotAdapter.updateTimeSlots(currentTimeSlotsList); // Az adapter update metódusát hívjuk
                progressBarBooking.setVisibility(View.GONE);
                updateUIVisibility(currentTimeSlotsList.isEmpty(), "Nincsenek elérhető időpontok ezen a napon.");
                Log.d(TAG, currentTimeSlotsList.size() + " időpont jelenik meg a RecyclerView-ban a " + dateString + " napra.");
            }

            @Override
            public void onError(Exception e) { // A paraméter típusa Exception
                Log.e(TAG, "Hiba az időpontok lekérdezésekor a FirebaseHelperből (Firestore): ", e);
                Toast.makeText(BookingActivity.this, "Hiba az időpontok lekérdezésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                progressBarBooking.setVisibility(View.GONE);
                updateUIVisibility(true, "Hiba történt az időpontok betöltése közben.");
            }
        });
    }

    /**
     * Segédfüggvény a UI elemek láthatóságának kezelésére.
     */
    private void updateUIVisibility(boolean showNoSlotsMessage, @Nullable String message) {
        if (showNoSlotsMessage) {
            tvNoSlotsMessage.setText(message != null ? message : "Nincsenek elérhető időpontok ezen a napon.");
            tvNoSlotsMessage.setVisibility(View.VISIBLE);
            rvTimeSlots.setVisibility(View.GONE);
        } else {
            tvNoSlotsMessage.setVisibility(View.GONE);
            rvTimeSlots.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "updateUIVisibility: showNoSlotsMessage=" + showNoSlotsMessage + ", rvVisible=" + (rvTimeSlots.getVisibility() == View.VISIBLE));
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
            progressBarBooking.setVisibility(View.VISIBLE);

            // A FirebaseHelper.bookSlot Firestore verzióját hívjuk
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
                            Log.d(TAG, "Firestore: Sikeres foglalás (callback). Időpont: " + timeSlot.getId());
                            Toast.makeText(BookingActivity.this, "Időpont sikeresen lefoglalva!", Toast.LENGTH_LONG).show();
                            loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime())); // Frissítjük a listát
                        }

                        @Override
                        public void onFailure(Exception e) { // Exception a paraméter
                            progressBarBooking.setVisibility(View.GONE);
                            Log.e(TAG, "Firestore: Foglalás sikertelen (callback). Időpont: " + timeSlot.getId(), e);
                            Toast.makeText(BookingActivity.this, "Hiba a foglalás során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            loadTimeSlotsForDate(dateFormatForFirebase.format(selectedCalendar.getTime())); // Frissítés hiba esetén is
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
        // A loadTimeSlotsForDate már az onCreate-ben és a dátumválasztáskor is meghívódik,
        // itt nem feltétlenül szükséges újra hívni, hacsak nincs specifikus ok.
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
        // Ha a getTimeSlotsForDate addSnapshotListener-t használna, itt kellene leiratkozni.
        // De mivel .get()-et használunk, ami egyszeri lekérés, itt nincs teendő a listenerrel.
    }
}
