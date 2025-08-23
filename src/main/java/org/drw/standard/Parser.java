package org.drw.standard;

import java.util.*;

/**
 * A utility class responsible for parsing input strings into a sequence of Tetris drops.
 */
public class Parser {
    /**
     * Parses a single line of input into a list of {@link Drop} commands.
     * The input line is expected to be a comma-separated list of piece drops,
     * e.g., "Q0,I4,T1". Each token consists of a piece letter and a 0-indexed
     * column number.
     *
     * @param line The input string to parse.
     * @return A {@link List} of {@link Drop} objects representing the parsed sequence.
     * @throws IllegalArgumentException if a piece is placed out of bounds or if the piece letter is invalid.
     */
    public static List<Drop> parseLine(String line) {
        List<Drop> result = new ArrayList<>();
        for (String tok : line.split(",")) {
            String t = tok.trim();
            if (t.isEmpty()) continue;
            int i = 0;
            while (i < t.length() && !Character.isDigit(t.charAt(i)) && t.charAt(i) != '-') i++;
            String letter = t.substring(0, i);
            int x = Integer.parseInt(t.substring(i));
            Piece piece = Piece.fromLetter(letter);
            if (x < 0 || x > 10 - piece.getWidth()) {
                throw new IllegalArgumentException("Piece " + piece + " at x=" + x + " out of bounds");
            }
            result.add(new Drop(piece, x));
        }
        return result;
    }
}