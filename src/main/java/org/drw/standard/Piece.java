package org.drw.standard;

import java.util.*;

/**
 * Represents the seven standard Tetris pieces (tetrominoes).
 * Each piece is defined by a set of relative coordinates for its constituent blocks.
 *
 * <p>Coordinate system: The origin (0,0) represents the bottom-left corner of the piece's
 * bounding box. X increases rightward, Y increases upward.
 *
 * <p>All pieces are defined in their standard orientation. This implementation does not
 * support piece rotation, which is consistent with the problem requirements.
 *
 * <p>Performance: Piece operations are O(1) for width calculation and O(k) for bitmask
 * generation where k is the number of blocks in the piece (always 4 for tetrominoes).
 *
 * @author George Bo Zhang
 * @version 1.0
 * @since 2025
 */
public enum Piece {
    /** I-piece: Straight line of 4 blocks horizontally */
    I(new int[][] { {0,0},{1,0},{2,0},{3,0} }),

    /** Q-piece (also called O): 2x2 square block */
    Q(new int[][] { {0,0},{1,0},{0,1},{1,1} }),

    /** T-piece: T-shaped piece with 3 blocks on bottom and 1 on top center */
    T(new int[][] { {0,1},{1,1},{2,1},{1,0} }),

    /** Z-piece: Z-shaped piece, offset stairs going down-right */
    Z(new int[][] { {1,0},{2,0},{0,1},{1,1} }),

    /** S-piece: S-shaped piece, offset stairs going up-right */
    S(new int[][] { {0,0},{1,0},{1,1},{2,1} }),

    /** L-piece: L-shaped piece with vertical line and bottom-right extension */
    L(new int[][] { {0,0},{0,1},{0,2},{1,0} }),

    /** J-piece: J-shaped piece with vertical line and bottom-left extension */
    J(new int[][] { {1,0},{1,1},{1,2},{0,0} });

    /**
     * The shape of the piece, defined as an array of [dx, dy] coordinates
     * relative to the piece's local origin (bottom-left of bounding box).
     * Each coordinate represents one block of the tetromino.
     */
    private final int[][] shape;

    /** Cached width value for performance optimization */
    private final int width;

    /**
     * Constructs a Piece with a given shape.
     *
     * @param shape The 2D array of relative coordinates defining the piece.
     *              Each sub-array must contain exactly 2 elements [x, y].
     * @throws IllegalArgumentException if shape is invalid
     */
    Piece(int[][] shape) {
        if (shape == null || shape.length != 4) {
            throw new IllegalArgumentException("Tetromino must have exactly 4 blocks");
        }

        for (int i = 0; i < shape.length; i++) {
            if (shape[i] == null || shape[i].length != 2) {
                throw new IllegalArgumentException("Each coordinate must be [x, y] pair");
            }
            if (shape[i][0] < 0 || shape[i][1] < 0) {
                throw new IllegalArgumentException("Coordinates must be non-negative");
            }
        }

        this.shape = Arrays.copyOf(shape, shape.length);
        // Deep copy to prevent external modification
        for (int i = 0; i < this.shape.length; i++) {
            this.shape[i] = Arrays.copyOf(shape[i], 2);
        }

        // Pre-calculate width for performance
        int maxX = 0;
        for (int[] p : this.shape) {
            maxX = Math.max(maxX, p[0]);
        }
        this.width = maxX + 1;
    }

    /**
     * Calculates the width of the piece's bounding box.
     * This is used for validating that a piece is dropped within the game board's horizontal bounds.
     *
     * @return The width of the piece in blocks (always between 1 and 4 for standard tetrominoes)
     */
    public int getWidth() {
        return width;
    }

    /**
     * Calculates the height of the piece's bounding box.
     * This is used for collision detection and grid management.
     *
     * @return The height of the piece in blocks (always between 1 and 4 for standard tetrominoes)
     */
    public int getHeight() {
        int maxY = 0;
        for (int[] p : shape) {
            maxY = Math.max(maxY, p[1]);
        }
        return maxY + 1;
    }

    /**
     * Converts the piece's shape into a map of bitmasks for efficient placement on the grid.
     * Each entry in the map represents a row of the piece, where the key is the relative
     * vertical offset (dy) and the value is an integer bitmask representing the occupied cells.
     *
     * @param x The horizontal (leftmost) column on the main grid where the piece is being placed.
     *          Must be non-negative and leave enough space for the piece width.
     * @return A map of relative row index to its corresponding bitmask
     * @throws IllegalArgumentException if x is negative or would cause out-of-bounds placement
     */
    public Map<Integer,Integer> buildRowMasks(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("X position cannot be negative: " + x);
        }
        if (x + getWidth() > 10) { // Assuming standard Tetris width of 10
            throw new IllegalArgumentException(
                String.format("Piece %s at x=%d would exceed grid width (piece width: %d)",
                             this, x, getWidth()));
        }

        Map<Integer,Integer> rows = new HashMap<>();
        for (int[] p : shape) {
            int dx = p[0], dy = p[1];
            int bit = 1 << (x + dx);
            rows.put(dy, rows.getOrDefault(dy, 0) | bit);
        }
        return rows;
    }

    /**
     * A factory method to get a Piece from its single-letter string representation.
     * It handles the common alias 'O' for the square piece 'Q'.
     *
     * @param s The single-letter string representing the piece (case-insensitive).
     *          Accepts standard piece letters: I, J, L, O/Q, S, T, Z
     * @return The corresponding {@link Piece} enum constant
     * @throws IllegalArgumentException if the letter does not correspond to a valid piece
     * @throws NullPointerException if s is null
     */
    public static Piece fromLetter(String s) {
        if (s == null) {
            throw new NullPointerException("Piece letter cannot be null");
        }
        if (s.trim().isEmpty()) {
            throw new IllegalArgumentException("Piece letter cannot be empty");
        }

        String trimmed = s.trim().toUpperCase();
        if (trimmed.length() != 1) {
            throw new IllegalArgumentException("Piece letter must be exactly one character: " + s);
        }

        // Handle the O/Q alias
        if ("O".equals(trimmed)) {
            return Q;
        }

        try {
            return Piece.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid piece letter: " + s + ". Valid pieces are: I, J, L, O/Q, S, T, Z");
        }
    }

    /**
     * Returns a set of all valid piece letters that can be used with {@link #fromLetter(String)}.
     *
     * @return An unmodifiable set containing all valid piece letters
     */
    public static Set<String> getValidLetters() {
        return Set.of("I", "J", "L", "O", "Q", "S", "T", "Z");
    }

    /**
     * Returns the shape coordinates of this piece.
     *
     * @return A defensive copy of the shape array to prevent external modification
     */
    public int[][] getShape() {
        int[][] copy = new int[shape.length][];
        for (int i = 0; i < shape.length; i++) {
            copy[i] = Arrays.copyOf(shape[i], shape[i].length);
        }
        return copy;
    }
}