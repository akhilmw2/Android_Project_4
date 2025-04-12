package com.example.android_project_4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameState gameState;
    private LinearLayout holesLayout;

    // References to the two player threads.
    private PlayerThread playerA;
    private PlayerThread playerB;

    private String currentPlayerName = "Player A";
    private boolean gameOver = false;

    // This handler receives shots from the player threads.
    private Handler gameHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageConstants.MSG_SHOT:
                    // msg.obj holds the player's name; arg1 holds the shot index.
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

        // Initialize game state (50 holes, one winning hole).
        gameState = new GameState();
        gameState.initializeHoles();

        // Show initial holes UI.
        updateUI();

        // Start both player threads, passing the common gameHandler.
        playerA = new PlayerThread("Player A", gameState, gameHandler);
        playerB = new PlayerThread("Player B", gameState, gameHandler);
        playerA.start();
        playerB.start();

        // Signal the first turn.
        sendYourTurn(currentPlayerName);
    }

    // Update the UI: clear the layout and add an ImageView (circle) for each hole.
    private void updateUI() {
        holesLayout.removeAllViews();
        int totalHoles = gameState.holes.size();
        for (int i = 0; i < totalHoles; i++) {
            GameState.Hole hole = gameState.holes.get(i);
            ImageView holeView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
            params.setMargins(8, 8, 8, 8);
            holeView.setLayoutParams(params);

            // Change appearance based on hole state.
            if (hole.isWinning()) {
                holeView.setBackgroundResource(R.drawable.winning_hole);
            } else if (!hole.getOccupiedBy().isEmpty()) {
                if (hole.getOccupiedBy().equals("Player A")) {
                    holeView.setBackgroundResource(R.drawable.player_a_hole);
                } else if (hole.getOccupiedBy().equals("Player B")) {
                    holeView.setBackgroundResource(R.drawable.player_b_hole);
                }
            } else {
                holeView.setBackgroundResource(R.drawable.default_hole);
            }
            holesLayout.addView(holeView);
        }
    }

    // Processes a player's shot.
    private void processShot(String playerName, int shotIndex) {
        if (gameOver) return;

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

        if (shotValid) {
            shotHole.setOccupiedBy(playerName);
        }
        Log.d("MainActivity", playerName + " outcome: " + outcome);

        updateUI();

        // Send shot outcome back to the player's thread.
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

        // Notify both players if game over.
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
            // Switch turns.
            currentPlayerName = playerName.equals("Player A") ? "Player B" : "Player A";
            sendYourTurn(currentPlayerName);
        }
    }

    // Sends a YOUR_TURN message to the designated player.
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