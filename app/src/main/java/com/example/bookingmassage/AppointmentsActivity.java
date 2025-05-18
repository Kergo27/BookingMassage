package com.example.bookingmassage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingmassage.AppointmentAdapter;
import com.example.bookingmassage.Appointment;
import com.example.bookingmassage.FirebaseHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppointmentsActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentActionListener {

    private static final String TAG = "AppointmentsActivity";
    public static final int EDIT_APPOINTMENT_REQUEST_CODE = 101;

    private RecyclerView rvUserAppointments;
    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> userAppointmentsList = new ArrayList<>(); // Lista az Activity-ben
    private ProgressBar progressBarAppointments;
    private TextView tvNoUserAppointmentsMessage;
    private ImageButton btnBackAppointments;

    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ValueEventListener appointmentsListener;
    private Query userAppointmentsQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);
        Log.d(TAG, "onCreate elindult.");

        rvUserAppointments = findViewById(R.id.rvUserAppointments);
        progressBarAppointments = findViewById(R.id.progressBarAppointments);
        tvNoUserAppointmentsMessage = findViewById(R.id.tvNoUserAppointmentsMessage);
        btnBackAppointments = findViewById(R.id.btnBackAppointments);

        if (rvUserAppointments == null || progressBarAppointments == null || tvNoUserAppointmentsMessage == null || btnBackAppointments == null) {
            Log.e(TAG, "Hiba: Egy vagy több UI elem null a findViewById után! Ellenőrizd az XML ID-kat!");
            Toast.makeText(this, "Hiba a felület inicializálásakor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        firebaseHelper = new FirebaseHelper();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó. Kérjük, jelentkezzen be.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Log.d(TAG, "Bejelentkezett felhasználó: " + currentUser.getEmail() + ", UID: " + currentUser.getUid());


        btnBackAppointments.setOnClickListener(v -> {
            Log.d(TAG, "Vissza gomb megnyomva.");
            onBackPressed();
        });

        setupRecyclerView();
        loadUserAppointments();
    }

    private void setupRecyclerView() {
        rvUserAppointments.setLayoutManager(new LinearLayoutManager(this));
        // Az adapternek most az Activity-ben lévő userAppointmentsList referenciáját adjuk át,
        // de az adapter konstruktora másolatot készít belőle.
        appointmentAdapter = new AppointmentAdapter(userAppointmentsList, this, this);
        rvUserAppointments.setAdapter(appointmentAdapter);
        Log.d(TAG, "RecyclerView és AppointmentAdapter beállítva.");
    }

    private void loadUserAppointments() {
        if (currentUser == null) {
            Log.e(TAG, "loadUserAppointments: currentUser null, nem tudok lekérdezni.");
            return;
        }
        final String userIdForQuery = currentUser.getUid();
        Log.i(TAG, "loadUserAppointments elindult. UID: " + userIdForQuery);
        progressBarAppointments.setVisibility(View.VISIBLE);
        tvNoUserAppointmentsMessage.setVisibility(View.GONE);
        rvUserAppointments.setVisibility(View.GONE);

        if (appointmentsListener != null && userAppointmentsQuery != null) {
            userAppointmentsQuery.removeEventListener(appointmentsListener);
            Log.d(TAG, "Régi appointmentsListener eltávolítva.");
        }

        userAppointmentsQuery = firebaseHelper.getUserBookedAppointments(userIdForQuery);
        Log.d(TAG, "Firebase Query objektum lekérve: " + userAppointmentsQuery.getRef().toString());

        appointmentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange hívva. Elérési út: " + dataSnapshot.getRef().toString() + ", Snapshot exists: " + dataSnapshot.exists() + ", Children count: " + dataSnapshot.getChildrenCount());
                userAppointmentsList.clear(); // Fontos: Az Activity listáját töröljük
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.v(TAG, "Snapshot feldolgozása, kulcs: " + snapshot.getKey());
                        Appointment appointment = snapshot.getValue(Appointment.class);
                        if (appointment != null) {
                            // Ha az appointmentId-t a Firebase kulcsból akarod venni (de a mi Appointment modellünknek van appointmentId mezője)
                            // if (appointment.getAppointmentId() == null || appointment.getAppointmentId().isEmpty()) {
                            //    appointment.setAppointmentId(snapshot.getKey());
                            // }
                            Log.d(TAG, "Sikeresen deszerializált foglalás: " + appointment.toString());
                            userAppointmentsList.add(appointment);
                        } else {
                            Log.w(TAG, "Nem sikerült deszerializálni az Appointment objektumot a snapshotból: " + snapshot.getKey());
                        }
                    }
                    // Rendezés időrendben (a legfrissebb felül, a bookingTimestamp alapján)
                    if (!userAppointmentsList.isEmpty() && userAppointmentsList.get(0).getBookingTimestamp() > 0) { // Csak akkor rendezzünk, ha van timestamp
                        Collections.sort(userAppointmentsList, Comparator.comparingLong(Appointment::getBookingTimestamp).reversed());
                    } else if (!userAppointmentsList.isEmpty()) {
                        Log.w(TAG, "BookingTimestamp hiányzik vagy 0, nem lehet rendezni timestamp alapján.");
                        // Esetleg rendezzük dátum és idő alapján, ha a timestamp hiányzik
                        Collections.sort(userAppointmentsList, (a1, a2) -> {
                            String dateTime1 = (a1.getDate() != null ? a1.getDate() : "") + (a1.getTime() != null ? a1.getTime() : "");
                            String dateTime2 = (a2.getDate() != null ? a2.getDate() : "") + (a2.getTime() != null ? a2.getTime() : "");
                            return dateTime2.compareTo(dateTime1); // Fordított sorrend (legújabb elöl)
                        });
                    }
                    Log.d(TAG, userAppointmentsList.size() + " foglalás feldolgozva és hozzáadva az Activity listájához.");
                } else {
                    Log.d(TAG, "Nincsenek foglalások ('user_appointments/" + userIdForQuery + "') ehhez a felhasználóhoz az adatbázisban.");
                }

                Log.d(TAG, "Adapter frissítése előtt, userAppointmentsList (Activity-ben) mérete: " + userAppointmentsList.size());
                appointmentAdapter.updateAppointments(userAppointmentsList); // Átadjuk az Activity frissített listáját az adapternek
                progressBarAppointments.setVisibility(View.GONE);

                if (userAppointmentsList.isEmpty()) {
                    tvNoUserAppointmentsMessage.setVisibility(View.VISIBLE);
                    rvUserAppointments.setVisibility(View.GONE);
                    Log.d(TAG, "Lista üres, 'Nincs foglalás' üzenet megjelenítve.");
                } else {
                    tvNoUserAppointmentsMessage.setVisibility(View.GONE);
                    rvUserAppointments.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Lista nem üres, RecyclerView megjelenítve.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Hiba a foglalások lekérdezésekor: ", databaseError.toException());
                Toast.makeText(AppointmentsActivity.this, "Hiba a foglalások betöltésekor.", Toast.LENGTH_SHORT).show();
                progressBarAppointments.setVisibility(View.GONE);
                tvNoUserAppointmentsMessage.setText("Hiba történt a foglalások betöltése közben.");
                tvNoUserAppointmentsMessage.setVisibility(View.VISIBLE);
                rvUserAppointments.setVisibility(View.GONE);
            }
        };
        userAppointmentsQuery.addValueEventListener(appointmentsListener);
        Log.d(TAG, "ValueEventListener hozzáadva a userAppointmentsQuery-hez.");
    }

    @Override
    public void onEditClicked(Appointment appointment) {
        Log.d(TAG, "Szerkesztés gomb megnyomva: " + appointment.toString());
        Intent intent = new Intent(this, EditAppointmentActivity.class);
        intent.putExtra("APPOINTMENT_ID", appointment.getAppointmentId());
        intent.putExtra("USER_ID", appointment.getUserId());
        intent.putExtra("TIME_SLOT_ID", appointment.getTimeSlotId());
        intent.putExtra("CURRENT_MASSAGE_TYPE", appointment.getServiceType());
        intent.putExtra("DATE", appointment.getDate());
        intent.putExtra("TIME", appointment.getTime());
        startActivityForResult(intent, EDIT_APPOINTMENT_REQUEST_CODE);
    }

    @Override
    public void onDeleteClicked(final Appointment appointment) {
        Log.d(TAG, "Törlés gomb megnyomva: " + appointment.toString());
        new AlertDialog.Builder(this)
                .setTitle("Foglalás Törlése")
                .setMessage("Biztosan törölni szeretnéd ezt a foglalást?\n" +
                        (appointment.getDate() != null ? appointment.getDate() : "") + " " +
                        (appointment.getTime() != null ? appointment.getTime() : "") + "\n" +
                        (appointment.getServiceType() != null ? appointment.getServiceType() : ""))
                .setPositiveButton("Törlés", (dialog, which) -> {
                    progressBarAppointments.setVisibility(View.VISIBLE);
                    firebaseHelper.cancelUserAppointment(
                            currentUser.getUid(),
                            appointment.getAppointmentId(),
                            appointment.getTimeSlotId(),
                            new FirebaseHelper.OnOperationCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    // A progressBarAppointments elrejtése a loadUserAppointments onDataChange-ben történik meg.
                                    // Itt nem kell külön, mert a ValueEventListener automatikusan frissít.
                                    Toast.makeText(AppointmentsActivity.this, "Foglalás sikeresen törölve.", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Foglalás (" + appointment.getAppointmentId() + ") törölve, a lista frissülni fog a listener által.");
                                    // Ha nem ValueEventListener-t használnánk, itt kellene manuálisan frissíteni az adaptert:
                                    // userAppointmentsList.remove(appointment);
                                    // appointmentAdapter.updateAppointments(userAppointmentsList);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    progressBarAppointments.setVisibility(View.GONE);
                                    Toast.makeText(AppointmentsActivity.this, "Hiba a foglalás törlésekor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Hiba a foglalás törlésekor (" + appointment.getAppointmentId() + "): ", e);
                                }
                            }
                    );
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_APPOINTMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d(TAG, "EditAppointmentActivity RESULT_OK, lista frissítése implicit módon a ValueEventListener által.");
            Toast.makeText(this, "Foglalás sikeresen módosítva.", Toast.LENGTH_SHORT).show();
            // A ValueEventListener miatt a lista automatikusan frissül, ha az adatbázisban változás történt.
            // Nincs szükség explicit loadUserAppointments() hívásra, ha a listener aktív.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentsListener != null && userAppointmentsQuery != null) {
            userAppointmentsQuery.removeEventListener(appointmentsListener);
            Log.d(TAG, "Appointments listener eltávolítva az onDestroy-ban.");
        }
    }
}
