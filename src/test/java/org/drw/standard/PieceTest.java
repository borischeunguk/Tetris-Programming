package org.drw.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Piece enum functionality.
 * Tests piece properties, bitmask generation, and factory methods.
 *
 * @author George Bo Zhang
 */
public class PieceTest {

    @Nested
    @DisplayName("Piece Properties")
    class PiecePropertiesTests {

        @ParameterizedTest
        @CsvSource({
            "I, 4, 1",  // Horizontal line
            "Q, 2, 2",  // Square
            "T, 3, 2",  // T-shape
            "Z, 3, 2",  // Z-shape
            "S, 3, 2",  // S-shape
            "L, 2, 3",  // L-shape
            "J, 2, 3"   // J-shape
        })
        @DisplayName("Piece dimensions")
        void testPieceDimensions(Piece piece, int expectedWidth, int expectedHeight) {
            assertEquals(expectedWidth, piece.getWidth());
            assertEquals(expectedHeight, piece.getHeight());
        }

        @ParameterizedTest
        @EnumSource(Piece.class)
        @DisplayName("All pieces have 4 blocks")
        void testAllPiecesHave4Blocks(Piece piece) {
            int[][] shape = piece.getShape();
            assertEquals(4, shape.length, "All tetrominoes should have exactly 4 blocks");

            for (int[] coord : shape) {
                assertEquals(2, coord.length, "Each coordinate should be [x, y]");
                assertTrue(coord[0] >= 0, "X coordinate should be non-negative");
                assertTrue(coord[1] >= 0, "Y coordinate should be non-negative");
            }
        }

        @Test
        @DisplayName("Shape defensive copying")
        void testShapeDefensiveCopying() {
            Piece piece = Piece.I;
            int[][] shape1 = piece.getShape();
            int[][] shape2 = piece.getShape();

            // Should be different array instances
            assertNotSame(shape1, shape2);

            // But same content
            assertArrayEquals(shape1, shape2);

            // Modifying returned array shouldn't affect piece
            shape1[0][0] = 999;
            int[][] shape3 = piece.getShape();
            assertNotEquals(999, shape3[0][0]);
        }
    }

    @Nested
    @DisplayName("Bitmask Generation")
    class BitmaskGenerationTests {

        @Test
        @DisplayName("I-piece bitmask generation")
        void testIPieceBitmask() {
            Map<Integer, Integer> masks = Piece.I.buildRowMasks(0);
            assertEquals(1, masks.size(), "I-piece should occupy 1 row");
            assertTrue(masks.containsKey(0), "Should occupy row 0");

            // I-piece at x=0 should occupy bits 0,1,2,3
            int expectedMask = (1 << 0) | (1 << 1) | (1 << 2) | (1 << 3);
            assertEquals(expectedMask, masks.get(0));
        }

        @Test
        @DisplayName("Q-piece bitmask generation")
        void testQPieceBitmask() {
            Map<Integer, Integer> masks = Piece.Q.buildRowMasks(0);
            assertEquals(2, masks.size(), "Q-piece should occupy 2 rows");
            assertTrue(masks.containsKey(0), "Should occupy row 0");
            assertTrue(masks.containsKey(1), "Should occupy row 1");

            // Q-piece at x=0 should occupy bits 0,1 in both rows
            int expectedMask = (1 << 0) | (1 << 1);
            assertEquals(expectedMask, masks.get(0));
            assertEquals(expectedMask, masks.get(1));
        }

