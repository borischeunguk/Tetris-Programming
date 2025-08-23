package org.drw.standard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Manages the state and logic of a Tetris game on a 10-wide grid.
 * The grid is represented by a list of integer bitmasks, where each bit
 * corresponds to a cell in a row.
 */
public class TetrisGame {
    /** The width of the game board. */
    private static final int WIDTH = 10;
    /** A bitmask representing a completely full row, used for line clearing. */
    private static final int FULL_ROW_MASK = (1 << WIDTH) - 1;
    /** Logger for debugging and informational output. */
    private static final Logger logger = LogManager.getLogger(TetrisGame.class);

    /** The game grid, where each integer is a bitmask for a row. Row 0 is the bottom. */
    private List<Integer> grid = new ArrayList<>();
    /** A flag to control whether to resettle floating blocks after a line clear. */
    private final boolean resettleEnabled;

    /**
     * Default constructor. Initializes a game with the 'resettle floating islands' feature disabled.
     */
    public TetrisGame() {
        this(false);
    }

    /**
     * Constructs a TetrisGame with a specific setting for the resettling feature.
     * @param resettleEnabled If true, floating blocks will drop down after line clears.
     */
    public TetrisGame(boolean resettleEnabled) {
        this.resettleEnabled = resettleEnabled;
    }

    /**
     * Main entry point for running the game from the command line.
     * Reads sequences of piece drops from standard input and prints the final height for each.
     * @param args Command line arguments (not used).
     * @throws Exception if there is an error reading input or parsing.
     */
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

    /**
     * Returns a string representation of the current grid state for debugging.
     * @return A multi-line string visualizing the grid, or "(empty)" if the grid is empty.
     */
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

    /**
     * Processes a single piece drop. This involves finding the final resting height of the piece,
     * placing it on the grid, and then clearing any full lines.
     *
     * @param piece The piece to drop.
     * @param x     The starting horizontal column for the piece.
     */
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
            if (clearFullRows()) {
                logger.debug("Grid after clearing rows:\n{}", this);
            }
        }
    }

    /**
     * Calculates the current height of the grid.
     * @return The number of the highest non-empty row plus one. Returns 0 for an empty grid.
     */
    public int getHeight() {
        for (int i = grid.size() - 1; i >= 0; i--) {
            if (grid.get(i) != 0) return i + 1;
        }
        return 0;
    }

    // ----- internals -----

    /**
     * Checks if a piece at a given vertical position collides with existing blocks.
     * @param rows A map of the piece's row bitmasks.
     * @param y    The target bottom-most row index for the piece.
     * @return True if there is a collision, false otherwise.
     */
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

    /**
     * Places a piece's bitmasks onto the grid at a specific vertical position.
     * @param rows The map of the piece's row bitmasks.
     * @param y    The final bottom-most row index for the piece.
     */
    private void place(Map<Integer, Integer> rows, int y) {
        int maxDy = rows.keySet().stream().max(Integer::compare).orElse(0);
        ensureRows(y + maxDy);
        for (var e : rows.entrySet()) {
            int rowIdx = y + e.getKey();
            grid.set(rowIdx, grid.get(rowIdx) | e.getValue());
        }
    }

    /**
     * Ensures the grid has enough rows to accommodate a piece being placed.
     * @param idxInclusive The highest row index that needs to exist.
     */
    private void ensureRows(int idxInclusive) {
        while (grid.size() <= idxInclusive) {
            grid.add(0);
        }
    }

    /**
     * Removes all full rows from the grid.
     * @return True if at least one row was cleared, false otherwise.
     */
    private boolean clearFullRows() {
        return grid.removeIf(row -> row == FULL_ROW_MASK);
    }

    /**
     * (Optional Feature) Finds all contiguous groups of blocks ("islands") and drops them
     * individually as if they were new pieces. This settles any floating blocks
     * left after a line clear.
     */
    private void resettleFloatingIslands() {
        if (grid.isEmpty()) return;

        List<Integer> originalGrid = new ArrayList<>(grid);
        boolean[][] visited = new boolean[originalGrid.size()][WIDTH];
        grid.clear();

        // Iterate from top to bottom to drop higher islands first.
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

    /**
     * Uses a depth-first search (DFS) to find all connected blocks belonging to a single island.
     * @param startY The starting Y coordinate for the search.
     * @param startX The starting X coordinate for the search.
     * @param sourceGrid The original grid to search within.
     * @param visited A 2D array to track visited cells to avoid reprocessing.
     * @param island  The map to populate with the island's normalized bitmasks.
     */
    private void collectIsland(int startY, int startX, List<Integer> sourceGrid, boolean[][] visited, Map<Integer, Integer> island) {
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startY, startX});
        visited[startY][startX] = true;

        int minIslandY = startY;
        Map<Integer, Integer> rawIsland = new HashMap<>();

        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int curY = pos[0];
            int curX = pos[1];

            minIslandY = Math.min(minIslandY, curY);
            int bit = 1 << curX;
            rawIsland.put(curY, rawIsland.getOrDefault(curY, 0) | bit);

            // Check 4 neighbors (up, down, left, right)
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

        // Normalize island coordinates to be relative to its own lowest point (dy=0)
        for (Map.Entry<Integer, Integer> entry : rawIsland.entrySet()) {
            island.put(entry.getKey() - minIslandY, entry.getValue());
        }
    }

    /**
     * Drops a collected island onto the current game grid.
     * @param island A map of bitmasks representing the island, normalized to its own origin.
     */
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