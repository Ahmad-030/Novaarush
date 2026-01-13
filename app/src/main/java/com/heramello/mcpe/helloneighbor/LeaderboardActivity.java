package com.heramello.mcpe.helloneighbor;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class LeaderboardActivity extends AppCompatActivity {

    private TextView tvLeaderboard;
    private Button btnBack, btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        tvLeaderboard = findViewById(R.id.tvLeaderboard);
        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);

        loadScores();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearConfirmation();
            }
        });
    }

    private void showClearConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Leaderboard")
                .setMessage("Are you sure you want to clear all scores?")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearScores();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearScores() {
        SharedPreferences prefs = getSharedPreferences("NovaRushScores", MODE_PRIVATE);
        prefs.edit().clear().apply();
        loadScores();
    }

    private void loadScores() {
        SharedPreferences prefs = getSharedPreferences("NovaRushScores", MODE_PRIVATE);
        String scoresString = prefs.getString("scores", "");

        if (scoresString.isEmpty()) {
            tvLeaderboard.setText("No scores yet!\nStart playing to set records!");
            return;
        }

        String[] scoreArray = scoresString.split(";");
        ArrayList<GameScore> scores = new ArrayList<>();
        for (String s : scoreArray) {
            String[] parts = s.split(",");
            if (parts.length == 3) {
                scores.add(new GameScore(
                        Integer.parseInt(parts[0]),
                        Long.parseLong(parts[1]),
                        Integer.parseInt(parts[2])
                ));
            }
        }

        StringBuilder display = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            GameScore gs = scores.get(i);
            String medal = "";
            if (i == 0) medal = "🥇 ";
            else if (i == 1) medal = "🥈 ";
            else if (i == 2) medal = "🥉 ";
            else medal = (i + 1) + ". ";

            display.append(medal)
                    .append("Score: ").append(gs.score)
                    .append(" | ").append(gs.time).append("s")
                    .append(" | ⚠").append(gs.nearMisses)
                    .append("\n\n");
        }

        tvLeaderboard.setText(display.toString());
    }

    class GameScore {
        int score;
        long time;
        int nearMisses;

        GameScore(int score, long time, int nearMisses) {
            this.score = score;
            this.time = time;
            this.nearMisses = nearMisses;
        }
    }
}