        @Test
        @DisplayName("T-piece bitmask generation")
        void testTPieceBitmask() {
            Map<Integer, Integer> masks = Piece.T.buildRowMasks(1);
            assertEquals(2, masks.size(), "T-piece should occupy 2 rows");

            // T-piece at x=1: bottom row has bits 1,2,3; top row has bit 2
            int bottomMask = (1 << 1) | (1 << 2) | (1 << 3);
            int topMask = (1 << 2);
            assertEquals(topMask, masks.get(0));
            assertEquals(bottomMask, masks.get(1));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
        @DisplayName("Bitmask generation at different positions")
        void testBitmaskAtDifferentPositions(int x) {
            for (Piece piece : Piece.values()) {
                if (x + piece.getWidth() <= 10) { // Valid position
                    assertDoesNotThrow(() -> piece.buildRowMasks(x));
                    Map<Integer, Integer> masks = piece.buildRowMasks(x);
                    assertFalse(masks.isEmpty(), "Should generate at least one row mask");
                }
            }
        }

        @Test
        @DisplayName("Bitmask validation for out-of-bounds")
        void testBitmaskOutOfBounds() {
            assertThrows(IllegalArgumentException.class, () -> Piece.I.buildRowMasks(7)); // 7+4 > 10
            assertThrows(IllegalArgumentException.class, () -> Piece.Q.buildRowMasks(9)); // 9+2 > 10
            assertThrows(IllegalArgumentException.class, () -> Piece.T.buildRowMasks(8)); // 8+3 > 10
        }

        @Test
        @DisplayName("Negative position validation")
        void testNegativePosition() {
            for (Piece piece : Piece.values()) {
                assertThrows(IllegalArgumentException.class, () -> piece.buildRowMasks(-1));
            }
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @ParameterizedTest
        @CsvSource({
            "I, I",
            "i, I",
            "Q, Q",
            "q, Q",
            "O, Q",  // Alias
            "o, Q",  // Alias lowercase
            "T, T",
            "Z, Z",
            "S, S",
            "L, L",
            "J, J"
        })
        @DisplayName("Valid piece letter conversion")
        void testValidPieceLetters(String input, Piece expected) {
            assertEquals(expected, Piece.fromLetter(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"X", "A", "B", "C", "D", "E", "F", "G", "H", "K", "M", "N", "P", "R", "U", "V", "W", "Y"})
        @DisplayName("Invalid piece letters")
        void testInvalidPieceLetters(String input) {
            assertThrows(IllegalArgumentException.class, () -> Piece.fromLetter(input));
        }

        @Test
        @DisplayName("Null and empty input handling")
        void testNullAndEmptyInput() {
            assertThrows(NullPointerException.class, () -> Piece.fromLetter(null));
            assertThrows(IllegalArgumentException.class, () -> Piece.fromLetter(""));
            assertThrows(IllegalArgumentException.class, () -> Piece.fromLetter("   "));
        }

        @Test
        @DisplayName("Multi-character input handling")
        void testMultiCharacterInput() {
            assertThrows(IllegalArgumentException.class, () -> Piece.fromLetter("II"));
            assertThrows(IllegalArgumentException.class, () -> Piece.fromLetter("QQ"));
            assertThrows(IllegalArgumentException.class, () -> Piece.fromLetter("ABC"));
        }

        @Test
        @DisplayName("Whitespace handling in fromLetter")
        void testWhitespaceHandling() {
            assertEquals(Piece.I, Piece.fromLetter(" I "));
            assertEquals(Piece.Q, Piece.fromLetter(" O "));
            assertEquals(Piece.T, Piece.fromLetter("\tT\n"));
        }

        @Test
        @DisplayName("Error messages are helpful")
        void testErrorMessages() {
            try {
                Piece.fromLetter("X");
                fail("Should have thrown exception");
            } catch (IllegalArgumentException e) {
                String msg = e.getMessage();
                assertTrue(msg.contains("X"), "Should mention the invalid letter");
                assertTrue(msg.contains("Valid pieces"), "Should list valid pieces");
            }
        }

        @Test
        @DisplayName("getValidLetters method")
        void testGetValidLetters() {
            Set<String> validLetters = Piece.getValidLetters();

            // Should contain all standard piece letters
            assertTrue(validLetters.contains("I"));
            assertTrue(validLetters.contains("J"));
            assertTrue(validLetters.contains("L"));
            assertTrue(validLetters.contains("O"));
            assertTrue(validLetters.contains("Q"));
            assertTrue(validLetters.contains("S"));
            assertTrue(validLetters.contains("T"));
            assertTrue(validLetters.contains("Z"));

            assertEquals(8, validLetters.size(), "Should have exactly 8 valid letters");

            // Should be immutable
            assertThrows(UnsupportedOperationException.class, () -> validLetters.add("X"));
        }
    }

    @Nested
    @DisplayName("Specific Piece Shapes")
    class SpecificPieceShapeTests {

        @Test
        @DisplayName("I-piece shape verification")
        void testIPieceShape() {
            int[][] shape = Piece.I.getShape();
            int[][] expected = {{0,0},{1,0},{2,0},{3,0}};
            assertArrayEquals(expected, shape);
        }

        @Test
        @DisplayName("Q-piece shape verification")
        void testQPieceShape() {
            int[][] shape = Piece.Q.getShape();
            int[][] expected = {{0,0},{1,0},{0,1},{1,1}};
            assertArrayEquals(expected, shape);
        }

        @Test
        @DisplayName("T-piece shape verification")
        void testTPieceShape() {
            int[][] shape = Piece.T.getShape();
            int[][] expected = {{0,1},{1,1},{2,1},{1,0}};
            assertArrayEquals(expected, shape);
        }

        @Test
        @DisplayName("Z-piece shape verification")
        void testZPieceShape() {
            int[][] shape = Piece.Z.getShape();
            int[][] expected = {{1,0},{2,0},{0,1},{1,1}};
            assertArrayEquals(expected, shape);
        }

        @Test
        @DisplayName("S-piece shape verification")
        void testSPieceShape() {
            int[][] shape = Piece.S.getShape();
            int[][] expected = {{0,0},{1,0},{1,1},{2,1}};
            assertArrayEquals(expected, shape);
        }

        @Test
        @DisplayName("L-piece shape verification")
        void testLPieceShape() {
            int[][] shape = Piece.L.getShape();
            int[][] expected = {{0,0},{0,1},{0,2},{1,0}};
            assertArrayEquals(expected, shape);
        }

        @Test
        @DisplayName("J-piece shape verification")
        void testJPieceShape() {
            int[][] shape = Piece.J.getShape();
            int[][] expected = {{1,0},{1,1},{1,2},{0,0}};
            assertArrayEquals(expected, shape);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Bitmask generation performance")
        void testBitmaskGenerationPerformance() {
            long startTime = System.currentTimeMillis();

            // Generate bitmasks many times
            for (int i = 0; i < 100000; i++) {
                for (Piece piece : Piece.values()) {
                    for (int x = 0; x <= 10 - piece.getWidth(); x++) {
                        piece.buildRowMasks(x);
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            assertTrue(endTime - startTime < 2000, "Bitmask generation should be fast");
        }

        @Test
        @DisplayName("fromLetter performance")
        void testFromLetterPerformance() {
            String[] letters = {"I", "J", "L", "O", "Q", "S", "T", "Z"};

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 100000; i++) {
                for (String letter : letters) {
                    Piece.fromLetter(letter);
                }
            }

            long endTime = System.currentTimeMillis();
            assertTrue(endTime - startTime < 1000, "fromLetter should be fast");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Maximum valid positions")
        void testMaximumValidPositions() {
            assertEquals(6, 10 - Piece.I.getWidth(), "I-piece max position should be 6");
            assertEquals(8, 10 - Piece.Q.getWidth(), "Q-piece max position should be 8");
            assertEquals(7, 10 - Piece.T.getWidth(), "T-piece max position should be 7");
            assertEquals(7, 10 - Piece.Z.getWidth(), "Z-piece max position should be 7");
            assertEquals(7, 10 - Piece.S.getWidth(), "S-piece max position should be 7");
            assertEquals(8, 10 - Piece.L.getWidth(), "L-piece max position should be 8");
            assertEquals(8, 10 - Piece.J.getWidth(), "J-piece max position should be 8");
        }

        @Test
        @DisplayName("All pieces fit at position 0")
        void testAllPiecesFitAtPosition0() {
            for (Piece piece : Piece.values()) {
                assertDoesNotThrow(() -> piece.buildRowMasks(0));
            }
        }

        @Test
        @DisplayName("Bitmask uniqueness per position")
        void testBitmaskUniquenessPerPosition() {
            for (Piece piece : Piece.values()) {
                for (int x1 = 0; x1 <= 10 - piece.getWidth(); x1++) {
                    for (int x2 = x1 + 1; x2 <= 10 - piece.getWidth(); x2++) {
                        Map<Integer, Integer> masks1 = piece.buildRowMasks(x1);
                        Map<Integer, Integer> masks2 = piece.buildRowMasks(x2);

                        assertNotEquals(masks1, masks2,
                            String.format("Piece %s should have different masks at x=%d vs x=%d", piece, x1, x2));
                    }
                }
            }
        }
    }
}
