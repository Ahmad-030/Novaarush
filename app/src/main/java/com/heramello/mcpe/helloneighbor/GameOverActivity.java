package com.heramello.mcpe.helloneighbor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;

public class GameOverActivity extends AppCompatActivity {

    private TextView tvGameOver, tvStats, tvNewRecord;
    private Button btnPlayAgain, btnMenu;
    private long survivalTime;
    private int nearMisses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        tvGameOver = findViewById(R.id.tvGameOver);
        tvStats = findViewById(R.id.tvStats);
        tvNewRecord = findViewById(R.id.tvNewRecord);
        btnPlayAgain = findViewById(R.id.btnPlayAgain);
        btnMenu = findViewById(R.id.btnMenu);

        survivalTime = getIntent().getLongExtra("SURVIVAL_TIME", 0);
        nearMisses = getIntent().getIntExtra("NEAR_MISSES", 0);

        int finalScore = calculateScore(survivalTime, nearMisses);

        tvStats.setText("Survival Time: " + survivalTime + "s\n" +
                "Near Misses: " + nearMisses + "\n" +
                "Final Score: " + finalScore);

        boolean isNewRecord = saveHighScore(finalScore, survivalTime, nearMisses);
        if (isNewRecord) {
            tvNewRecord.setVisibility(View.VISIBLE);
        }

        btnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GameOverActivity.this, GameActivity.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GameOverActivity.this, MenuActivity.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private int calculateScore(long time, int misses) {
        return (int) (time * 10 + misses * 5);
    }

    private boolean saveHighScore(int score, long time, int misses) {
        SharedPreferences prefs = getSharedPreferences("NovaRushScores", MODE_PRIVATE);
        String scoresString = prefs.getString("scores", "");

        ArrayList<GameScore> scores = new ArrayList<>();
        if (!scoresString.isEmpty()) {
            String[] scoreArray = scoresString.split(";");
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
        }

        boolean isNewRecord = scores.isEmpty() || score > scores.get(0).score;

        scores.add(new GameScore(score, time, misses));
        Collections.sort(scores, (a, b) -> Integer.compare(b.score, a.score));

        if (scores.size() > 10) {
            scores = new ArrayList<>(scores.subList(0, 10));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            GameScore gs = scores.get(i);
            sb.append(gs.score).append(",")
                    .append(gs.time).append(",")
                    .append(gs.nearMisses);
            if (i < scores.size() - 1) sb.append(";");
        }

        prefs.edit().putString("scores", sb.toString()).apply();
        return isNewRecord;
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