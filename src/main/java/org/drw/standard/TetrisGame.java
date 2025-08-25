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
 *
 * <p>This implementation supports two modes:
 * <ul>
 *   <li>Standard mode: Pieces drop and lines clear without resettling floating blocks</li>
 *   <li>Resettle mode: After line clears, floating blocks drop down to fill gaps</li>
 * </ul>
 *
 * <p>Thread safety: This class is not thread-safe and should not be used concurrently
 * without external synchronization.
 *
 * <p>Performance characteristics:
 * <ul>
 *   <li>Drop operation: O(h) where h is the current height</li>
 *   <li>Line clearing: O(h) in worst case</li>
 *   <li>Resettling: O(w*h) where w is width and h is height</li>
 * </ul>
 *
 * @author Geroge Bo Zhang
 * @version 1.0
 * @since 2025
 */
public class TetrisGame {
    /** The width of the game board. */
    private static final int WIDTH = 10;
    /** A bitmask representing a completely full row, used for line clearing. */
    private static final int FULL_ROW_MASK = (1 << WIDTH) - 1;
    /** Logger for debugging and informational output. */
    private static final Logger logger = LogManager.getLogger(TetrisGame.class);
    /** Maximum reasonable grid height to prevent memory issues. */
    private static final int MAX_GRID_HEIGHT = 1000;

    /** The game grid, where each integer is a bitmask for a row. Row 0 is the bottom. */
    private final List<Integer> grid = new ArrayList<>();
    /** A flag to control whether to resettle floating blocks after a line clear. */
    private final boolean resettleEnabled;

    /**
     * Default constructor. Initializes a game with the 'resettle floating islands' feature disabled.
     * This is the standard Tetris behavior where cleared lines don't cause floating blocks to fall.
     */
    public TetrisGame() {
        this(false);
    }

    /**
     * Constructs a TetrisGame with a specific setting for the resettling feature.
     *
     * @param resettleEnabled If true, floating blocks will drop down after line clears.
     *                       If false, follows standard Tetris rules where floating blocks remain.
     * @throws IllegalArgumentException if invalid parameters are provided
     */
    public TetrisGame(boolean resettleEnabled) {
        this.resettleEnabled = resettleEnabled;
        logger.debug("TetrisGame initialized with resettleEnabled={}", resettleEnabled);
    }

    /**
     * Main entry point for running the game from the command line.
     * Reads sequences of piece drops from standard input and prints the final height for each.
     *
     * <p>Input format: Each line contains comma-separated piece drops (e.g., "Q0,I4,T1").
     * Empty lines result in height 0.
     *
     * @param args Command line arguments. Currently supports:
     *            --resettle: Enable floating block resettling after line clears
     * @throws Exception if there is an error reading input or parsing
     */
    public static void main(String[] args) throws Exception {
        boolean enableResettle = Arrays.asList(args).contains("--resettle");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    System.out.println(0);
                    continue;
                }

