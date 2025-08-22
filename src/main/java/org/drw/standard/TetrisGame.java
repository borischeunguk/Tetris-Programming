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

    /** Drop a piece at column x, apply line clears. */
    public void drop(Piece piece, int x) {
        Map<Integer, Integer> rows = piece.buildRowMasks(x);

        int y = getHeight() + 10; // start above stack
        while (true) {
            if (y == 0) break;
            if (collides(rows, y - 1)) break;
            y--;
        }

        place(rows, y);
        clearFullRows();
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
        int write = 0;
        for (int read = 0; read < grid.size(); read++) {
            int row = grid.get(read);
            if (row != FULL_ROW_MASK) {
                if (write != read) grid.set(write, row);
                write++;
            }
        }
        while (grid.size() > write) {
            grid.remove(grid.size() - 1);
        }
    }
}

