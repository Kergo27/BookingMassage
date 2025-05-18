package com.example.bookingmassage; // Ellenőrizd a csomagnevet!

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingmassage.R; // Az R.color, R.drawable stb. eléréséhez
import com.example.bookingmassage.TimeSlot; // Ellenőrizd a csomagnevet!

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<TimeSlot> timeSlots;
    private OnTimeSlotClickListener listener;
    private Context context; // Szükséges a színek eléréséhez

    // Interface a kattintások kezelésére az Activity-ben
    public interface OnTimeSlotClickListener {
        void onTimeSlotClicked(TimeSlot timeSlot);
    }

    public TimeSlotAdapter(List<TimeSlot> timeSlots, OnTimeSlotClickListener listener) {
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@com.example.bookingmassage.NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext(); // Context lekérése
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        TimeSlot currentTimeSlot = timeSlots.get(position);
        holder.tvTimeSlot.setText(currentTimeSlot.getTime());

        if (currentTimeSlot.isAvailable()) {
            holder.tvSlotStatus.setText("Szabad");
            holder.tvSlotStatus.setTextColor(ContextCompat.getColor(context, R.color.available_slot_text_color)); // Definiáld ezt a színt
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.available_slot_bg_color)); // Definiáld ezt a színt
            holder.cardViewTimeSlot.setCardElevation(4f); // Alapértelmezett elevation
            holder.itemView.setAlpha(1.0f); // Teljesen látható
            holder.itemView.setClickable(true);
        } else {
            holder.tvSlotStatus.setText("Foglalt");
            holder.tvSlotStatus.setTextColor(ContextCompat.getColor(context, R.color.booked_slot_text_color)); // Definiáld ezt a színt
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.booked_slot_bg_color)); // Definiáld ezt a színt
            holder.cardViewTimeSlot.setCardElevation(0f); // Nincs elevation, ha foglalt
            holder.itemView.setAlpha(0.6f); // Kissé áttetsző, ha foglalt
            holder.itemView.setClickable(false); // Ne legyen kattintható, ha foglalt
        }

        holder.itemView.setOnClickListener(v -> {
            if (currentTimeSlot.isAvailable() && listener != null) {
                listener.onTimeSlotClicked(currentTimeSlot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots != null ? timeSlots.size() : 0;
    }

    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeSlot;
        TextView tvSlotStatus;
        LinearLayout itemLayout; // A LinearLayout referenciája a háttérszín állításához
        CardView cardViewTimeSlot;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            tvSlotStatus = itemView.findViewById(R.id.tvSlotStatus);
            itemLayout = itemView.findViewById(R.id.linearLayoutTimeSlot); // ID az item_time_slot.xml-ből
            cardViewTimeSlot = itemView.findViewById(R.id.cardViewTimeSlot);
        }
    }

    // Metódus az adatok frissítésére (opcionális, de hasznos)
    public void updateData(List<TimeSlot> newTimeSlots) {
        this.timeSlots.clear();
        this.timeSlots.addAll(newTimeSlots);
        notifyDataSetChanged();
    }
}
