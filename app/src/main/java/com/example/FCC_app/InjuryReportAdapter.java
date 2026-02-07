package com.example.FCC_app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InjuryReportAdapter extends RecyclerView.Adapter<InjuryReportAdapter.InjuryViewHolder> {

    private final List<InjuryEntry> injuryList;
    private final OnItemClickListener listener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(InjuryEntry item);
    }

    public InjuryReportAdapter(List<InjuryEntry> injuryList, OnItemClickListener listener) {
        this.injuryList = injuryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InjuryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_injury, parent, false);
        return new InjuryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InjuryViewHolder holder, int position) {
        InjuryEntry currentEntry = injuryList.get(position);
        holder.bind(currentEntry, listener);

        holder.playerNameText.setText("Spieler: " + currentEntry.playerName);
        holder.bodyPartText.setText("Körperteil: " + currentEntry.bodyPart);
        holder.dateText.setText("Datum: " + currentEntry.date);
        holder.painLevelView.setText(String.valueOf(currentEntry.painLevel));

        // Set background color of pain level circle based on pain level
        // Make sure the background is a shape drawable we can modify
        GradientDrawable background = (GradientDrawable) holder.painLevelView.getBackground().mutate();
        if (currentEntry.painLevel >= 7) {
            background.setColor(Color.RED);
        } else if (currentEntry.painLevel >= 4) {
            background.setColor(Color.parseColor("#FFA500")); // Orange
        } else {
            background.setColor(Color.parseColor("#4CAF50")); // Green
        }
    }

    @Override
    public int getItemCount() {
        return injuryList.size();
    }

    static class InjuryViewHolder extends RecyclerView.ViewHolder {
        final TextView playerNameText;
        final TextView bodyPartText;
        final TextView dateText;
        final TextView painLevelView;

        InjuryViewHolder(@NonNull View itemView) {
            super(itemView);
            playerNameText = itemView.findViewById(R.id.player_name_text);
            bodyPartText = itemView.findViewById(R.id.body_part_text);
            dateText = itemView.findViewById(R.id.date_text);
            painLevelView = itemView.findViewById(R.id.pain_level_view);
        }

        public void bind(final InjuryEntry item, final OnItemClickListener listener) {
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
