package com.example.FCC_app;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrainerCalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private RecyclerView recyclerView;
    private SlotAdapter adapter;
    private List<TrainingSlot> daySlots = new ArrayList<>();
    private CalendarDay selectedDay;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer_calendar);

        db = FirebaseFirestore.getInstance();
        selectedDay = CalendarDay.today();

        Toolbar toolbar = findViewById(R.id.calendar_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        calendarView = findViewById(R.id.calendarView);
        recyclerView = findViewById(R.id.slots_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter(daySlots);
        recyclerView.setAdapter(adapter);

        calendarView.setSelectedDate(selectedDay);
        updateSelectedDateText();
        loadSlotsForDate(selectedDay);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDay = date;
            updateSelectedDateText();
            loadSlotsForDate(date);
        });

        findViewById(R.id.fab_add_slot).setOnClickListener(v -> showAddWindowDialog());
    }

    private void updateSelectedDateText() {
        TextView dateTitle = findViewById(R.id.selected_date_text);
        dateTitle.setText(String.format(Locale.GERMANY, "Termine am %02d.%02d.%d", 
                selectedDay.getDay(), selectedDay.getMonth() + 1, selectedDay.getYear()));
    }

    private void loadSlotsForDate(CalendarDay date) {
        String dateKey = String.format(Locale.US, "%d-%02d-%02d", date.getYear(), date.getMonth() + 1, date.getDay());
        
        db.collection("training_slots")
                .whereEqualTo("date", dateKey)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    daySlots.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        daySlots.add(doc.toObject(TrainingSlot.class).withId(doc.getId()));
                    }
                    daySlots.sort((o1, o2) -> {
                        String time1 = (o1.bookedStartTime != null) ? o1.bookedStartTime : o1.startTime;
                        String time2 = (o2.bookedStartTime != null) ? o2.bookedStartTime : o2.startTime;
                        return time1.compareTo(time2);
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddWindowDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_time_window, null);
        TextView startText = view.findViewById(R.id.text_window_start);
        TextView endText = view.findViewById(R.id.text_window_end);

        final int[] startH = {9}, startM = {0}, endH = {13}, endM = {0};

        startText.setOnClickListener(v -> new TimePickerDialog(this, (view1, h, m) -> {
            startH[0] = h; startM[0] = m;
            startText.setText(String.format(Locale.GERMANY, "Start: %02d:%02d Uhr", h, m));
        }, startH[0], startM[0], true).show());

        endText.setOnClickListener(v -> new TimePickerDialog(this, (view1, h, m) -> {
            endH[0] = h; endM[0] = m;
            endText.setText(String.format(Locale.GERMANY, "Ende: %02d:%02d Uhr", h, m));
        }, endH[0], endM[0], true).show());

        new AlertDialog.Builder(this)
                .setTitle("Verfügbarkeit anbieten")
                .setView(view)
                .setPositiveButton("Erstellen", (dialog, which) -> {
                    String startStr = String.format(Locale.GERMANY, "%02d:%02d", startH[0], startM[0]);
                    String endStr = String.format(Locale.GERMANY, "%02d:%02d", endH[0], endM[0]);
                    saveWindowToFirestore(startStr, endStr);
                })
                .setNegativeButton("Abbrechen", null).show();
    }

    private void saveWindowToFirestore(String start, String end) {
        String dateKey = String.format(Locale.US, "%d-%02d-%02d", selectedDay.getYear(), selectedDay.getMonth() + 1, selectedDay.getDay());
        Map<String, Object> slot = new HashMap<>();
        slot.put("date", dateKey);
        slot.put("startTime", start);
        slot.put("endTime", end);
        slot.put("status", "AVAILABLE");
        db.collection("training_slots").add(slot).addOnSuccessListener(d -> loadSlotsForDate(selectedDay));
    }

    private void acceptBooking(TrainingSlot slot) {
        // 1. Check if there's free time BEFORE the booking
        if (slot.bookedStartTime.compareTo(slot.startTime) > 0) {
            saveWindowToFirestore(slot.startTime, slot.bookedStartTime);
        }

        // 2. Check if there's free time AFTER the booking
        if (slot.bookedEndTime.compareTo(slot.endTime) < 0) {
            saveWindowToFirestore(slot.bookedEndTime, slot.endTime);
        }

        // 3. Confirm the current booking and remove original window bounds to prevent re-splitting
        db.collection("training_slots").document(slot.id)
                .update("status", "CONFIRMED", 
                        "startTime", slot.bookedStartTime, 
                        "endTime", slot.bookedEndTime)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Termin bestätigt!", Toast.LENGTH_SHORT).show();
                    loadSlotsForDate(selectedDay);
                });
    }

    private void declineBooking(TrainingSlot slot) {
        // Simply set back to available and clear player data
        db.collection("training_slots").document(slot.id)
                .update("status", "AVAILABLE",
                        "playerTag", null,
                        "bookedStartTime", null,
                        "bookedEndTime", null,
                        "reason", null,
                        "phone", null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Anfrage abgelehnt.", Toast.LENGTH_SHORT).show();
                    loadSlotsForDate(selectedDay);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder> {
        private List<TrainingSlot> slots;
        SlotAdapter(List<TrainingSlot> slots) { this.slots = slots; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trainer_calendar_slot, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TrainingSlot slot = slots.get(position);
            
            if ("AVAILABLE".equals(slot.status)) {
                holder.textTime.setText(slot.startTime + " - " + slot.endTime + " Uhr");
                holder.textStatus.setText("Freies Zeitfenster");
                holder.textStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                holder.layoutActions.setVisibility(View.GONE);
                holder.textDetails.setVisibility(View.GONE);
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            } else if ("PENDING".equals(slot.status)) {
                holder.textTime.setText(slot.bookedStartTime + " - " + slot.bookedEndTime + " Uhr");
                holder.textStatus.setText("ANFRAGE: " + slot.playerTag);
                holder.textStatus.setTextColor(Color.parseColor("#2196F3")); // Blue
                holder.textDetails.setText(slot.type + " (" + slot.duration + ")\nGrund: " + slot.reason);
                holder.textDetails.setVisibility(View.VISIBLE);
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnAccept.setOnClickListener(v -> acceptBooking(slot));
                holder.btnDecline.setOnClickListener(v -> declineBooking(slot));
            } else if ("CONFIRMED".equals(slot.status)) {
                holder.textTime.setText(slot.bookedStartTime + " - " + slot.bookedEndTime + " Uhr");
                holder.textStatus.setText("TERMIN BESTÄTIGT");
                holder.textStatus.setTextColor(Color.RED);
                holder.textDetails.setText("Spieler: " + slot.playerTag + "\nTyp: " + slot.type + "\nTel: " + slot.phone);
                holder.textDetails.setVisibility(View.VISIBLE);
                holder.layoutActions.setVisibility(View.GONE);
                holder.itemView.setBackgroundColor(Color.parseColor("#FFF0F0")); // Very light red
            }

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(TrainerCalendarActivity.this).setTitle("Eintrag löschen?").setPositiveButton("Löschen", (d, w) -> {
                    db.collection("training_slots").document(slot.id).delete().addOnSuccessListener(a -> loadSlotsForDate(selectedDay));
                }).setNegativeButton("Abbrechen", null).show();
                return true;
            });
        }

        @Override public int getItemCount() { return slots.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTime, textStatus, textDetails;
            View layoutActions;
            ImageButton btnAccept, btnDecline;
            ViewHolder(View v) { 
                super(v); 
                textTime = v.findViewById(R.id.slot_time_text);
                textStatus = v.findViewById(R.id.slot_status_text);
                textDetails = v.findViewById(R.id.slot_details_text);
                layoutActions = v.findViewById(R.id.layout_trainer_actions);
                btnAccept = v.findViewById(R.id.btn_accept_slot);
                btnDecline = v.findViewById(R.id.btn_decline_slot);
            }
        }
    }
}