package com.example.bookingmassage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingmassage.R;
import com.example.bookingmassage.Appointment; // Győződj meg róla, hogy a csomagnév helyes

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList; // Hozzáadva az ArrayList importja
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private static final String TAG = "AppointmentAdapter";
    private List<Appointment> appointmentList; // Az adapter belső listája
    private OnAppointmentActionListener listener;
    private Context context; // Szükséges lehet a formázáshoz vagy erőforrásokhoz
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    // Magyar formátum a nap nevével
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));


    // Interface a törlés és szerkesztés gombok eseményeinek kezelésére az Activity-ben
    public interface OnAppointmentActionListener {
        void onEditClicked(Appointment appointment);
        void onDeleteClicked(Appointment appointment);
    }

    public AppointmentAdapter(List<Appointment> initialAppointmentList, OnAppointmentActionListener listener, Context context) {
        // Fontos: Hozzunk létre egy ÚJ listát az adapteren belül, hogy ne az Activity-ben lévő
        // ugyanazt a referenciát használjuk, ami problémákat okozhat a clear()/addAll() során.
        this.appointmentList = new ArrayList<>(initialAppointmentList != null ? initialAppointmentList : new ArrayList<>());
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // A context-et itt is beállíthatjuk, ha a konstruktorban nem tettük meg, vagy frissíteni akarjuk
        if (this.context == null) {
            this.context = parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment currentAppointment = appointmentList.get(position);

        if (currentAppointment == null) {
            Log.e(TAG, "Appointment objektum null a " + position + ". pozíción. ViewHolder ürítése.");
            holder.tvAppointmentDate.setText("N/A");
            holder.tvAppointmentTime.setText("N/A");
            holder.tvAppointmentMassageType.setText("N/A");
            holder.btnEditAppointment.setVisibility(View.GONE);
            holder.btnDeleteAppointment.setVisibility(View.GONE);
            return;
        }

        // Dátum formázása
        String formattedDate = currentAppointment.getDate(); // Alapértelmezett érték
        if (currentAppointment.getDate() != null) {
            try {
                Calendar cal = Calendar.getInstance();
                // A Firebase-ben tárolt "yyyy-MM-dd" formátumú dátumot parse-oljuk
                cal.setTime(inputDateFormat.parse(currentAppointment.getDate()));
                // Majd a kívánt "yyyy. MMMM dd. (EEEE)" formátumra alakítjuk magyarul
                formattedDate = displayDateFormat.format(cal.getTime());
            } catch (ParseException e) {
                Log.e(TAG, "Hiba a dátum (" + currentAppointment.getDate() + ") formázásakor: ", e);
                // Ha hiba van, az eredeti (vagy egy hibaüzenet) marad
            }
        }

        holder.tvAppointmentDate.setText(formattedDate != null ? formattedDate : "N/A");
        holder.tvAppointmentTime.setText(currentAppointment.getTime() != null ? currentAppointment.getTime() : "N/A");

        String serviceType = currentAppointment.getServiceType();
        if (serviceType != null && !serviceType.equalsIgnoreCase("null")) { // Ellenőrizzük a "null" stringet is
            holder.tvAppointmentMassageType.setText(serviceType);
        } else {
            holder.tvAppointmentMassageType.setText("Nincs megadva"); // Vagy valami más placeholder
            Log.w(TAG, "ServiceType null vagy 'null' string a következő foglaláshoz: " + currentAppointment.getAppointmentId());
        }


        if (listener != null) {
            holder.btnEditAppointment.setVisibility(View.VISIBLE);
            holder.btnDeleteAppointment.setVisibility(View.VISIBLE);
            holder.btnEditAppointment.setOnClickListener(v -> listener.onEditClicked(currentAppointment));
            holder.btnDeleteAppointment.setOnClickListener(v -> listener.onDeleteClicked(currentAppointment));
        } else {
            holder.btnEditAppointment.setVisibility(View.GONE);
            holder.btnDeleteAppointment.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList != null ? appointmentList.size() : 0;
    }

    /**
     * Frissíti az adapterben lévő adatokat az új listával.
     * @param newAppointments Az új foglalások listája.
     */
    public void updateAppointments(List<Appointment> newAppointments) {
        this.appointmentList.clear(); // Először töröljük az adapter belső listáját
        if (newAppointments != null) {
            this.appointmentList.addAll(newAppointments); // Majd hozzáadjuk az új elemeket
            Log.d(TAG, "updateAppointments: " + newAppointments.size() + " új elem hozzáadva az adapter listájához.");
        } else {
            Log.d(TAG, "updateAppointments: newAppointments lista null volt, az adapter listája üres maradt.");
        }
        notifyDataSetChanged(); // Értesítjük az adaptert a változásról
        Log.d(TAG, "Adapter adatai frissítve, új elemszám (getItemCount): " + getItemCount());
    }


    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppointmentDate, tvAppointmentTime, tvAppointmentMassageType;
        ImageButton btnEditAppointment, btnDeleteAppointment;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvAppointmentMassageType = itemView.findViewById(R.id.tvAppointmentMassageType);
            btnEditAppointment = itemView.findViewById(R.id.btnEditAppointment);
            btnDeleteAppointment = itemView.findViewById(R.id.btnDeleteAppointment);

            if (tvAppointmentDate == null || tvAppointmentTime == null || tvAppointmentMassageType == null || btnEditAppointment == null || btnDeleteAppointment == null) {
                Log.e(TAG, "ViewHolder: Egy vagy több UI elem null. Ellenőrizd az item_user_appointment.xml ID-kat!");
            }
        }
    }
}
