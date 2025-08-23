package org.drw.standard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger = LogManager.getLogger(TetrisGameFileTest.class);

    @Test
    void processInputFileAndWriteOutput() {
        String inputResourcePath = "/input.txt";
        String outputFilePath = "src/test/resources/output.txt";

        URL resource = TetrisGameFileTest.class.getResource(inputResourcePath);
        assertNotNull(resource, "Test input file not found: " + inputResourcePath + ". Please ensure it's in src/test/resources.");

        try (InputStream inputStream = TetrisGameFileTest.class.getResourceAsStream(inputResourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {

            logger.info("Processing test input file...");
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("Input line: {}", line);
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
                logger.debug("Calculated height: {}", height);
                writer.write(String.valueOf(height));
                writer.newLine();
            }
            logger.info("Finished processing test input file.");
        } catch (IOException | NullPointerException e) {
            fail("Error processing test file: " + e.getMessage(), e);
        }
    }
}