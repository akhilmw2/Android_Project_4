package com.example.android_project_4;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlayerThread extends Thread {
    private String playerName;
    private GameState gameState;
    public Handler handler;           // Receives YOUR_TURN, SHOT_RESPONSE, GAME_OVER.
    private Handler uiHandler;         // Reference to MainActivity's handler.
    private Set<Integer> attemptedShots;
    private Random random;

    // Strategy variables for Player A.
    private Integer lastShotIndex = null;
    private String lastOutcome = null;

    public PlayerThread(String name, GameState state, Handler uiHandler) {
        this.playerName = name;
        this.gameState = state;
        this.uiHandler = uiHandler;
        this.attemptedShots = new HashSet<>();
        this.random = new Random();
    }

    @Override
    public void run() {
        Looper.prepare();

        handler = new Handler(Looper.myLooper(), msg -> {
            switch (msg.what) {
                case MessageConstants.MSG_YOUR_TURN:
                    Log.d("PlayerThread", playerName + " received YOUR_TURN");
                    takeShot();
                    break;
                case MessageConstants.MSG_SHOT_RESPONSE:
                    String outcome = (String) msg.obj;
                    lastOutcome = outcome;  // Store outcome to influence strategy.
                    Log.d("PlayerThread", playerName + " received outcome: " + outcome);
                    break;
                case MessageConstants.MSG_GAME_OVER:
                    Log.d("PlayerThread", playerName + " received GAME_OVER. Terminating thread.");
                    Looper.myLooper().quit();
                    break;
                default:
                    break;
            }
            return true;
        });

        Log.d("PlayerThread", playerName + " is ready for messages.");
        Looper.loop();
    }

    // Determines which hole to shoot at based on strategy.
    private int chooseShotIndex() {
        // For Player A, if the last outcome was a near miss, try to choose a hole in the same group.
        if ("Player A".equals(playerName) && lastOutcome != null && lastOutcome.equals("NEAR_MISS") && lastShotIndex != null) {
            int group = lastShotIndex / 5;
            List<Integer> groupIndices = new ArrayList<>();
            for (int i = group * 5; i < group * 5 + 5; i++) {
                if (!attemptedShots.contains(i)) {
                    groupIndices.add(i);
                }
            }
            if (!groupIndices.isEmpty()) {
                return groupIndices.get(random.nextInt(groupIndices.size()));
            }
        }
        // Otherwise, choose a random hole not yet attempted.
        int shotIndex;
        do {
            shotIndex = random.nextInt(50);
        } while (attemptedShots.contains(shotIndex));
        return shotIndex;
    }

    // Take a shot after waiting for a delay.
    public void takeShot() {
        try {
            // Use a longer delay (e.g., 5 secs) so the UI update is noticeable.
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int shotIndex = chooseShotIndex();
        attemptedShots.add(shotIndex);
        lastShotIndex = shotIndex; // Save the current shot index for strategy adjustments.
        Log.d("PlayerThread", playerName + " is taking shot at hole " + shotIndex);

        Message shotMsg = Message.obtain();
        shotMsg.what = MessageConstants.MSG_SHOT;
        shotMsg.arg1 = shotIndex;
        shotMsg.obj = playerName;
        uiHandler.sendMessage(shotMsg);
    }
}