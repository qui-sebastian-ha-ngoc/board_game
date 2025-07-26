package com.example.iuboardgamejava;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import android.content.SharedPreferences;
import android.widget.ImageButton;
import android.app.AlertDialog;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private MeetingAdapter adapter;
    private List<Meeting> meetings;
    public static final String PREFS_NAME = "MeetingsPrefs";
    private static final String KEY_LAST_MEETING = "lastMeeting";
    private static final String KEY_HOST_INDEX = "hostIndex";
    private List<Player> players;
    private int hostIndex = 0;
    private static final String KEY_HOSTS = "hosts";
    public static final String KEY_VOTES_PREFIX = "votes_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Spieler-Liste (feste Reihenfolge)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Gastgeber-Liste laden
        players = new ArrayList<>();
        String hostsString = prefs.getString(KEY_HOSTS, null);
        if (hostsString != null && !hostsString.isEmpty()) {
            String[] hostArr = hostsString.split(",");
            for (String h : hostArr) {
                if (!h.trim().isEmpty()) players.add(new Player(h.trim()));
            }
        }
        // Host-Index laden
        hostIndex = prefs.getInt(KEY_HOST_INDEX, 0);
        // Meetings laden
        meetings = new ArrayList<>();
        String lastMeeting = prefs.getString(KEY_LAST_MEETING, null);
        if (lastMeeting != null) {
            String[] parts = lastMeeting.split(";;");
            if (parts.length >= 6) {
                Meeting m = new Meeting(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
                loadMeetingVotes(m);
                meetings.add(m);
            }
        }
        // Dummy für Start
        if (meetings.isEmpty()) {
            meetings.add(new Meeting("Spieleabend im Café", "12.06.2024, 19:00", "Café Spielwiese", "Wir spielen Klassiker wie Catan und Carcassonne. Jeder ist willkommen!", players.get(hostIndex).name, ""));
        }
        if (players.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Keine Gastgeber vorhanden")
                .setMessage("Bitte füge über das + unten rechts mindestens einen Gastgeber hinzu, damit Spieleabende geplant werden können.")
                .setPositiveButton("OK", null)
                .show();
        }
        sortMeetingsByDateTime();
        RecyclerView recyclerView = findViewById(R.id.recyclerViewMeetings);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new MeetingAdapter(meetings, this);
        recyclerView.setAdapter(adapter);
        FloatingActionButton fab = findViewById(R.id.fabAddMeeting);
        fab.setOnClickListener(v -> showAddMeetingDialog());
        FloatingActionButton fabAddHost = findViewById(R.id.fabAddHost);
        fabAddHost.setOnClickListener(v -> showHostManagementDialog());
    }

    private void sortMeetingsByDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMAN);
        Collections.sort(meetings, new Comparator<Meeting>() {
            @Override
            public int compare(Meeting m1, Meeting m2) {
                try {
                    return sdf.parse(m1.dateTime).compareTo(sdf.parse(m2.dateTime));
                } catch (ParseException e) {
                    return 0;
                }
            }
        });
    }

    private void showAddMeetingDialog() {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        android.widget.EditText inputTitle = new android.widget.EditText(this);
        inputTitle.setHint("Titel");
        android.widget.EditText inputDateTime = new android.widget.EditText(this);
        inputDateTime.setHint("Datum & Uhrzeit");
        android.widget.EditText inputLocation = new android.widget.EditText(this);
        inputLocation.setHint("Ort");
        android.widget.EditText inputDescription = new android.widget.EditText(this);
        inputDescription.setHint("Beschreibung");
        android.widget.EditText inputGames = new android.widget.EditText(this);
        inputGames.setHint("Spiele-Vorschläge (Komma getrennt)");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(inputTitle);
        layout.addView(inputDateTime);
        layout.addView(inputLocation);
        layout.addView(inputDescription);
        layout.addView(inputGames);
        String nextHost = players.get((hostIndex + 1) % players.size()).name;
        new android.app.AlertDialog.Builder(this)
            .setTitle("Neuen Spieleabend planen (Gastgeber: " + nextHost + ")")
            .setView(layout)
            .setPositiveButton("Speichern", (dialog, which) -> {
                String title = inputTitle.getText().toString();
                String dateTime = inputDateTime.getText().toString();
                String location = inputLocation.getText().toString();
                String description = inputDescription.getText().toString();
                String games = inputGames.getText().toString();
                if (!title.isEmpty() && !dateTime.isEmpty() && !location.isEmpty()) {
                    hostIndex = (hostIndex + 1) % players.size();
                    Meeting newMeeting = new Meeting(title, dateTime, location, description, players.get(hostIndex).name, games);
                    meetings.add(newMeeting);
                    sortMeetingsByDateTime();
                    adapter.notifyDataSetChanged();
                    // Speichern
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(KEY_LAST_MEETING, title + ";;" + dateTime + ";;" + location + ";;" + description + ";;" + players.get(hostIndex).name + ";;" + games);
                    editor.putInt(KEY_HOST_INDEX, hostIndex);
                    editor.apply();
                }
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }

    private void showHostManagementDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog_Alert);
        builder.setTitle("Gastgeber verwalten");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);
        // Gastgeber-Liste anzeigen
        android.widget.TextView tvList = new android.widget.TextView(this);
        updateHostListText(tvList);
        layout.addView(tvList);
        // Hinzufügen
        android.widget.LinearLayout inputRow = new android.widget.LinearLayout(this);
        inputRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.EditText inputName = new android.widget.EditText(this);
        inputName.setHint("Neuer Gastgeber");
        inputRow.addView(inputName, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        android.widget.Button btnAdd = new android.widget.Button(this);
        btnAdd.setText("Hinzufügen");
        inputRow.addView(btnAdd);
        layout.addView(inputRow);
        // Löschen
        android.widget.Spinner spinner = new android.widget.Spinner(this);
        updateHostSpinner(spinner);
        android.widget.Button btnDelete = new android.widget.Button(this);
        btnDelete.setText("Ausgewählten Gastgeber löschen");
        layout.addView(spinner);
        layout.addView(btnDelete);
        btnAdd.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            if (!name.isEmpty()) {
                players.add(new Player(name));
                saveHosts();
                updateHostListText(tvList);
                updateHostSpinner(spinner);
                inputName.setText("");
                android.widget.Toast.makeText(this, "Gastgeber hinzugefügt: " + name, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        btnDelete.setOnClickListener(v -> {
            int idx = spinner.getSelectedItemPosition();
            if (idx >= 0 && idx < players.size()) {
                String removed = players.remove(idx).name;
                saveHosts();
                updateHostListText(tvList);
                updateHostSpinner(spinner);
                android.widget.Toast.makeText(this, "Gastgeber gelöscht: " + removed, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        builder.setView(layout);
        builder.setPositiveButton("Schließen", null);
        builder.show();
    }
    private void updateHostListText(android.widget.TextView tv) {
        StringBuilder sb = new StringBuilder();
        sb.append("Aktuelle Gastgeber:\n");
        for (Player p : players) sb.append("- ").append(p.name).append("\n");
        tv.setText(sb.toString());
    }
    private void updateHostSpinner(android.widget.Spinner spinner) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Player p : players) names.add(p.name);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadMeetingVotes(Meeting meeting) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String votesString = prefs.getString(KEY_VOTES_PREFIX + meeting.title, null);
        if (votesString != null) {
            for (String entry : votesString.split(",")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    meeting.allGames.add(parts[0]);
                    try {
                        meeting.votes.put(parts[0], Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        meeting.votes.put(parts[0], 0);
                    }
                }
            }
        }
    }

    private void saveHosts() {
        StringBuilder sb = new StringBuilder();
        for (Player p : players) {
            if (sb.length() > 0) sb.append(",");
            sb.append(p.name);
        }
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_HOSTS, sb.toString());
        editor.apply();
    }
}

// Modellklasse für ein Spieletreffen
class Meeting {
    String title;
    String dateTime;
    String location;
    String description;
    String host;
    String games; // Komma-getrennte Spiele-Vorschläge
    Map<String, Integer> votes = new HashMap<>(); // Spielname -> Upvotes
    Set<String> allGames = new HashSet<>(); // Alle vorgeschlagenen Spiele
    Meeting(String title, String dateTime, String location, String description, String host, String games) {
        this.title = title;
        this.dateTime = dateTime;
        this.location = location;
        this.description = description;
        this.host = host;
        this.games = games;
        if (games != null && !games.isEmpty()) {
            for (String g : games.split(",")) {
                String trimmed = g.trim();
                if (!trimmed.isEmpty()) {
                    allGames.add(trimmed);
                    votes.put(trimmed, 0);
                }
            }
        }
    }
    // Hilfsmethode für Top 3 Spiele
    List<String> getTop3Games() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(votes.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<String> top = new ArrayList<>();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            top.add(list.get(i).getKey());
        }
        return top;
    }
}
// Adapter für die RecyclerView
class MeetingAdapter extends RecyclerView.Adapter<MeetingViewHolder> {
    private final List<Meeting> meetings;
    private final android.content.Context context;
    MeetingAdapter(List<Meeting> meetings, android.content.Context context) {
        this.meetings = meetings;
        this.context = context;
    }
    @Override
    public MeetingViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meeting, parent, false);
        return new MeetingViewHolder(view);
    }
    @Override
    public void onBindViewHolder(MeetingViewHolder holder, int position) {
        Meeting meeting = meetings.get(position);
        holder.bind(meeting);
        holder.itemView.setOnClickListener(v -> {
            showMeetingDetailDialog(meeting);
        });
    }
    @Override
    public int getItemCount() {
        return meetings.size();
    }

    private void showMeetingDetailDialog(Meeting meeting) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog_Alert);
        builder.setTitle(meeting.title + " (" + meeting.dateTime + ")");
        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);
        layout.setBackgroundResource(android.R.color.background_light);
        layout.setClipToOutline(true);
        // Spiele-Liste anzeigen
        android.widget.TextView tvHostGames = new android.widget.TextView(context);
        tvHostGames.setText("Vorgeschlagene Spiele vom Gastgeber: " + meeting.games);
        tvHostGames.setTextSize(18);
        tvHostGames.setPadding(0, 0, 0, 16);
        layout.addView(tvHostGames);
        // Eigenen Vorschlag
        android.widget.LinearLayout inputRow = new android.widget.LinearLayout(context);
        inputRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.EditText inputGame = new android.widget.EditText(context);
        inputGame.setHint("Eigenes Spiel vorschlagen");
        inputGame.setPadding(0, 16, 0, 16);
        inputRow.addView(inputGame, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        android.widget.Button btnAddGame = new android.widget.Button(context);
        btnAddGame.setText("Hinzufügen");
        inputRow.addView(btnAddGame);
        layout.addView(inputRow);
        // Spiele-Liste mit CardView-Optik
        android.widget.LinearLayout gamesList = new android.widget.LinearLayout(context);
        gamesList.setOrientation(android.widget.LinearLayout.VERTICAL);
        updateGamesListUI(meeting, gamesList);
        layout.addView(gamesList);
        btnAddGame.setOnClickListener(v -> {
            String newGame = inputGame.getText().toString().trim();
            if (!newGame.isEmpty() && !meeting.allGames.contains(newGame)) {
                meeting.allGames.add(newGame);
                meeting.votes.put(newGame, 1); // Erster Upvote für eigenen Vorschlag
                saveMeetingVotes(meeting);
                updateGamesListUI(meeting, gamesList);
                inputGame.setText("");
            }
        });
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setNegativeButton("Schließen", null);
        builder.show();
    }

    private void updateGamesListUI(Meeting meeting, android.widget.LinearLayout gamesList) {
        gamesList.removeAllViews();
        for (String game : meeting.allGames) {
            androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(context);
            android.widget.LinearLayout row = new android.widget.LinearLayout(context);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setPadding(16, 8, 16, 8);
            android.widget.TextView gameName = new android.widget.TextView(context);
            gameName.setText(game);
            gameName.setTextSize(16);
            gameName.setPadding(0, 0, 16, 0);
            row.addView(gameName, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            android.widget.TextView voteCount = new android.widget.TextView(context);
            voteCount.setText(String.valueOf(meeting.votes.get(game)));
            voteCount.setTextSize(16);
            voteCount.setPadding(8, 0, 8, 0);
            row.addView(voteCount);
            android.widget.ImageButton upvoteBtn = new android.widget.ImageButton(context);
            upvoteBtn.setImageResource(android.R.drawable.ic_input_add);
            upvoteBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            upvoteBtn.setOnClickListener(v -> {
                meeting.votes.put(game, meeting.votes.get(game) + 1);
                voteCount.setText(String.valueOf(meeting.votes.get(game)));
                saveMeetingVotes(meeting);
            });
            row.addView(upvoteBtn);
            card.addView(row);
            android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 8, 0, 8);
            gamesList.addView(card, cardParams);
        }
    }
    private void updateVotesText(Meeting meeting, android.widget.TextView tvVotes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Upvotes:\n");
        for (String g : meeting.allGames) {
            sb.append(g).append(": ").append(meeting.votes.get(g)).append("\n");
        }
        tvVotes.setText(sb.toString());
    }
    private void saveMeetingVotes(Meeting meeting) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String g : meeting.allGames) {
            if (sb.length() > 0) sb.append(",");
            sb.append(g).append(":").append(meeting.votes.get(g));
        }
        prefs.edit().putString(MainActivity.KEY_VOTES_PREFIX + meeting.title, sb.toString()).apply();
    }
}
// ViewHolder für die RecyclerView
class MeetingViewHolder extends RecyclerView.ViewHolder {
    private final android.widget.TextView tvTitle;
    private final android.widget.TextView tvDateTime;
    private final android.widget.TextView tvLocation;
    public MeetingViewHolder(android.view.View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvDateTime = itemView.findViewById(R.id.tvDateTime);
        tvLocation = itemView.findViewById(R.id.tvLocation);
    }
    public void bind(Meeting meeting) {
        String top3 = "";
        List<String> topGames = meeting.getTop3Games();
        if (!topGames.isEmpty()) {
            top3 = "\nTop 3: " + String.join(", ", topGames);
        }
        tvTitle.setText(meeting.title + " (Gastgeber: " + meeting.host + ")" + top3);
        tvDateTime.setText(meeting.dateTime);
        tvLocation.setText(meeting.location);
    }
}

class Player {
    String name;
    Player(String name) { this.name = name; }
}