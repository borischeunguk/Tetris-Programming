package org.drw.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TetrisGameTest {

    /** Helper to run a single input line through a fresh game. */
    private int run(String line) {
        List<Drop> seq = Parser.parseLine(line);
        TetrisGame game = new TetrisGame();
        for (Drop d : seq) {
            game.drop(d.piece(), d.x());
        }
        return game.getHeight();
    }

    // ---------- Parameterized tests for compactness ----------

    @ParameterizedTest(name = "Sequence {0} should result in height {1}")
    @CsvSource({
            // Single pieces
            "I0, 1",
            "I6, 1",
            "Q0, 2",
            "O4, 2",
            "T0, 2",

            // Examples from spec
            "'I0,I4,Q8', 1",
            "'T1,Z3,I4', 4",
            "'T0,J3,L5,Z1,Q8,I0,I6,S4,T2', 3"
    })
    void testSequences(String input, int expectedHeight) {
        assertEquals(expectedHeight, run(input));
    }

    // ---------- Regular edge condition tests ----------

    @Test
    void testIndependentGames() {
        // Each input line must start with a fresh grid
        int h1 = run("Q0,Q2");
        int h2 = run("I0");
        assertEquals(2, h1);
        assertEquals(1, h2);
    }

    @Test
    void testInvalidPieceThrows() {
        assertThrows(IllegalArgumentException.class, () -> run("X0"));
    }

    @Test
    void testOutOfBoundsThrows() {
        // J is width 2, so J9 is invalid
        assertThrows(IllegalArgumentException.class, () -> run("J9"));
    }
}

