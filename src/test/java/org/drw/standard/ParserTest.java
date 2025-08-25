package org.drw.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Parser functionality.
 * Tests input parsing, validation, and error handling.
 *
 * @author George Bo Zhang
 */
public class ParserTest {

    @Nested
    @DisplayName("Valid Input Parsing")
    class ValidInputTests {

        @Test
        @DisplayName("Simple single piece parsing")
        void testSimpleParsing() {
            List<Drop> drops = Parser.parseLine("Q0");
            assertEquals(1, drops.size());
            assertEquals(Piece.Q, drops.get(0).piece());
            assertEquals(0, drops.get(0).x());
        }

        @Test
        @DisplayName("Multiple pieces parsing")
        void testMultiplePieces() {
            List<Drop> drops = Parser.parseLine("Q0,I4,T1");
            assertEquals(3, drops.size());

            assertEquals(Piece.Q, drops.get(0).piece());
            assertEquals(0, drops.get(0).x());

            assertEquals(Piece.I, drops.get(1).piece());
            assertEquals(4, drops.get(1).x());

            assertEquals(Piece.T, drops.get(2).piece());
            assertEquals(1, drops.get(2).x());
        }

        @Test
        @DisplayName("O/Q piece alias")
        void testOQAlias() {
            List<Drop> drops1 = Parser.parseLine("O0");
            List<Drop> drops2 = Parser.parseLine("Q0");

            assertEquals(drops1.get(0).piece(), drops2.get(0).piece());
            assertEquals(Piece.Q, drops1.get(0).piece());
        }

        @ParameterizedTest
        @CsvSource({
            "I0, I, 0",
            "I6, I, 6",
            "Q0, Q, 0",
            "Q8, Q, 8",
            "T0, T, 0",
            "T7, T, 7",
            "Z0, Z, 0",
            "S0, S, 0",
            "L0, L, 0",
            "J0, J, 0"
        })
        @DisplayName("All piece types with valid positions")
        void testAllPieces(String input, String expectedPiece, int expectedX) {
            List<Drop> drops = Parser.parseLine(input);
            assertEquals(1, drops.size());
            assertEquals(Piece.valueOf(expectedPiece), drops.get(0).piece());
            assertEquals(expectedX, drops.get(0).x());
        }

        @Test
        @DisplayName("Empty and null input handling")
        void testEmptyInput() {
            assertTrue(Parser.parseLine("").isEmpty());
            assertTrue(Parser.parseLine("   ").isEmpty());
            assertTrue(Parser.parseLine(null).isEmpty());
            assertTrue(Parser.parseLine(",,,").isEmpty());
        }

        @Test
        @DisplayName("Edge positions")
        void testEdgePositions() {
            // Test rightmost valid positions for each piece
            assertDoesNotThrow(() -> Parser.parseLine("I6"));  // Width 4: 6+4=10
            assertDoesNotThrow(() -> Parser.parseLine("Q8"));  // Width 2: 8+2=10
            assertDoesNotThrow(() -> Parser.parseLine("T7"));  // Width 3: 7+3=10
            assertDoesNotThrow(() -> Parser.parseLine("Z7"));  // Width 3: 7+3=10
            assertDoesNotThrow(() -> Parser.parseLine("S7"));  // Width 3: 7+3=10
            assertDoesNotThrow(() -> Parser.parseLine("L8"));  // Width 2: 8+2=10
            assertDoesNotThrow(() -> Parser.parseLine("J8"));  // Width 2: 8+2=10
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @ParameterizedTest
        @ValueSource(strings = {"X0", "A5", "B2", "ABC0", "123"})
        @DisplayName("Invalid piece letters")
        void testInvalidPieceLetters(String input) {
            assertThrows(IllegalArgumentException.class, () -> Parser.parseLine(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"I7", "I8", "I9", "I10", "Q9", "Q10", "T8", "T9", "Z8", "S8", "L9", "J9"})
        @DisplayName("Out of bounds positions")
        void testOutOfBoundsPositions(String input) {
            assertThrows(IllegalArgumentException.class, () -> Parser.parseLine(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"I-1", "Q-5", "T-10"})
        @DisplayName("Negative positions")
        void testNegativePositions(String input) {
            assertThrows(IllegalArgumentException.class, () -> Parser.parseLine(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Q", "I", "T5X", "Q0.5", "I4.2", "QQ0", "5I", "Q 5 0"})
        @DisplayName("Malformed tokens")
        void testMalformedTokens(String input) {
            assertThrows(IllegalArgumentException.class, () -> Parser.parseLine(input));
        }

        @Test
        @DisplayName("Mixed valid and invalid tokens")
        void testMixedTokens() {
            // Should fail on the invalid token
            assertThrows(IllegalArgumentException.class, () -> Parser.parseLine("Q0,X5,I4"));
            assertThrows(IllegalArgumentException.class, () -> Parser.parseLine("Q0,I10"));
        }

        @Test
        @DisplayName("Error messages contain helpful information")
        void testErrorMessages() {
            try {
                Parser.parseLine("X0");
                fail("Should have thrown exception");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("X"), "Error should mention invalid piece");
            }

            try {
                Parser.parseLine("I10");
                fail("Should have thrown exception");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().toLowerCase().contains("exceed grid width"), "Error should mention bounds");
            }
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodTests {

        @Test
        @DisplayName("parseLines method")
        void testParseLines() {
            List<String> lines = List.of("Q0,I4", "T1", "", "Z3,S5");
            List<List<Drop>> result = Parser.parseLines(lines);

            assertEquals(4, result.size());
            assertEquals(2, result.get(0).size()); // Q0,I4
            assertEquals(1, result.get(1).size()); // T1
            assertEquals(0, result.get(2).size()); // empty line
            assertEquals(2, result.get(3).size()); // Z3,S5
        }

        @Test
        @DisplayName("parseLines with null input")
        void testParseLinesNull() {
            assertTrue(Parser.parseLines(null).isEmpty());
        }

        @Test
        @DisplayName("parseLines error propagation")
        void testParseLinesErrorPropagation() {
            List<String> lines = List.of("Q0", "X5", "I4");

            try {
                Parser.parseLines(lines);
                fail("Should have thrown exception");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("line 2"), "Error should indicate line number");
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Q0, true",
            "I6, true",
            "T7, true",
            "O8, true",
            "X0, false",
            "I10, false",
            "Q-1, false",
            "QQ0, false",
            "'', false"
        })
        @DisplayName("isValidTokenFormat method")
        void testIsValidTokenFormat(String token, boolean expected) {
            assertEquals(expected, Parser.isValidTokenFormat(token));
        }

        @Test
        @DisplayName("isValidTokenFormat with null")
        void testIsValidTokenFormatNull() {
            assertFalse(Parser.isValidTokenFormat(null));
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Large input parsing performance")
        void testLargeInputPerformance() {
            StringBuilder largeInput = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                if (i > 0) largeInput.append(",");
                largeInput.append("Q").append(i % 9);
            }

            long startTime = System.currentTimeMillis();
            List<Drop> drops = Parser.parseLine(largeInput.toString());
            long endTime = System.currentTimeMillis();

            assertEquals(10000, drops.size());
            assertTrue(endTime - startTime < 1000, "Parsing should complete within 1 second");
        }

        @Test
        @DisplayName("Many small inputs performance")
        void testManySmallInputsPerformance() {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 10000; i++) {
                List<Drop> drops = Parser.parseLine("Q" + (i % 9));
                assertEquals(1, drops.size());
            }

            long endTime = System.currentTimeMillis();
            assertTrue(endTime - startTime < 1000, "Many small parses should complete within 1 second");
        }
    }
}
