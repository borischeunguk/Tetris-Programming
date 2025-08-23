package org.drw.standard;

import java.util.*;

public enum Piece {
    I(new int[][] { {0,0},{1,0},{2,0},{3,0} }),
    Q(new int[][] { {0,0},{1,0},{0,1},{1,1} }),
    T(new int[][] { {0,1},{1,1},{2,1},{1,0} }),
    Z(new int[][] { {1,0},{2,0},{0,1},{1,1} }),
    S(new int[][] { {0,0},{1,0},{1,1},{2,1} }),
    L(new int[][] { {0,0},{0,1},{0,2},{1,0} }),
    J(new int[][] { {1,0},{1,1},{1,2},{0,0} });

    private final int[][] shape;

    Piece(int[][] shape) {
        this.shape = shape;
    }

    /** Width for validity checks. */
    public int getWidth() {
        int max = 0;
        for (int[] p : shape) max = Math.max(max, p[0]);
        return max + 1;
    }

    /** Build dy → bitmask for given left x. */
    public Map<Integer,Integer> buildRowMasks(int x) {
        Map<Integer,Integer> rows = new HashMap<>();
        for (int[] p : shape) {
            int dx = p[0], dy = p[1];
            int bit = 1 << (x + dx);
            rows.put(dy, rows.getOrDefault(dy, 0) | bit);
        }
        return rows;
    }

    /** Alias O → Q. */
    public static Piece fromLetter(String s) {
        if (s.equalsIgnoreCase("O")) return Q;
        return Piece.valueOf(s.toUpperCase());
    }
}
