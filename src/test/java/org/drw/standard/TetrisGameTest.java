package org.drw.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for TetrisGame functionality.
 * Tests cover standard gameplay, edge cases, error conditions, and performance scenarios.
 *
 * @author George Bo Zhang
 */
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

    /** Helper to run a single input line through a game with resettle enabled. */
    private int runWithResettle(String line) {
        List<Drop> seq = Parser.parseLine(line);
        TetrisGame game = new TetrisGame(true);
        for (Drop d : seq) {
            game.drop(d.piece(), d.x());
        }
        return game.getHeight();
    }

    // ---------- Parameterized tests for standard cases ----------

    @ParameterizedTest(name = "Sequence {0} should result in height {1}")
    @CsvSource({
            // Single pieces - basic placement
            "I0, 1",
            "I6, 1",
            "Q0, 2",
            "O4, 2",
            "T0, 2",
            "T7, 2",
            "Z0, 2",
            "S0, 2",
            "L0, 3",
            "J0, 3",

            // Edge positions
            "I6, 1",     // I-piece at rightmost position
            "Q8, 2",     // Q-piece at rightmost position
            "T7, 2",     // T-piece at rightmost position

            // Complex sequences
            "'T1,Z3,I4', 4",
            "'I0,I4,Q8', 1",
            "'Q0,Q2,Q4,Q6,Q8', 0",

            // Line clearing scenarios (without resettle)
            "'Q0,I2,I6,I0,I6,I6,Q2,Q4', 3",
            "'T0,J3,L5,Z1,Q8,I0,I6,S4,T2', 7",

            // Stacking scenarios
            "'Q0,Q0', 4",      // Q on top of Q
            "'I0,I0', 2",      // I on top of I
            "'T0,T0', 4",      // T on top of T
    })
    @DisplayName("Standard gameplay sequences")
    void testSequences(String input, int expectedHeight) {
        assertEquals(expectedHeight, run(input));
    }

    // ---------- Resettle mode tests ----------

    @Nested
    @DisplayName("Resettle Mode Tests")
    class ResettleModeTests {

        @Test
        @DisplayName("Simple resettle scenario")
        void testBasicResettle() {
            // This sequence should create floating blocks that resettle
            String sequence = "Q0,I2,I6,I0,I6,I6,Q2,Q4";
            int normalHeight = run(sequence);
            int resettleHeight = runWithResettle(sequence);

            assertEquals(3, normalHeight, "Normal mode should leave floating blocks");
            assertEquals(1, resettleHeight, "Resettle mode should settle floating blocks");
        }

        @Test
        @DisplayName("Multiple islands resettle")
        void testMultipleIslands() {
            TetrisGame game = new TetrisGame(true);

            // Create a scenario with multiple floating islands
            game.drop(Piece.Q, 0);  // Bottom left
            game.drop(Piece.Q, 8);  // Bottom right
            game.drop(Piece.I, 2);  // Bridge in middle
            game.drop(Piece.Q, 3);  // Island above bridge left
            game.drop(Piece.Q, 5);  // Island above bridge right

            // The I-piece should clear, leaving two Q islands to resettle
            assertTrue(game.getHeight() <= 4, "Islands should resettle to reasonable height");
        }

        @Test
        @DisplayName("Resettle mode flag")
        void testResettleModeFlag() {
            TetrisGame normalGame = new TetrisGame();
            TetrisGame resettleGame = new TetrisGame(true);

            assertFalse(normalGame.isResettleEnabled());
            assertTrue(resettleGame.isResettleEnabled());
        }
    }

    // ---------- Edge cases and error conditions ----------

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Independent games maintain separate state")
        void testIndependentGames() {
            int h1 = run("Q0,Q2");
            int h2 = run("I0");
            assertEquals(2, h1);
            assertEquals(1, h2);
        }

        @Test
        @DisplayName("Invalid piece letters throw exceptions")
        void testInvalidPieceThrows() {
            assertThrows(IllegalArgumentException.class, () -> run("X0"));
            assertThrows(IllegalArgumentException.class, () -> run("A5"));
            assertThrows(IllegalArgumentException.class, () -> run("B2"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"J9", "I7", "Q9", "T8", "Z8", "S8", "L9"})
        @DisplayName("Out of bounds positions throw exceptions")
        void testOutOfBoundsThrows(String invalidDrop) {
            assertThrows(IllegalArgumentException.class, () -> run(invalidDrop));
        }

        @Test
        @DisplayName("Null piece handling")
        void testNullPieceHandling() {
            TetrisGame game = new TetrisGame();
            assertThrows(IllegalArgumentException.class, () -> game.drop(null, 0));
        }

        @Test
        @DisplayName("Negative positions")
        void testNegativePositions() {
            TetrisGame game = new TetrisGame();
            assertThrows(IllegalArgumentException.class, () -> game.drop(Piece.I, -1));
        }

        @Test
        @DisplayName("Grid height limit protection")
        void testGridHeightLimit() {
            TetrisGame game = new TetrisGame();

            // This should not throw an exception for reasonable heights
            for (int i = 0; i < 100; i++) {
                game.drop(Piece.Q, 0);
            }

            assertTrue(game.getHeight() <= 1000, "Height should be within reasonable limits");
        }
    }

    // ---------- Specific piece behavior tests ----------

    @Nested
    @DisplayName("Individual Piece Tests")
    class IndividualPieceTests {

        private TetrisGame game;

        @BeforeEach
        void setUp() {
            game = new TetrisGame();
        }

        @Test
        @DisplayName("I-piece horizontal placement")
        void testIPiece() {
            game.drop(Piece.I, 0);
            assertEquals(1, game.getHeight());

            game.drop(Piece.I, 4);
            assertEquals(1, game.getHeight()); // Should still be height 1

            game.drop(Piece.I, 6);
            assertEquals(2, game.getHeight()); // Should still be height 1
        }

        @Test
        @DisplayName("Q-piece square placement")
        void testQPiece() {
            game.drop(Piece.Q, 0);
            assertEquals(2, game.getHeight());

            game.drop(Piece.Q, 2);
            assertEquals(2, game.getHeight());

            game.drop(Piece.Q, 0); // Stack on first Q
            assertEquals(4, game.getHeight());
        }

        @Test
        @DisplayName("T-piece placement and stacking")
        void testTPiece() {
            game.drop(Piece.T, 0);
            assertEquals(2, game.getHeight());

            // T-piece should stack properly
            game.drop(Piece.T, 3);
            assertEquals(2, game.getHeight());
        }

        @Test
        @DisplayName("L and J piece placement")
        void testLAndJPieces() {
            game.drop(Piece.L, 0);
            assertEquals(3, game.getHeight());

            game.drop(Piece.J, 2);
            assertEquals(3, game.getHeight());
        }

        @Test
        @DisplayName("S and Z piece placement")
        void testSAndZPieces() {
            game.drop(Piece.S, 0);
            assertEquals(2, game.getHeight());

            game.drop(Piece.Z, 3);
            assertEquals(2, game.getHeight());
        }
    }

    // ---------- Performance and stress tests ----------

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Large sequence performance")
        void testLargeSequencePerformance() {
            StringBuilder largeSequence = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                if (i > 0) largeSequence.append(",");
                largeSequence.append("Q").append(i % 9); // Alternate positions
            }

            long startTime = System.currentTimeMillis();
            int height = run(largeSequence.toString());
            long endTime = System.currentTimeMillis();

            assertTrue(height > 0, "Should produce valid height");
            assertTrue(endTime - startTime < 5000, "Should complete within 5 seconds");
        }

        @Test
        @DisplayName("Memory efficiency with tall stacks")
        void testMemoryEfficiency() {
            TetrisGame game = new TetrisGame();

            // Create a tall stack
            for (int i = 0; i < 500; i++) {
                game.drop(Piece.Q, 4); // Stack in the middle
            }

            assertTrue(game.getHeight() > 900, "Should create a very tall stack");

            // Grid state should be accessible
            List<Integer> gridState = game.getGridState();
            assertNotNull(gridState);
            assertEquals(game.getHeight(), gridState.size());
        }
    }

    // ---------- Grid state and utility tests ----------

    @Nested
    @DisplayName("Utility and State Tests")
    class UtilityTests {

        @Test
        @DisplayName("Empty game state")
        void testEmptyGame() {
            TetrisGame game = new TetrisGame();
            assertEquals(0, game.getHeight());
            assertEquals("(empty)", game.toString());
            assertTrue(game.getGridState().isEmpty());
        }

        @Test
        @DisplayName("toString formatting")
        void testToStringFormatting() {
            TetrisGame game = new TetrisGame();
            game.drop(Piece.I, 0);

            String str = game.toString();
            assertNotNull(str);
            assertTrue(str.contains("#"), "Should contain filled blocks");
            assertTrue(str.contains("."), "Should contain empty spaces");
            assertTrue(str.contains("0:"), "Should contain row numbers");
        }

        @Test
        @DisplayName("Grid state immutability")
        void testGridStateImmutability() {
            TetrisGame game = new TetrisGame();
            game.drop(Piece.Q, 0);

            List<Integer> gridState = game.getGridState();
            assertThrows(UnsupportedOperationException.class, () -> gridState.clear());
            assertThrows(UnsupportedOperationException.class, () -> gridState.add(0));
        }
    }

    // ---------- Line clearing mechanics ----------

    @Nested
    @DisplayName("Line Clearing Tests")
    class LineClearingTests {

        @Test
        @DisplayName("Single line clear")
        void testSingleLineClear() {
            TetrisGame game = new TetrisGame();

            // Fill a complete line except for 4 spaces in the middle
            game.drop(Piece.I, 0);    // Positions 0,1,2,3
            game.drop(Piece.I, 6);    // Positions 6,7,8,9
            game.drop(Piece.Q, 4);    // Fill positions 4,5 (and row above)

            // The bottom row should be cleared
            assertEquals(1, game.getHeight()); // Only the Q's top row remains
        }

        @Test
        @DisplayName("Multiple line clear")
        void testMultipleLineClear() {
            TetrisGame game = new TetrisGame();

            // Create a scenario where multiple lines can be cleared at once
            for (int x = 0; x < 5; x++) {
                game.drop(Piece.Q, x * 2); // Fill two rows completely
            }

            // Both rows should be cleared
            assertEquals(0, game.getHeight());
        }

        @Test
        @DisplayName("Partial line no clear")
        void testPartialLineNoClear() {
            TetrisGame game = new TetrisGame();

            // Create an incomplete line
            game.drop(Piece.I, 0);    // Positions 0,1,2,3
            game.drop(Piece.I, 5);    // Positions 5,6,7,8
            // Position 4 and 9 are empty

            assertEquals(1, game.getHeight()); // Line should not clear
        }
    }
}
