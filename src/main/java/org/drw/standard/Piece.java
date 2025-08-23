package org.drw.standard;

import java.util.*;

/**
 * Represents the seven standard Tetris pieces (tetrominoes).
 * Each piece is defined by a set of relative coordinates for its constituent blocks.
 */
public enum Piece {
    I(new int[][] { {0,0},{1,0},{2,0},{3,0} }),
    Q(new int[][] { {0,0},{1,0},{0,1},{1,1} }),
    T(new int[][] { {0,1},{1,1},{2,1},{1,0} }),
    Z(new int[][] { {1,0},{2,0},{0,1},{1,1} }),
    S(new int[][] { {0,0},{1,0},{1,1},{2,1} }),
    L(new int[][] { {0,0},{0,1},{0,2},{1,0} }),
    J(new int[][] { {1,0},{1,1},{1,2},{0,0} });

    /**
     * The shape of the piece, defined as an array of [dx, dy] coordinates
     * relative to the piece's local origin (typically its top-left corner).
     */
    private final int[][] shape;

    /**
     * Constructs a Piece with a given shape.
     * @param shape The 2D array of relative coordinates defining the piece.
     */
    Piece(int[][] shape) {
        this.shape = shape;
    }

    /**
     * Calculates the width of the piece's bounding box.
     * This is used for validating that a piece is dropped within the game board's horizontal bounds.
     * @return The width of the piece in blocks.
     */
    public int getWidth() {
        int max = 0;
        for (int[] p : shape) max = Math.max(max, p[0]);
        return max + 1;
    }

    /**
     * Converts the piece's shape into a map of bitmasks for efficient placement on the grid.
     * Each entry in the map represents a row of the piece, where the key is the relative
     * vertical offset (dy) and the value is an integer bitmask representing the occupied cells.
     *
     * @param x The horizontal (leftmost) column on the main grid where the piece is being placed.
     * @return A map of relative row index to its corresponding bitmask.
     */
    public Map<Integer,Integer> buildRowMasks(int x) {
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
     * @return The corresponding {@link Piece} enum constant.
     * @throws IllegalArgumentException if the letter does not correspond to a valid piece.
     */
    public static Piece fromLetter(String s) {
        if (s.equalsIgnoreCase("O")) return Q;
        return Piece.valueOf(s.toUpperCase());
    }
}