package com.example.bookingmassage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Ha Button-t használsz az item layoutban
import android.widget.ImageButton; // Ha ImageButton-t használsz
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingmassage.R; // Győződj meg róla, hogy az R import helyes
import com.example.bookingmassage.Appointment; // Győződj meg róla, hogy a modell import helyes

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private static final String TAG = "AppointmentAdapter";
    private List<Appointment> appointmentListInternal; // Az adapter belső listája
    private OnAppointmentActionListener actionListener;
    private Context context;

    // Dátumformázók
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));


    // Interface a törlés és szerkesztés gombok eseményeinek kezelésére az Activity-ben
    public interface OnAppointmentActionListener {
        void onEditClicked(Appointment appointment);
        void onDeleteClicked(Appointment appointment);
    }

    public AppointmentAdapter(List<Appointment> initialAppointmentList, OnAppointmentActionListener listener, Context context) {
        // Másolatot készítünk a listáról, hogy az adapternek saját példánya legyen,
        // elkerülve a referencia alapú problémákat.
        this.appointmentListInternal = new ArrayList<>(initialAppointmentList != null ? initialAppointmentList : new ArrayList<>());
        this.actionListener = listener;
        this.context = context; // A context hasznos lehet erőforrásokhoz vagy más műveletekhez
        Log.d(TAG, "Adapter létrehozva, kezdeti elemszám: " + this.appointmentListInternal.size());
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // A context-et itt is beállíthatjuk, ha a konstruktorban nem lenne, vagy frissíteni kellene.
        // De általában a konstruktorban átadott context elegendő.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment currentAppointment = appointmentListInternal.get(position);

        if (currentAppointment == null) {
            Log.e(TAG, "onBindViewHolder: Appointment objektum null a " + position + ". pozíción. ViewHolder ürítése.");
            // Alapértelmezett értékek vagy üresen hagyás, hogy ne okozzon hibát
            holder.tvAppointmentDate.setText("N/A");
            holder.tvAppointmentTime.setText("N/A");
            holder.tvAppointmentMassageType.setText("N/A");
            // A gombok láthatóságát is kezelhetnénk, ha az adat hibás
            holder.btnEditAppointment.setVisibility(View.GONE);
            holder.btnDeleteAppointment.setVisibility(View.GONE);
            return;
        }
        Log.v(TAG, "onBindViewHolder pozíció: " + position + ", Adat: " + currentAppointment.toString());


        // Dátum formázása
        String formattedDate = currentAppointment.getDate(); // Alapértelmezett érték, ha a parse nem sikerül
        if (currentAppointment.getDate() != null && !currentAppointment.getDate().isEmpty()) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(inputDateFormat.parse(currentAppointment.getDate()));
                formattedDate = displayDateFormat.format(cal.getTime());
            } catch (ParseException e) {
                Log.e(TAG, "Hiba a dátum (" + currentAppointment.getDate() + ") formázásakor: ", e);
                // Ha hiba van, az eredeti (vagy egy hibaüzenet) marad, vagy "N/A"
                formattedDate = "Dátumhiba";
            }
        } else {
            formattedDate = "Nincs dátum";
        }
        holder.tvAppointmentDate.setText(formattedDate);

        holder.tvAppointmentTime.setText(currentAppointment.getTime() != null ? currentAppointment.getTime() : "N/A");

        // Masszázs típusának megjelenítése, "null" string kezelésével
        String serviceType = currentAppointment.getMassageType();
        if (serviceType != null && !serviceType.trim().isEmpty() && !serviceType.equalsIgnoreCase("null")) {
            holder.tvAppointmentMassageType.setText(serviceType);
        } else {
            holder.tvAppointmentMassageType.setText("Nincs megadva");
            Log.w(TAG, "ServiceType null, üres, vagy 'null' string a foglalásnál: " +
                    (currentAppointment.getAppointmentId() != null ? currentAppointment.getAppointmentId() : "ID_NÉLKÜL") +
                    ", Kapott érték: '" + serviceType + "'");
        }

        // Listener beállítása a gombokhoz
        if (actionListener != null) {
            holder.btnEditAppointment.setVisibility(View.VISIBLE);
            holder.btnDeleteAppointment.setVisibility(View.VISIBLE);

            holder.btnEditAppointment.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Módosít gomb lenyomva, pozíció: " + currentPosition);
                    actionListener.onEditClicked(appointmentListInternal.get(currentPosition));
                } else {
                    Log.w(TAG, "Módosít gomb: Érvénytelen pozíció (NO_POSITION).");
                }
            });

            holder.btnDeleteAppointment.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Töröl gomb lenyomva, pozíció: " + currentPosition);
                    actionListener.onDeleteClicked(appointmentListInternal.get(currentPosition));
                } else {
                    Log.w(TAG, "Töröl gomb: Érvénytelen pozíció (NO_POSITION).");
                }
            });
        } else {
            // Ha nincs listener, elrejtjük a gombokat
            Log.w(TAG, "Nincs OnAppointmentActionListener beállítva, a műveleti gombok elrejtve.");
            holder.btnEditAppointment.setVisibility(View.GONE);
            holder.btnDeleteAppointment.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        int count = appointmentListInternal != null ? appointmentListInternal.size() : 0;
        Log.v(TAG, "getItemCount: " + count);
        return count;
    }

    /**
     * Frissíti az adapterben lévő adatokat az új listával.
     * @param newAppointments Az új foglalások listája.
     */
    public void updateAppointments(List<Appointment> newAppointments) {
        this.appointmentListInternal.clear(); // Először töröljük az adapter belső listáját
        if (newAppointments != null) {
            this.appointmentListInternal.addAll(newAppointments); // Majd hozzáadjuk az új elemeket
            Log.d(TAG, "updateAppointments: " + newAppointments.size() + " új elem hozzáadva az adapter belső listájához.");
        } else {
            Log.d(TAG, "updateAppointments: newAppointments lista null volt, az adapter listája üres maradt.");
        }
        notifyDataSetChanged(); // Értesítjük az adaptert a változásról, hogy újra rajzolja a listát
        Log.d(TAG, "Adapter adatai frissítve (notifyDataSetChanged hívva), új elemszám (getItemCount): " + getItemCount());
    }


    // ViewHolder Osztály
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppointmentDate, tvAppointmentTime, tvAppointmentMassageType;
        // A gombok típusa attól függ, hogy az item_user_appointment.xml-ben Button vagy ImageButton van-e
        Button btnEditAppointment;    // Ha Button-t használsz
        Button btnDeleteAppointment;  // Ha Button-t használsz
        // VAGY
        // ImageButton btnEditAppointment; // Ha ImageButton-t használsz
        // ImageButton btnDeleteAppointment; // Ha ImageButton-t használsz


        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvAppointmentMassageType = itemView.findViewById(R.id.tvAppointmentMassageType);

            // Győződj meg róla, hogy a gombok típusa megegyezik a deklarációval
            btnEditAppointment = itemView.findViewById(R.id.btnEditAppointment);
            btnDeleteAppointment = itemView.findViewById(R.id.btnDeleteAppointment);

            // Null ellenőrzések a ViewHolder elemeire
            if (tvAppointmentDate == null) Log.e(TAG, "ViewHolder: tvAppointmentDate is NULL");
            if (tvAppointmentTime == null) Log.e(TAG, "ViewHolder: tvAppointmentTime is NULL");
            if (tvAppointmentMassageType == null) Log.e(TAG, "ViewHolder: tvAppointmentMassageType is NULL");
            if (btnEditAppointment == null) Log.e(TAG, "ViewHolder: btnEditAppointment is NULL");
            if (btnDeleteAppointment == null) Log.e(TAG, "ViewHolder: btnDeleteAppointment is NULL");
        }
    }
}
