package com.example.android_project_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    public List<Hole> holes = new ArrayList<>();
    public int winningHoleIndex;

    // Initializes 50 holes and randomly marks one as the winning hole.
    public void initializeHoles() {
        for (int i = 0; i < 50; i++) {
            holes.add(new Hole(i));
        }
        Random rand = new Random();
        winningHoleIndex = rand.nextInt(50);
        holes.get(winningHoleIndex).setWinning(true);
    }

    // Represents a single hole.
    public static class Hole {
        private int id;
        private boolean isWinning;
        private String occupiedBy;  // Future use: "Player A" or "Player B"

        public Hole(int id) {
            this.id = id;
            this.isWinning = false;
            this.occupiedBy = "";
        }

        public int getId() {
            return id;
        }

        public boolean isWinning() {
            return isWinning;
        }

        public void setWinning(boolean winning) {
            isWinning = winning;
        }

        public String getOccupiedBy() {
            return occupiedBy;
        }

        public void setOccupiedBy(String occupiedBy) {
            this.occupiedBy = occupiedBy;
        }
    }
}
