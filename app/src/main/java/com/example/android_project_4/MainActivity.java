package com.example.android_project_4;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameState gameState;
    private LinearLayout holesLayout;
    private TextView summaryTextView; // For game-over summary

    // References to the two player threads.
    private PlayerThread playerA;
    private PlayerThread playerB;

    private String currentPlayerName = "Player A";
    private boolean gameOver = false;

    // Handler that receives shot messages from player threads.
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
        // Use a layout with a vertical LinearLayout containing the holes list and a summary TextView.
        setContentView(R.layout.activity_main);

        holesLayout = findViewById(R.id.holesLayout);
        summaryTextView = findViewById(R.id.summaryTextView);

        // Initialize game state (50 holes, one winning hole).
        gameState = new GameState();
        gameState.initializeHoles();

        // Show initial holes UI.
        updateUI();

        // Start both player threads.
        playerA = new PlayerThread("Player A", gameState, gameHandler);
        playerB = new PlayerThread("Player B", gameState, gameHandler);
        playerA.start();
        playerB.start();

        // Signal the first turn.
        sendYourTurn(currentPlayerName);
    }

    // Updated updateUI(): display a horizontal container for each hole that has a circle (ImageView) and info (TextView).
    private void updateUI() {
        holesLayout.removeAllViews();
        int totalHoles = gameState.holes.size();
        for (int i = 0; i < totalHoles; i++) {
            GameState.Hole hole = gameState.holes.get(i);
            // Create a horizontal LinearLayout for each hole.
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.HORIZONTAL);

            // Create an ImageView for the hole circle.
            ImageView holeView = new ImageView(this);
            LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(150, 150);
            circleParams.setMargins(8, 8, 8, 8);
            holeView.setLayoutParams(circleParams);

            // Set background based on hole state.
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
            container.addView(holeView);

            // Create a TextView for shot information.
            TextView infoText = new TextView(this);
            infoText.setTextSize(16f);
            String info;
            if (!hole.getOccupiedBy().isEmpty()) {
                info = " " + hole.getOccupiedBy() + " (" + hole.getOutcome() + ")";
            } else if (hole.isWinning()) {
                info = " Winning Hole";
            } else {
                info = " Empty";
            }
            infoText.setText(info);
            container.addView(infoText);

            holesLayout.addView(container);
        }
    }

    // Process a player's shot.
    private void processShot(String playerName, int shotIndex) {
        if (gameOver) return;

        // Determine shot outcome
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
            shotHole.setOutcome(outcome);
        }
        Log.d("MainActivity", playerName + " outcome: " + outcome);

        // Force the UI update on the main thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI();
                holesLayout.requestLayout();
            }
        });

        // Send the outcome back to the shooting player's thread.
        Message response = Message.obtain();
        response.what = MessageConstants.MSG_SHOT_RESPONSE;
        response.obj = outcome;
        if (playerName.equals("Player A")) {
            if (playerA.handler != null)
                playerA.handler.sendMessage(response);
        } else {
            if (playerB.handler != null)
                playerB.handler.sendMessage(response);
        }

        // Game-over handling: show final summary and popup if needed.
        if (gameOver) {
            Message gameOverMsgA = Message.obtain();
            gameOverMsgA.what = MessageConstants.MSG_GAME_OVER;
            if (playerA.handler != null)
                playerA.handler.sendMessage(gameOverMsgA);

            Message gameOverMsgB = Message.obtain();
            gameOverMsgB.what = MessageConstants.MSG_GAME_OVER;
            if (playerB.handler != null)
                playerB.handler.sendMessage(gameOverMsgB);

            String finalMessage;
            if (outcome.equals("JACKPOT")) {
                finalMessage = "Game Over: " + playerName + " won by hitting the winning hole!";
            } else if (outcome.equals("CATASTROPHE")) {
                finalMessage = "Game Over: " + playerName + " hit an occupied hole. Opponent wins!";
            } else {
                finalMessage = "Game Over.";
            }
            Log.d("MainActivity", finalMessage);
            summaryTextView.setText(finalMessage);
            showGameResult(finalMessage);
        } else {
            // Instead of calling sendYourTurn() immediately, use postDelayed on the main thread.
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendYourTurn(playerName.equals("Player A") ? "Player B" : "Player A");
                }
            }, 2000); // 2-second delay before next turn (adjust if needed)
        }
    }



    // Sends a YOUR_TURN message to the designated player.
    private void sendYourTurn(String playerName) {
        Message turnMsg = Message.obtain();
        turnMsg.what = MessageConstants.MSG_YOUR_TURN;
        if (playerName.equals("Player A")) {
            if (playerA.handler != null) {
                playerA.handler.sendMessage(turnMsg);
                Log.d("MainActivity", "Turn: Player A");
            }
        } else {
            if (playerB.handler != null) {
                playerB.handler.sendMessage(turnMsg);
                Log.d("MainActivity", "Turn: Player B");
            }
        }
    }

    // Displays an AlertDialog with the game result.
    private void showGameResult(String resultMessage) {
        // Create an AlertDialog on the main thread.
        new AlertDialog.Builder(this)
                .setTitle("Game Result")
                .setMessage(resultMessage)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // Optionally, you could finish() the activity or restart the game.
                    }
                })
                .show();
    }
}