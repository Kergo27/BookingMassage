package com.example.bookingmassage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Fontos: Button importálása
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private static final String TAG = "AppointmentAdapter";
    private List<Appointment> appointmentList;
    private OnAppointmentActionListener listener;
    private Context context;
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy. MMMM dd. (EEEE)", new Locale("hu", "HU"));


    public interface OnAppointmentActionListener {
        void onEditClicked(Appointment appointment);
        void onDeleteClicked(Appointment appointment);
    }

    public AppointmentAdapter(List<Appointment> initialAppointmentList, OnAppointmentActionListener listener, Context context) {
        this.appointmentList = new ArrayList<>(initialAppointmentList != null ? initialAppointmentList : new ArrayList<>());
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
            Log.e(TAG, "Appointment objektum null a " + position + ". pozíción.");
            holder.tvAppointmentDate.setText("N/A");
            holder.tvAppointmentTime.setText("N/A");
            holder.tvAppointmentMassageType.setText("N/A"); // Alapértelmezett érték
            holder.btnEditAppointment.setVisibility(View.GONE);
            holder.btnDeleteAppointment.setVisibility(View.GONE);
            return;
        }


        String formattedDate = currentAppointment.getDate();
        if (currentAppointment.getDate() != null) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(inputDateFormat.parse(currentAppointment.getDate()));
                formattedDate = displayDateFormat.format(cal.getTime());
            } catch (ParseException e) {
                Log.e(TAG, "Hiba a dátum (" + currentAppointment.getDate() + ") formázásakor: ", e);
            }
        }

        holder.tvAppointmentDate.setText(formattedDate != null ? formattedDate : "N/A");
        holder.tvAppointmentTime.setText(currentAppointment.getTime() != null ? currentAppointment.getTime() : "N/A");

        String serviceType = currentAppointment.getMassageType();
        if (serviceType != null && !serviceType.trim().isEmpty() && !serviceType.equalsIgnoreCase("null")) {
            // Ha a serviceType nem null, nem csak whitespace-ekből áll, ÉS nem a "null" string (kis-nagybetű érzéketlenül)
            holder.tvAppointmentMassageType.setText(serviceType);
        } else {
            // Ha null, üres, vagy a "null" string, akkor egy placeholder szöveget jelenítünk meg
            holder.tvAppointmentMassageType.setText("Nincs megadva"); // Vagy pl. "-", "Ismeretlen"
            Log.w(TAG, "ServiceType null, üres, vagy 'null' string a következő foglaláshoz: " +
                    (currentAppointment.getAppointmentId() != null ? currentAppointment.getAppointmentId() : "ID_HIÁNYZIK") +
                    ", Érték: '" + serviceType + "'");
        }

        // Listener beállítása a gombokhoz
        if (listener != null) {
            // Biztosítjuk, hogy a gombok láthatóak legyenek, ha van listener
            holder.btnEditAppointment.setVisibility(View.VISIBLE);
            holder.btnDeleteAppointment.setVisibility(View.VISIBLE);

            holder.btnEditAppointment.setOnClickListener(v -> {
                Log.d(TAG, "Módosít gomb lenyomva, pozíció: " + holder.getAdapterPosition());
                if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) { // Ellenőrzés, hogy a pozíció érvényes-e
                    listener.onEditClicked(appointmentList.get(holder.getAdapterPosition()));
                }
            });
            holder.btnDeleteAppointment.setOnClickListener(v -> {
                Log.d(TAG, "Töröl gomb lenyomva, pozíció: " + holder.getAdapterPosition());
                if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteClicked(appointmentList.get(holder.getAdapterPosition()));
                }
            });
        } else {
            // Ha nincs listener, elrejtjük a gombokat, hogy ne okozzanak hibát
            Log.w(TAG, "Nincs OnAppointmentActionListener beállítva, a gombok elrejtve.");
            holder.btnEditAppointment.setVisibility(View.GONE);
            holder.btnDeleteAppointment.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList != null ? appointmentList.size() : 0;
    }

    public void updateAppointments(List<Appointment> newAppointments) {
        this.appointmentList.clear();
        if (newAppointments != null) {
            this.appointmentList.addAll(newAppointments);
        }
        notifyDataSetChanged();
        Log.d(TAG, "Adapter adatai frissítve, új elemszám: " + getItemCount());
    }

    // ViewHolder Osztály - Fontos, hogy Button típust használjon
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppointmentDate, tvAppointmentTime, tvAppointmentMassageType;
        Button btnEditAppointment, btnDeleteAppointment; // ITT BUTTON-NAK KELL LENNIE

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvAppointmentMassageType = itemView.findViewById(R.id.tvAppointmentMassageType);
            btnEditAppointment = itemView.findViewById(R.id.btnEditAppointment); // XML ID alapján
            btnDeleteAppointment = itemView.findViewById(R.id.btnDeleteAppointment); // XML ID alapján

            if (btnEditAppointment == null) {
                Log.e(TAG, "ViewHolder: btnEditAppointment (Button) nem található az item_user_appointment.xml-ben! Ellenőrizd az ID-t.");
            }
            if (btnDeleteAppointment == null) {
                Log.e(TAG, "ViewHolder: btnDeleteAppointment (Button) nem található az item_user_appointment.xml-ben! Ellenőrizd az ID-t.");
            }
        }
    }
}
