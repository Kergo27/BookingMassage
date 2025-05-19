package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

// Győződj meg róla, hogy a TimeSlot modell a helyes csomagból van importálva
import com.example.bookingmassage.TimeSlot;

import java.util.ArrayList;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private static final String TAG = "TimeSlotAdapter"; // Egyedi TAG az adapternek
    private List<TimeSlot> timeSlotsList;
    private OnTimeSlotClickListener clickListener;
    private Context context;

    public interface OnTimeSlotClickListener {
        void onTimeSlotClicked(TimeSlot timeSlot);
    }

    public TimeSlotAdapter(List<TimeSlot> initialTimeSlots, OnTimeSlotClickListener listener) {
        // Biztosítjuk, hogy a lista soha ne legyen null
        this.timeSlotsList = new ArrayList<>(initialTimeSlots != null ? initialTimeSlots : new ArrayList<>());
        this.clickListener = listener;
        Log.d(TAG, "Adapter inicializálva " + this.timeSlotsList.size() + " elemmel.");
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (this.context == null) {
            this.context = parent.getContext(); // Context beszerzése a parent-ből
        }
        Log.d(TAG, "onCreateViewHolder: View létrehozása az item_time_slot layoutból.");
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        TimeSlot currentTimeSlot = timeSlotsList.get(position);

        if (currentTimeSlot == null) {
            Log.e(TAG, "onBindViewHolder: currentTimeSlot null a " + position + ". pozíción. Ez nem fordulhatna elő.");
            // Alapértelmezett vagy hiba állapot beállítása a UI-n
            holder.tvTime.setText("Hiba");
            holder.tvStatus.setText("");
            holder.itemView.setClickable(false);
            holder.itemView.setEnabled(false);
            return;
        }

        Log.d(TAG, "Binding pozíció: " + position +
                ", Idő: '" + (currentTimeSlot.getTime() != null ? currentTimeSlot.getTime() : "NULL IDŐ") +
                "', Szabad: " + currentTimeSlot.isAvailable());

        // Időpont beállítása
        if (currentTimeSlot.getTime() != null && !currentTimeSlot.getTime().isEmpty()) {
            holder.tvTime.setText(currentTimeSlot.getTime());
        } else {
            holder.tvTime.setText("N/A"); // Ha az idő null vagy üres
            Log.w(TAG, "currentTimeSlot.getTime() null vagy üres a " + position + ". pozíción.");
        }

        // Státusz és kinézet beállítása
        if (currentTimeSlot.isAvailable()) {
            holder.tvStatus.setText("Szabad");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.item_time_slot_text_available));
            if (holder.cardViewRoot != null) {
                holder.cardViewRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.item_time_slot_bg_available));
                holder.cardViewRoot.setAlpha(1.0f);
            }
            holder.itemView.setClickable(true);
            holder.itemView.setEnabled(true);
        } else {
            holder.tvStatus.setText("Foglalt");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.item_time_slot_text_booked));
            if (holder.cardViewRoot != null) {
                holder.cardViewRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.item_time_slot_bg_booked));
                holder.cardViewRoot.setAlpha(0.7f); // Enyhén áttetszőbb a foglalt
            }
            holder.itemView.setClickable(false);
            holder.itemView.setEnabled(false);
        }

        // Kattintáskezelő beállítása az egész itemre (a kártyára)
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Kattintás az itemre: " + currentTimeSlot.getTime() + ", Szabad: " + currentTimeSlot.isAvailable());
            if (currentTimeSlot.isAvailable() && clickListener != null) {
                clickListener.onTimeSlotClicked(currentTimeSlot);
            } else if (!currentTimeSlot.isAvailable()) {
                // Opcionális: Visszajelzés a felhasználónak, hogy az időpont foglalt
                Toast.makeText(context, "Ez az időpont (" + currentTimeSlot.getTime() + ") már foglalt.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = timeSlotsList != null ? timeSlotsList.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    // Metódus az adatok frissítésére az adapterben
    public void updateTimeSlots(List<TimeSlot> newTimeSlots) {
        Log.d(TAG, "updateTimeSlots meghívva, új elemek száma: " + (newTimeSlots != null ? newTimeSlots.size() : "null lista"));
        this.timeSlotsList.clear();
        if (newTimeSlots != null) {
            this.timeSlotsList.addAll(newTimeSlots);
        }
        notifyDataSetChanged(); // Értesíti a RecyclerView-t a változásokról
        Log.d(TAG, "Adapter adatai frissítve, notifyDataSetChanged() meghívva. Új elemszám: " + getItemCount());
    }

    // ViewHolder Osztály
    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;    // Az időpont megjelenítésére
        TextView tvStatus;  // A "Szabad" / "Foglalt" státusz megjelenítésére
        MaterialCardView cardViewRoot; // A gyökér CardView elem

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "TimeSlotViewHolder konstruktor elindult.");

            // A gyökér elem a MaterialCardView, az ID-ja 'cardViewTimeSlotItemRoot' az XML-ben
            // De itt az 'itemView' maga a MaterialCardView, ha a gyökér elem.
            if (itemView instanceof MaterialCardView) {
                cardViewRoot = (MaterialCardView) itemView;
                Log.d(TAG, "cardViewRoot sikeresen castolva MaterialCardView-ra.");
            } else {
                Log.e(TAG, "HIBA: Az itemView nem MaterialCardView! Ellenőrizd az item_time_slot.xml gyökér elemét.");
            }

            // TextView-k inicializálása az XML-ben definiált ID-k alapján
            tvTime = itemView.findViewById(R.id.tvItemTimeSlotTime);
            tvStatus = itemView.findViewById(R.id.tvItemTimeSlotStatus);

            // Null ellenőrzések a hibakereséshez
            if (tvTime == null) {
                Log.e(TAG, "HIBA A VIEWHOLDERBEN: tvTime (R.id.tvItemTimeSlotTime) NEM TALÁLHATÓ!");
            } else {
                Log.d(TAG, "tvTime (R.id.tvItemTimeSlotTime) sikeresen inicializálva.");
            }
            if (tvStatus == null) {
                Log.e(TAG, "HIBA A VIEWHOLDERBEN: tvStatus (R.id.tvItemTimeSlotStatus) NEM TALÁLHATÓ!");
            } else {
                Log.d(TAG, "tvStatus (R.id.tvItemTimeSlotStatus) sikeresen inicializálva.");
            }
        }
    }
}
