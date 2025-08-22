package org.drw.standard;

import java.util.*;

public class Parser {
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

