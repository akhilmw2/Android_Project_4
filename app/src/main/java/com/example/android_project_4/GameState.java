package com.example.android_project_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    public List<Hole> holes = new ArrayList<>();
    public int winningHoleIndex;

    public void initializeHoles() {
        for (int i = 0; i < 50; i++) {
            holes.add(new Hole(i));
        }
        Random rand = new Random();
        winningHoleIndex = rand.nextInt(50);
        holes.get(winningHoleIndex).setWinning(true);
    }

    public static class Hole {
        private int id;
        private boolean isWinning;
        private String occupiedBy;  // e.g., "Player A", "Player B"
        private String outcome;     // e.g., "BIG_MISS", "NEAR_MISS", etc.

        public Hole(int id) {
            this.id = id;
            this.isWinning = false;
            this.occupiedBy = "";
            this.outcome = "";
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

        public String getOutcome() {
            return outcome;
        }

        public void setOutcome(String outcome) {
            this.outcome = outcome;
        }
    }
}
