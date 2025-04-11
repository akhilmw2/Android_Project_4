package com.example.android_project_4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private GameState gameState;
    private LinearLayout holesLayout;
    private TextView holePlaceholder;

    // Player threads
    private PlayerThread playerA;
    private PlayerThread playerB;

    // UI handler for possible future updates
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        holesLayout = findViewById(R.id.holesLayout);
        holePlaceholder = findViewById(R.id.holePlaceholder);

        // Initialize game state (create 50 holes and pick a winning hole)
        gameState = new GameState();
        gameState.initializeHoles();

        // Update the UI to display the list of holes
        updateUI();

        // Initialize and start the two player threads
        playerA = new PlayerThread("Player A", gameState);
        playerB = new PlayerThread("Player B", gameState);
        playerA.start();
        playerB.start();

        // Future: you can send initial messages to player threads via their handlers
    }

    // Method to update the UI with the list of holes and highlight the winning hole.
    private void updateUI() {
        StringBuilder displayText = new StringBuilder();
        for (int i = 0; i < gameState.holes.size(); i++) {
            GameState.Hole hole = gameState.holes.get(i);
            displayText.append("Hole ").append(i);
            if (hole.isWinning()) {
                displayText.append(" (Winning Hole)");
            }
            displayText.append("\n");
        }
        holePlaceholder.setText(displayText.toString());
    }
}