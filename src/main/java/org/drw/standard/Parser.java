package org.drw.standard;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A utility class responsible for parsing input strings into a sequence of Tetris drops.
 *
 * <p>This parser handles the standard input format for Tetris piece sequences:
 * comma-separated tokens where each token consists of a piece letter followed by
 * a column number (e.g., "Q0,I4,T1").
 *
 * <p>The parser is robust and handles various edge cases including:
 * <ul>
 *   <li>Whitespace around tokens and separators</li>
 *   <li>Empty tokens (which are ignored)</li>
 *   <li>Case-insensitive piece letters</li>
 *   <li>The O/Q piece alias</li>
 *   <li>Negative column numbers (validation)</li>
 *   <li>Out-of-bounds placement detection</li>
 * </ul>
 *
 * @author George Bo Zhang
 * @version 1.0
 * @since 2025
 */
public class Parser {

    /** Standard Tetris grid width for bounds checking */
    private static final int GRID_WIDTH = 10;

    /** Pattern to validate token format: letters followed by optional negative sign and digits */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z]+(-?\\d+)$");

    /**
     * Parses a single line of input into a list of {@link Drop} commands.
     * The input line is expected to be a comma-separated list of piece drops,
     * e.g., "Q0,I4,T1". Each token consists of a piece letter and a 0-indexed
     * column number.
     *
     * @param line The input string to parse. Can be null or empty.
     * @return A {@link List} of {@link Drop} objects representing the parsed sequence.
     *         Returns an empty list for null or empty input.
     * @throws IllegalArgumentException if a piece is placed out of bounds,
     *                                 if the piece letter is invalid,
     *                                 or if the token format is malformed
     */
    public static List<Drop> parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Drop> result = new ArrayList<>();
        String[] tokens = line.split(",");

        for (int tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++) {
            String tok = tokens[tokenIndex];
            String t = tok.trim();

            if (t.isEmpty()) {
                continue; // Skip empty tokens
            }

            try {
                Drop drop = parseToken(t);
                result.add(drop);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format("Error parsing token '%s' at position %d: %s",
                                 tok, tokenIndex, e.getMessage()), e);
            }
        }

        return result;
    }

    /**
     * Parses a single token into a Drop object.
     *
     * @param token The token to parse (e.g., "Q0", "I4", "T-1")
     * @return The parsed Drop object
     * @throws IllegalArgumentException if the token format is invalid or parameters are out of bounds
     */
    private static Drop parseToken(String token) {
        if (!TOKEN_PATTERN.matcher(token).matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid token format '%s'. Expected format: [piece][column] (e.g., Q0, I4)", token));
        }

        // Find the boundary between letters and digits
        int i = 0;
        while (i < token.length() && Character.isLetter(token.charAt(i))) {
            i++;
        }

        if (i == 0) {
            throw new IllegalArgumentException("Token must start with a piece letter: " + token);
        }
        if (i == token.length()) {
            throw new IllegalArgumentException("Token must include a column number: " + token);
        }

        String letter = token.substring(0, i);
        String numberStr = token.substring(i);

        // Parse piece
        Piece piece;
        try {
            piece = Piece.fromLetter(letter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid piece letter '" + letter + "'", e);
        }

        // Parse column number
        int x;
        try {
            x = Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid column number '" + numberStr + "'", e);
        }

        // Validate bounds
        validatePlacement(piece, x);

        return new Drop(piece, x);
    }

    /**
     * Validates that a piece can be legally placed at the given column.
     *
     * @param piece The piece to validate
     * @param x The column position
     * @throws IllegalArgumentException if the placement is out of bounds
     */
    private static void validatePlacement(Piece piece, int x) {
        if (x < 0) {
            throw new IllegalArgumentException(
                String.format("Column position cannot be negative: %d", x));
        }

        if (x > GRID_WIDTH - piece.getWidth()) {
            throw new IllegalArgumentException(
                String.format("Piece %s at column %d would exceed grid width %d (piece width: %d)",
                             piece, x, GRID_WIDTH, piece.getWidth()));
        }
    }

    /**
     * Parses multiple lines of input, where each line represents a separate game.
     *
     * @param lines The input lines to parse
     * @return A list of Drop sequences, one per input line
     * @throws IllegalArgumentException if any line contains invalid input
     */
    public static List<List<Drop>> parseLines(List<String> lines) {
        if (lines == null) {
            return new ArrayList<>();
        }

        List<List<Drop>> result = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            try {
                result.add(parseLine(lines.get(lineIndex)));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format("Error parsing line %d: %s", lineIndex + 1, e.getMessage()), e);
            }
        }

        return result;
    }

    /**
     * Validates the format of a token without fully parsing it.
     * Useful for input validation before processing.
     *
     * @param token The token to validate
     * @return true if the token has valid format, false otherwise
     */
    public static boolean isValidTokenFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            parseToken(token.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}