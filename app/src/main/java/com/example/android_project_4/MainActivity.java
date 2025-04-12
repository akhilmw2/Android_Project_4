package com.example.android_project_4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameState gameState;
    private LinearLayout holesLayout;
    private TextView holePlaceholder;

    // Player thread references.
    private PlayerThread playerA;
    private PlayerThread playerB;

    // Track whose turn it is; starting with Player A.
    private String currentPlayerName = "Player A";
    private boolean gameOver = false;

    // Handler that will process messages (shots) from player threads.
    private Handler gameHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageConstants.MSG_SHOT:
                    // Process a shot taken by a player.
                    String playerName = (String) msg.obj;
                    int shotIndex = msg.arg1;
                    Log.d("MainActivity", playerName + " shot at hole " + shotIndex);
                    processShot(playerName, shotIndex);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        holesLayout = findViewById(R.id.holesLayout);
        holePlaceholder = findViewById(R.id.holePlaceholder);

        // Initialize game state (creates 50 holes and selects one winning hole).
        gameState = new GameState();
        gameState.initializeHoles();

        // Update UI to reflect current game board.
        updateUI();

        // Start both player threads; pass them the common gameHandler.
        playerA = new PlayerThread("Player A", gameState, gameHandler);
        playerB = new PlayerThread("Player B", gameState, gameHandler);
        playerA.start();
        playerB.start();

        // Start the game by signaling the first turn.
        sendYourTurn(currentPlayerName);
    }

    // Refresh the on-screen display.
    private void updateUI() {
        StringBuilder displayText = new StringBuilder();
        for (int i = 0; i < gameState.holes.size(); i++) {
            GameState.Hole hole = gameState.holes.get(i);
            displayText.append("Hole ").append(i);
            if (hole.isWinning()) {
                displayText.append(" [Winning]");
            }
            if (!hole.getOccupiedBy().isEmpty()) {
                displayText.append(" [").append(hole.getOccupiedBy()).append("]");
            }
            displayText.append("\n");
        }
        holePlaceholder.setText(displayText.toString());
    }

    // Process a player's shot.
    private void processShot(String playerName, int shotIndex) {
        if (gameOver) return; // Ignore further shots if the game has ended.

        GameState.Hole shotHole = gameState.holes.get(shotIndex);

        String outcome;
        boolean shotValid = shotHole.getOccupiedBy().isEmpty();

        int winningIndex = gameState.winningHoleIndex;
        int winningGroup = winningIndex / 5;
        int shotGroup = shotIndex / 5;

        if (shotIndex == winningIndex) {
            outcome = "JACKPOT";
            gameOver = true;
        } else if (!shotValid && !shotHole.getOccupiedBy().equals(playerName)) {
            outcome = "CATASTROPHE";
            gameOver = true;
        } else if (shotGroup == winningGroup) {
            outcome = "NEAR_MISS";
        } else if (Math.abs(shotGroup - winningGroup) == 1) {
            outcome = "NEAR_GROUP";
        } else {
            outcome = "BIG_MISS";
        }

        // Mark the hole as occupied if it wasn’t already (skip marking in a catastrophe case).
        if (shotValid) {
            shotHole.setOccupiedBy(playerName);
        }

        Log.d("MainActivity", playerName + " outcome: " + outcome);

        // Update the UI to show the shot and current game state.
        updateUI();

        // Send the outcome back to the shooting player's thread.
        Message response = Message.obtain();
        response.what = MessageConstants.MSG_SHOT_RESPONSE;
        response.obj = outcome;
        if (playerName.equals("Player A")) {
            if (playerA.handler != null) {
                playerA.handler.sendMessage(response);
            }
        } else {
            if (playerB.handler != null) {
                playerB.handler.sendMessage(response);
            }
        }

        // If the game is over, notify both players with separate messages.
        if (gameOver) {
            Message gameOverMsgA = Message.obtain();
            gameOverMsgA.what = MessageConstants.MSG_GAME_OVER;
            if (playerA.handler != null)
                playerA.handler.sendMessage(gameOverMsgA);

            Message gameOverMsgB = Message.obtain();
            gameOverMsgB.what = MessageConstants.MSG_GAME_OVER;
            if (playerB.handler != null)
                playerB.handler.sendMessage(gameOverMsgB);

            Log.d("MainActivity", "Game Over! Winner: " +
                    (outcome.equals("JACKPOT") ? playerName : "Opponent wins by catastrophe"));
        } else {
            // Switch turns and signal the other player.
            currentPlayerName = playerName.equals("Player A") ? "Player B" : "Player A";
            sendYourTurn(currentPlayerName);
        }
    }

    // Sends a YOUR_TURN message to the appropriate player thread.
    private void sendYourTurn(String playerName) {
        Message turnMsg = Message.obtain();
        turnMsg.what = MessageConstants.MSG_YOUR_TURN;
        if (playerName.equals("Player A")) {
            if (playerA.handler != null) {
                playerA.handler.sendMessage(turnMsg);
                Log.d("MainActivity", "Sent turn to Player A");
            }
        } else {
            if (playerB.handler != null) {
                playerB.handler.sendMessage(turnMsg);
                Log.d("MainActivity", "Sent turn to Player B");
            }
        }
    }
}