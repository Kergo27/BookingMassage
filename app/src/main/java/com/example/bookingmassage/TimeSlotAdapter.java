package com.example.bookingmassage;

import android.content.Context;
import android.graphics.Color; // Szükséges lehet, ha közvetlenül színeket állítasz be
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // Ha a layoutod LinearLayout-t használ a színezéshez
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView; // Ha CardView-t használsz az item layoutban
import androidx.core.content.ContextCompat; // A ContextCompat.getColor() használatához
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingmassage.R; // Győződj meg róla, hogy az R import helyes
import com.example.bookingmassage.TimeSlot; // Győződj meg róla, hogy a modell import helyes

import java.util.ArrayList;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private static final String TAG = "TimeSlotAdapter";
    private List<TimeSlot> timeSlotsInternalList; // Az adapter belső listája
    private OnTimeSlotClickListener clickListener;
    private Context context; // Szükséges a ContextCompat.getColor() és erőforrások eléréséhez

    // Interface a kattintások kezelésére az Activity-ben (pl. BookingActivity)
    public interface OnTimeSlotClickListener {
        void onTimeSlotClicked(TimeSlot timeSlot);
    }

    public TimeSlotAdapter(List<TimeSlot> initialTimeSlots, OnTimeSlotClickListener listener) {
        // Másolatot készítünk a listáról, hogy az adapternek saját példánya legyen.
        this.timeSlotsInternalList = new ArrayList<>(initialTimeSlots != null ? initialTimeSlots : new ArrayList<>());
        this.clickListener = listener;
        // A context-et az onCreateViewHolder-ben szerezzük meg a parent-ből,
        // mert az a ViewHolder létrehozásakor már biztosan rendelkezésre áll.
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (this.context == null) {
            this.context = parent.getContext(); // Context megszerzése itt
        }
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_time_slot, parent, false); // Győződj meg róla, hogy az item_time_slot.xml létezik
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        TimeSlot currentTimeSlot = timeSlotsInternalList.get(position);

        if (currentTimeSlot == null) {
            Log.e(TAG, "onBindViewHolder: currentTimeSlot null a " + position + ". pozíción.");
            // Kezelhetnénk ezt az esetet, pl. üres vagy hibaüzenetet megjelenítő itemmel
            holder.tvTimeSlot.setText("Hiba");
            holder.tvSlotStatus.setText("");
            holder.itemView.setClickable(false);
            return;
        }
        Log.v(TAG, "onBindViewHolder pozíció: " + position + ", Adat: " + currentTimeSlot.toString());

        holder.tvTimeSlot.setText(currentTimeSlot.getTime() != null ? currentTimeSlot.getTime() : "N/A");

        if (currentTimeSlot.isAvailable()) {
            holder.tvSlotStatus.setText("Szabad");
            // Használj erőforrásból származó színeket a jobb karbantarthatóságért
            holder.tvSlotStatus.setTextColor(ContextCompat.getColor(context, R.color.available_slot_text_color));
            if (holder.itemLayout != null) { // Ellenőrizzük, hogy a layout elem létezik-e
                holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.available_slot_bg_color));
            } else if (holder.cardViewTimeSlot != null) { // Vagy ha CardView van, annak a hátterét állítjuk
                holder.cardViewTimeSlot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.available_slot_bg_color));
            }
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setClickable(true);
        } else {
            holder.tvSlotStatus.setText("Foglalt");
            holder.tvSlotStatus.setTextColor(ContextCompat.getColor(context, R.color.booked_slot_text_color));
            if (holder.itemLayout != null) {
                holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.booked_slot_bg_color));
            } else if (holder.cardViewTimeSlot != null) {
                holder.cardViewTimeSlot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.booked_slot_bg_color));
            }
            holder.itemView.setAlpha(0.6f); // Kissé áttetszőbb, ha foglalt
            holder.itemView.setClickable(false); // Ne legyen kattintható, ha foglalt
        }

        // Kattintás eseménykezelője
        holder.itemView.setOnClickListener(v -> {
            if (currentTimeSlot.isAvailable() && clickListener != null) {
                Log.d(TAG, "Időpontra kattintva (szabad): " + currentTimeSlot.getTime());
                clickListener.onTimeSlotClicked(currentTimeSlot);
            } else if (!currentTimeSlot.isAvailable()) {
                Log.d(TAG, "Időpontra kattintva (foglalt): " + currentTimeSlot.getTime());
                // Itt megjeleníthetsz egy Toast üzenetet, ha foglalt időpontra kattintanak,
                // bár a setClickable(false) ezt megakadályozza. Ha a setClickable(true) maradna:
                // Toast.makeText(context, "Ez az időpont már foglalt.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = timeSlotsInternalList != null ? timeSlotsInternalList.size() : 0;
        Log.v(TAG, "getItemCount: " + count);
        return count;
    }

    /**
     * Frissíti az adapterben lévő adatokat az új listával.
     * @param newTimeSlots Az új időpontok listája.
     */
    public void updateTimeSlots(List<TimeSlot> newTimeSlots) {
        this.timeSlotsInternalList.clear();
        if (newTimeSlots != null) {
            this.timeSlotsInternalList.addAll(newTimeSlots);
            Log.d(TAG, "updateTimeSlots: " + newTimeSlots.size() + " új elem hozzáadva az adapter belső listájához.");
        } else {
            Log.d(TAG, "updateTimeSlots: newTimeSlots lista null volt, az adapter listája üres maradt.");
        }
        notifyDataSetChanged();
        Log.d(TAG, "Adapter adatai frissítve (notifyDataSetChanged hívva), új elemszám (getItemCount): " + getItemCount());
    }

    // ViewHolder Osztály
    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeSlot;
        TextView tvSlotStatus;
        LinearLayout itemLayout; // Ha a gyökér elem a LinearLayout és annak a hátterét akarod színezni
        CardView cardViewTimeSlot; // Ha CardView a gyökér elem

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            tvSlotStatus = itemView.findViewById(R.id.tvSlotStatus);

            // Próbáljuk meg mindkettőt megtalálni, és azt használni, amelyik létezik
            // (az item_time_slot.xml-ed alapján CardView a gyökér, és benne van egy LinearLayout)
            if (itemView instanceof CardView) {
                cardViewTimeSlot = (CardView) itemView;
            }
            // A LinearLayout-ot az ID-ja alapján keressük meg a CardView-n belül
            itemLayout = itemView.findViewById(R.id.linearLayoutTimeSlot); // Győződj meg róla, hogy ez az ID létezik az item_time_slot.xml-ben

            // Null ellenőrzések
            if (tvTimeSlot == null) Log.e(TAG, "ViewHolder: tvTimeSlot is NULL");
            if (tvSlotStatus == null) Log.e(TAG, "ViewHolder: tvSlotStatus is NULL");
            if (itemLayout == null && cardViewTimeSlot == null) {
                Log.w(TAG, "ViewHolder: Sem itemLayout (LinearLayout), sem cardViewTimeSlot nem található a háttérszínezéshez.");
            } else if (itemLayout == null) {
                Log.d(TAG, "ViewHolder: itemLayout (LinearLayout) nem található, CardView hátterét fogjuk színezni.");
            }
        }
    }
}
