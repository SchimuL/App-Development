package com.example.FCC_app;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class PlayerCalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private RecyclerView recyclerView;
    private PlayerSlotAdapter adapter;
    private List<TrainingSlot> daySlots = new ArrayList<>();
    private HashSet<CalendarDay> availableDays = new HashSet<>();
    private HashSet<CalendarDay> myBookedDays = new HashSet<>();
    private CalendarDay selectedDay;
    private FirebaseFirestore db;
    private String loggedInPlayerTag;

    interface OnSlotClickListener { void onSlotClick(TrainingSlot slot); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Abgabe1_LG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_calendar);

        db = FirebaseFirestore.getInstance();
        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");
        selectedDay = CalendarDay.today();

        Toolbar toolbar = findViewById(R.id.player_calendar_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        calendarView = findViewById(R.id.player_calendarView);
        recyclerView = findViewById(R.id.player_slots_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new PlayerSlotAdapter(daySlots, slot -> {
            if ("AVAILABLE".equals(slot.status)) {
                showBookingDialog(slot);
            }
        });
        recyclerView.setAdapter(adapter);

        calendarView.setSelectedDate(selectedDay);
        updateDateText();
        
        loadCalendarDecorators();
        loadSlotsForDate(selectedDay);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDay = date;
            updateDateText();
            loadSlotsForDate(date);
        });

        calendarView.setOnMonthChangedListener((widget, date) -> loadCalendarDecorators());
    }

    private void updateDateText() {
        TextView dateTitle = findViewById(R.id.player_selected_date_text);
        dateTitle.setText(String.format(Locale.GERMANY, "Termine am %02d.%02d.%d", 
                selectedDay.getDay(), selectedDay.getMonth() + 1, selectedDay.getYear()));
    }

    private void loadCalendarDecorators() {
        db.collection("training_slots").get().addOnSuccessListener(queryDocumentSnapshots -> {
            availableDays.clear();
            myBookedDays.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String dateStr = doc.getString("date");
                String status = doc.getString("status");
                String player = doc.getString("playerTag");
                
                if (dateStr != null) {
                    String[] parts = dateStr.split("-");
                    CalendarDay day = CalendarDay.from(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
                    
                    if ("AVAILABLE".equals(status)) {
                        availableDays.add(day);
                    } else if ("CONFIRMED".equals(status) && loggedInPlayerTag != null && loggedInPlayerTag.equals(player)) {
                        myBookedDays.add(day);
                    }
                }
            }
            calendarView.removeDecorators();
            calendarView.addDecorator(new EventDecorator(Color.GREEN, availableDays));
            calendarView.addDecorator(new EventDecorator(Color.BLUE, myBookedDays));
        });
    }

    private void loadSlotsForDate(CalendarDay date) {
        String dateKey = String.format(Locale.US, "%d-%02d-%02d", date.getYear(), date.getMonth() + 1, date.getDay());
        
        db.collection("training_slots")
                .whereEqualTo("date", dateKey)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    daySlots.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TrainingSlot slot = doc.toObject(TrainingSlot.class).withId(doc.getId());
                        if ("AVAILABLE".equals(slot.status) || (loggedInPlayerTag != null && loggedInPlayerTag.equals(slot.playerTag))) {
                            daySlots.add(slot);
                        }
                    }
                    daySlots.sort((o1, o2) -> {
                        String t1 = (o1.bookedStartTime != null) ? o1.bookedStartTime : o1.startTime;
                        String t2 = (o2.bookedStartTime != null) ? o2.bookedStartTime : o2.startTime;
                        // Handle potential nulls
                        if (t1 == null) t1 = ""; if (t2 == null) t2 = "";
                        return t1.compareTo(t2);
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    private void showBookingDialog(TrainingSlot slot) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_book_slot_full, null);
        TextView windowInfo = view.findViewById(R.id.text_window_info_player);
        TextView startTimeText = view.findViewById(R.id.text_book_start_time);
        Spinner typeSpinner = view.findViewById(R.id.spinner_book_type);
        Spinner durationSpinner = view.findViewById(R.id.spinner_book_duration);
        EditText editPhone = view.findViewById(R.id.edit_book_phone);
        EditText editReason = view.findViewById(R.id.edit_book_reason);

        windowInfo.setText(String.format("Verfügbar: %s - %s Uhr", slot.startTime, slot.endTime));
        windowInfo.setTextColor(Color.WHITE);

        final int[] selH = {-1}, selM = {-1};
        startTimeText.setOnClickListener(v -> {
            String[] parts = (slot.startTime != null ? slot.startTime : "09:00").split(":");
            new TimePickerDialog(this, (tp, h, m) -> {
                selH[0] = h; selM[0] = m;
                startTimeText.setText(String.format(Locale.GERMANY, "%02d:%02d Uhr", h, m));
            }, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), true).show();
        });

        new AlertDialog.Builder(this, R.style.App_Theme_Dialog)
                .setTitle("Termin anfragen")
                .setView(view)
                .setPositiveButton("Senden", (dialog, which) -> {
                    if (selH[0] == -1) return;
                    String startTime = String.format(Locale.GERMANY, "%02d:%02d", selH[0], selM[0]);
                    int dur = durationSpinner.getSelectedItem().toString().contains("90") ? 90 : 60;
                    int endM = selM[0] + dur;
                    String endTime = String.format(Locale.GERMANY, "%02d:%02d", selH[0] + (endM/60), endM % 60);
                    sendRequest(slot, startTime, endTime, typeSpinner.getSelectedItem().toString(), 
                                durationSpinner.getSelectedItem().toString(), editPhone.getText().toString(), editReason.getText().toString());
                })
                .setNegativeButton("Abbrechen", null).show();
    }

    private void sendRequest(TrainingSlot slot, String start, String end, String type, String dur, String phone, String reason) {
        db.collection("training_slots").document(slot.id)
                .update("status", "PENDING", "bookedStartTime", start, "bookedEndTime", end,
                        "type", type, "duration", dur, "playerTag", loggedInPlayerTag, "phone", phone, "reason", reason)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Anfrage gesendet!", Toast.LENGTH_SHORT).show();
                    loadSlotsForDate(selectedDay);
                    loadCalendarDecorators();
                });
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;
        public EventDecorator(int color, HashSet<CalendarDay> dates) { this.color = color; this.dates = dates; }
        @Override public boolean shouldDecorate(CalendarDay day) { return dates.contains(day); }
        @Override public void decorate(DayViewFacade view) { view.addSpan(new DotSpan(8, color)); }
    }

    private class PlayerSlotAdapter extends RecyclerView.Adapter<PlayerSlotAdapter.ViewHolder> {
        private final List<TrainingSlot> slots;
        private final OnSlotClickListener listener;
        PlayerSlotAdapter(List<TrainingSlot> slots, OnSlotClickListener listener) { this.slots = slots; this.listener = listener; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_calendar_slot, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TrainingSlot slot = slots.get(position);
            if ("AVAILABLE".equals(slot.status)) {
                holder.time.setText("FREI: " + slot.startTime + " - " + slot.endTime + " Uhr");
                holder.status.setText("Klicke zum Buchen");
                holder.background.setBackgroundColor(Color.parseColor("#1B5E20")); // Deep Green
            } else {
                holder.time.setText("DEIN TERMIN: " + slot.bookedStartTime + " - " + slot.bookedEndTime + " Uhr");
                holder.status.setText("Status: " + slot.status + " (" + slot.type + ")");
                holder.background.setBackgroundColor(Color.parseColor("#0D47A1")); // Deep Blue
            }
            holder.itemView.setOnClickListener(v -> listener.onSlotClick(slot));
        }

        @Override public int getItemCount() { return slots.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView time, status;
            View background;
            ViewHolder(View v) { 
                super(v); 
                time = v.findViewById(R.id.text_slot_time); 
                status = v.findViewById(R.id.text_slot_status);
                background = v.findViewById(R.id.slot_background);
            }
        }
    }
}