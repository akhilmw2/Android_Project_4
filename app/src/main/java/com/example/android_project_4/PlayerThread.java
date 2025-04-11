package com.example.android_project_4;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class PlayerThread extends Thread {
    private String playerName;
    private GameState gameState;
    public Handler handler;

    public PlayerThread(String name, GameState state) {
        this.playerName = name;
        this.gameState = state;
    }

    @Override
    public void run() {
        // Prepare the looper for handling messages on this thread.
        Looper.prepare();

        // Initialize the thread’s handler.
        handler = new Handler(msg -> {
            processMessage(msg);
            return true;
        });

        // Log that the thread is ready.
        Log.d("PlayerThread", playerName + " is ready for messages.");

        // Enter the loop to keep waiting for messages.
        Looper.loop();
    }

    // Process incoming messages (for now, just log the message).
    private void processMessage(Message msg) {
        Log.d("PlayerThread", playerName + " received message: " + msg.what);
    }

    // Placeholder method to simulate a shot.
    public void takeShot() {
        Log.d("PlayerThread", playerName + " is taking a shot.");
    }
}