                try {
                    List<Drop> seq = Parser.parseLine(line);
                    TetrisGame game = new TetrisGame(enableResettle);
                    for (Drop d : seq) {
                        game.drop(d.piece(), d.x());
                    }
                    System.out.println(game.getHeight());
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid input line: {}", line, e);
                    System.err.println("Error processing line: " + e.getMessage());
                    System.out.println(0); // Output 0 for invalid lines
                }
            }
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
     * @param piece The piece to drop. Must not be null.
     * @param x     The starting horizontal column for the piece. Must be valid for the piece width.
     * @throws IllegalArgumentException if piece is null or x is out of bounds
     * @throws IllegalStateException if the grid would exceed maximum height
     */
    public void drop(Piece piece, int x) {
        if (piece == null) {
            throw new IllegalArgumentException("Piece cannot be null");
        }
        if (x < 0 || x > WIDTH - piece.getWidth()) {
            throw new IllegalArgumentException(
                String.format("Invalid x position %d for piece %s (width %d)", x, piece, piece.getWidth()));
        }

        Map<Integer, Integer> rows = piece.buildRowMasks(x);

        int y = getHeight();
        while (true) {
            if (y == 0) break;
            if (collides(rows, y - 1)) break;
            y--;
        }

        // Check for grid height limit
        int maxRequiredRow = y + rows.keySet().stream().max(Integer::compare).orElse(0);
        if (maxRequiredRow >= MAX_GRID_HEIGHT) {
            throw new IllegalStateException("Grid height would exceed maximum limit: " + MAX_GRID_HEIGHT);
        }

        place(rows, y);
        logger.debug("Dropped {} at x={}, final y={}, height: {}", piece, x, y, getHeight());

        if (resettleEnabled) {
            while (clearFullRows()) {
                logger.debug("Grid after clearing rows:\n{}", this);
                resettleFloatingIslands();
                logger.debug("Grid after resettling islands:\n{}", this);
            }
        } else {
            if (clearFullRows()) {
                logger.debug("Grid after clearing rows:\n{}", this);
            }
        }
    }

    /**
     * Calculates the current height of the grid.
     *
     * @return The number of the highest non-empty row plus one. Returns 0 for an empty grid.
     *         This value represents the number of rows currently occupied.
     */
    public int getHeight() {
        for (int i = grid.size() - 1; i >= 0; i--) {
            if (grid.get(i) != 0) return i + 1;
        }
        return 0;
    }

    /**
     * Gets the current state of the grid for testing and debugging purposes.
     *
     * @return An unmodifiable view of the current grid state
     */
    public List<Integer> getGridState() {
        return Collections.unmodifiableList(grid);
    }

    /**
     * Checks if the resettle feature is enabled for this game.
     *
     * @return true if floating blocks will be resettled after line clears, false otherwise
     */
    public boolean isResettleEnabled() {
        return resettleEnabled;
    }

    // ----- internals -----

    /**
     * Checks if a piece at a given vertical position collides with existing blocks.
     *
     * @param rows A map of the piece's row bitmasks relative to the piece's bottom row
     * @param y    The target bottom-most row index for the piece on the main grid
     * @return True if there is a collision, false otherwise
     */
    private boolean collides(Map<Integer, Integer> rows, int y) {
        for (var e : rows.entrySet()) {
            int rowIdx = y + e.getKey();
            if (rowIdx < 0) return true; // Collision with the floor
            if (rowIdx < grid.size() && (grid.get(rowIdx) & e.getValue()) != 0) {
                return true; // Collision with existing blocks
            }
        }
        return false;
    }

    /**
     * Places a piece's bitmasks onto the grid at a specific vertical position.
     *
     * @param rows The map of the piece's row bitmasks relative to the piece's bottom row
     * @param y    The final bottom-most row index for the piece on the main grid
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
     *
     * @param idxInclusive The highest row index that needs to exist (0-based)
     */
    private void ensureRows(int idxInclusive) {
        while (grid.size() <= idxInclusive) {
            grid.add(0);
        }
    }

    /**
     * Removes all full rows from the grid and returns whether any were cleared.
     * This is an atomic operation that removes all full rows in a single pass.
     *
     * @return True if at least one row was cleared, false otherwise
     */
    private boolean clearFullRows() {
        boolean removed = grid.removeIf(row -> row == FULL_ROW_MASK);
        if (removed) {
            logger.debug("Cleared full rows, new height: {}", getHeight());
        }
        return removed;
    }

    /**
     * (Optional Feature) Finds all contiguous groups of blocks ("islands") and drops them
     * individually as if they were new pieces. This settles any floating blocks
     * left after a line clear.
     *
     * <p>Algorithm: Uses depth-first search to identify connected components,
     * then drops each component from top to bottom to maintain proper stacking order.
     *
     * <p>Time complexity: O(width * height) in worst case.
     */
    private void resettleFloatingIslands() {
        if (grid.isEmpty()) return;

        List<Integer> originalGrid = new ArrayList<>(grid);
        boolean[][] visited = new boolean[originalGrid.size()][WIDTH];
        grid.clear();

        // Iterate from top to bottom to drop higher islands first.
        // This ensures proper stacking when multiple islands exist.
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
     * The search considers 4-connectivity (up, down, left, right).
     *
     * @param startY The starting Y coordinate for the search
     * @param startX The starting X coordinate for the search
     * @param sourceGrid The original grid to search within
     * @param visited A 2D array to track visited cells to avoid reprocessing
     * @param island  The map to populate with the island's normalized bitmasks
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
     * Drops a collected island onto the current game grid using the same logic as piece drops.
     *
     * @param island A map of bitmasks representing the island, normalized to its own origin
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