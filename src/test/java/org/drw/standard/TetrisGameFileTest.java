package org.drw.standard;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class TetrisGameFileTest {

    @Test
    void processInputFileAndWriteOutput() {
        String inputResourcePath = "/input.txt";
        String outputFilePath = "src/test/resources/output.txt";

        URL resource = TetrisGameFileTest.class.getResource(inputResourcePath);
        assertNotNull(resource, "Test input file not found: " + inputResourcePath + ". Please ensure it's in src/test/resources.");

        try (InputStream inputStream = TetrisGameFileTest.class.getResourceAsStream(inputResourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Input: " + line);
                System.out.println("--------------------");
                line = line.trim();
                int height;
                if (line.isEmpty()) {
                    height = 0;
                } else {
                    List<Drop> seq = Parser.parseLine(line);
                    TetrisGame game = new TetrisGame();
                    for (Drop d : seq) {
                        game.drop(d.piece(), d.x());
                    }
                    height = game.getHeight();
                }
                System.out.println("Output: " + height);
                System.out.println("----------------------------------------");
                writer.write(String.valueOf(height));
                writer.newLine();
            }
        } catch (IOException | NullPointerException e) {
            fail("Error processing test file: " + e.getMessage(), e);
        }
    }
}