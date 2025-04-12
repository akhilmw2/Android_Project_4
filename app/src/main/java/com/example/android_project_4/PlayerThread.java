package com.example.android_project_4;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PlayerThread extends Thread {
    private String playerName;
    private GameState gameState;
    public Handler handler;         // Receives messages (YOUR_TURN, SHOT_RESPONSE, GAME_OVER)
    private Handler uiHandler;       // Reference to MainActivity's handler for sending shots
    private Set<Integer> attemptedShots;
    private Random random;

    public PlayerThread(String name, GameState state, Handler uiHandler) {
        this.playerName = name;
        this.gameState = state;
        this.uiHandler = uiHandler;
        this.attemptedShots = new HashSet<>();
        this.random = new Random();
    }

    @Override
    public void run() {
        // Prepare the looper for this thread so it can process messages.
        Looper.prepare();

        // Initialize the thread's handler.
        handler = new Handler(Looper.myLooper(), msg -> {
            switch (msg.what) {
                case MessageConstants.MSG_YOUR_TURN:
                    Log.d("PlayerThread", playerName + " received YOUR_TURN");
                    takeShot();
                    break;
                case MessageConstants.MSG_SHOT_RESPONSE:
                    String outcome = (String) msg.obj;
                    Log.d("PlayerThread", playerName + " received outcome: " + outcome);
                    // Outcome processing can be expanded for strategy adjustments.
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

    // Simulate taking a shot. Wait 2 seconds, choose a hole that hasn't been tried before, then send the shot.
    public void takeShot() {
        try {
            Thread.sleep(5000); // Delay so a human viewer can see the previous move.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Choose a random hole index that hasn't been attempted by this player.
        int shotIndex;
        do {
            shotIndex = random.nextInt(50);
        } while (attemptedShots.contains(shotIndex));
        attemptedShots.add(shotIndex);

        Log.d("PlayerThread", playerName + " is taking shot at hole " + shotIndex);

        // Create and send the shot message to the UI thread.
        Message shotMsg = Message.obtain();
        shotMsg.what = MessageConstants.MSG_SHOT;
        shotMsg.arg1 = shotIndex;
        shotMsg.obj = playerName;
        uiHandler.sendMessage(shotMsg);
    }
}