package org.drw.standard;

/**
 * Represents a single piece being dropped at a specific horizontal position.
 * This is an immutable data carrier class that encapsulates the essential information
 * needed to execute a Tetris piece drop operation.
 *
 * <p>This record is used throughout the system to represent parsed input commands
 * and to maintain a clear separation between input parsing and game logic.
 *
 * <p>Validation: The record itself does not perform validation - validation is
 * handled by the {@link Parser} class during creation and by {@link TetrisGame}
 * during execution to ensure proper error handling and user feedback.
 *
 * @param piece The type of Tetris piece being dropped. Must not be null.
 * @param x     The 0-indexed horizontal starting column for the drop.
 *              Must be non-negative and within valid bounds for the piece width.
 *
 * @author George Bo Zhang
 * @version 1.0
 * @since 2025
 */
public record Drop(Piece piece, int x) {

    /**
     * Creates a new Drop with validation.
     *
     * @param piece The piece to drop
     * @param x The column position
     * @throws IllegalArgumentException if piece is null or x is negative
     */
    public Drop {
        if (piece == null) {
            throw new IllegalArgumentException("Piece cannot be null");
        }
        if (x < 0) {
            throw new IllegalArgumentException("Column position cannot be negative: " + x);
        }
    }

    /**
     * Returns a string representation suitable for debugging and logging.
     *
     * @return A formatted string showing the piece and position (e.g., "Drop[Q at x=0]")
     */
    @Override
    public String toString() {
        return String.format("Drop[%s at x=%d]", piece, x);
    }
}
