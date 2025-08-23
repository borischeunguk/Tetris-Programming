package org.drw.standard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;


public class TetrisGame {
    private static final int WIDTH = 10;
    private static final int FULL_ROW_MASK = (1 << WIDTH) - 1;
    private static final Logger logger = LogManager.getLogger(TetrisGame.class);

    private List<Integer> grid = new ArrayList<>(); // row 0 = bottom
    private final boolean resettleEnabled;

    public TetrisGame() {
        this(false);
    }

    public TetrisGame(boolean resettleEnabled) {
        this.resettleEnabled = resettleEnabled;
    }

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
            // To enable resettling from main, you could check a command-line arg, for example.
            // For now, it uses the default (false).
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
        logger.debug("Dropped {} at {}, height: {}\n{}", piece, x, getHeight(), this);

        if(resettleEnabled){
            while(clearFullRows()) {
                logger.debug("Grid after clearing rows:\n{}", this);
                resettleFloatingIslands();
                logger.debug("Grid after resettling islands:\n{}", this);
            }
        }else{
            clearFullRows();
            logger.debug("Grid after clearing rows:\n{}", this);
        }
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

    private boolean clearFullRows() {
        boolean clearedOnce = false;
        boolean clearedInLoop;
        do {
            clearedInLoop = grid.removeIf(row -> row == FULL_ROW_MASK);
            if (clearedInLoop) {
                clearedOnce = true;
            }
        } while (clearedInLoop && !grid.isEmpty());
        return clearedOnce;
    }

    // Resettle floating islands after line clears.
    // This is not required by the spec, but is an interesting extension.
    private void resettleFloatingIslands() {
        if (grid.isEmpty()) return;

        List<Integer> originalGrid = new ArrayList<>(grid);
        boolean[][] visited = new boolean[originalGrid.size()][WIDTH];
        grid.clear();

        for (int y = originalGrid.size() - 1; y >= 0; y--) {
            for (int x = 0; x < WIDTH; x++) {
                if (!visited[y][x] && (originalGrid.get(y) & (1 << x)) != 0) {
                    // Found a new, unvisited island.
                    Map<Integer, Integer> island = new HashMap<>();
                    collectIsland(y, x, originalGrid, visited, island);
                    dropIsland(island);
                }
            }
        }
    }

    private void collectIsland(int startY, int startX, List<Integer> sourceGrid, boolean[][] visited, Map<Integer, Integer> island) {
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startY, startX});
        visited[startY][startX] = true;

        int minIslandY = startY;

        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int curY = pos[0];
            int curX = pos[1];

            minIslandY = Math.min(minIslandY, curY);
            int bit = 1 << curX;
            island.put(curY, island.getOrDefault(curY, 0) | bit);

            int[] dy = {-1, 1, 0, 0};
            int[] dx = {0, 0, -1, 1};

            for (int i = 0; i < 4; i++) {
                int nextY = curY + dy[i];
                int nextX = curX + dx[i];

                if (nextY >= 0 && nextY < sourceGrid.size() && nextX >= 0 && nextX < WIDTH &&
                        !visited[nextY][nextX] && (sourceGrid.get(nextY) & (1 << nextX)) != 0) {
                    visited[nextY][nextX] = true;
                    stack.push(new int[]{nextY, nextX});
                }
            }
        }

        // Normalize island to be relative to its own lowest point
        Map<Integer, Integer> normalizedIsland = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : island.entrySet()) {
            normalizedIsland.put(entry.getKey() - minIslandY, entry.getValue());
        }
        island.clear();
        island.putAll(normalizedIsland);
    }

    private void dropIsland(Map<Integer, Integer> island) {
        int y = getHeight();
        while (true) {
            if (y == 0) break;
            if (collides(island, y - 1)) break;
            y--;
        }
        place(island, y);
    }
}