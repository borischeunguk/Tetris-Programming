package org.drw.standard;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;


public class TetrisGame {
    private static final int WIDTH = 10;
    private static final int FULL_ROW_MASK = (1 << WIDTH) - 1;

    private final List<Integer> grid = new ArrayList<>(); // row 0 = bottom

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                System.out.println(0);
                continue;
            }
            List<Drop> seq = Parser.parseLine(line);
            TetrisGame game = new TetrisGame();
            for (Drop d : seq) {
                game.drop(d.piece(), d.x());
            }
            System.out.println(game.getHeight());
        }
    }

    /** Returns a string representation of a grid for debugging. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int height = getHeight();
        if (height == 0) {
            return "(empty)";
        }
        for (int i = height - 1; i >= 0; i--) {
            sb.append(String.format("%2d: ", i));
            int row = grid.get(i);
            for (int j = 0; j < WIDTH; j++) {
                if ((row & (1 << j)) != 0) {
                    sb.append('#');
                } else {
                    sb.append('.');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Drop a piece at column x, apply line clears. */
    public void drop(Piece piece, int x) {
        Map<Integer, Integer> rows = piece.buildRowMasks(x);

        int y = getHeight();
        while (true) {
            if (y == 0) break;
            if (collides(rows, y - 1)) break;
            y--;
        }

        place(rows, y);
        clearFullRows();

        // --- Debugging output ---
         System.out.println("Dropped " + piece + " at " + x + ", height: " + getHeight());
         System.out.println(this);
         System.out.println("--------------------");
    }

    /** Return current height (top non-empty row + 1). */
    public int getHeight() {
        for (int i = grid.size() - 1; i >= 0; i--) {
            if (grid.get(i) != 0) return i + 1;
        }
        return 0;
    }

    // ----- internals -----

    private boolean collides(Map<Integer, Integer> rows, int y) {
        for (var e : rows.entrySet()) {
            int rowIdx = y + e.getKey();
            if (rowIdx < 0) return true; // Collision with the floor
            if (rowIdx < grid.size() && (grid.get(rowIdx) & e.getValue()) != 0) {
                return true;
            }
        }
        return false;
    }

    private void place(Map<Integer, Integer> rows, int y) {
        int maxDy = rows.keySet().stream().max(Integer::compare).orElse(0);
        ensureRows(y + maxDy);
        for (var e : rows.entrySet()) {
            int rowIdx = y + e.getKey();
            grid.set(rowIdx, grid.get(rowIdx) | e.getValue());
        }
    }

    private void ensureRows(int idxInclusive) {
        while (grid.size() <= idxInclusive) {
            grid.add(0);
        }
    }

    private void clearFullRows() {
        boolean cleared;
        do {
            cleared = grid.removeIf(row -> row == FULL_ROW_MASK);
        } while (cleared && !grid.isEmpty());
    }
}