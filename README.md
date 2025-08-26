# DRW Tetris Programming Exercise

A high-performance Tetris simulation engine that processes sequences of piece drops and calculates the resulting grid height. Built for DRW's technical assessment with focus on correctness, performance, and maintainability.
Source code is available in jar file, and uploaded to github: https://github.com/borischeunguk/Tetris-Programming

## Quick Start

### Running the Application
```bash
# Build the project
./gradlew build

# Run with input file and save output
java -jar build/libs/tetris.jar < src/test/resources/input.txt > output.txt

# Run with resettle mode enabled
java -jar build/libs/tetris.jar --resettle < src/test/resources/input.txt > output.txt

# Run tests
./gradlew test
```

### Input Format
```
Q0,I4,T1    # Sequence: piece_letter + column_number
I0,I4,Q8    # Each line is a separate game
            # Empty lines result in height 0
```

## Architecture

### Core Components
- **TetrisGame**: Main engine with bitmask-based grid representation
- **Piece**: Seven standard tetrominoes (I, J, L, O/Q, S, T, Z) with efficient bitmask generation
- **Parser**: Robust input processing with comprehensive validation
- **Drop**: Immutable command representing piece placement

### Key Features
- **Performance**: O(1) collision detection, 75% memory reduction vs 2D arrays
- **Two Modes**: Standard Tetris + optional floating block resettling
- **Robust**: Comprehensive input validation and error handling
- **Tested**: 190+ tests covering edge cases and performance

## Implementation Highlights

### Strengths
- **Bitmask Operations**: Efficient grid representation and collision detection
- **Memory Efficient**: Lazy grid expansion, configurable height limits
- **Enterprise Ready**: Log4j2 integration, professional error handling
- **Well Tested**: Comprehensive test coverage with performance benchmarks

### Current Limitations
- No piece rotation (by design for problem scope)
- Single-threaded (well-documented threading constraints)
- Resettle algorithm is O(w*h) complexity

## Performance
- **Time Complexity**: O(h) for piece drops, O(1) collision detection
- **Benchmarks**: 10,000+ piece drops in <1 second
- **Memory**: ~75% reduction vs boolean array representation

## Future Improvements

### Short-Term
- Parallel processing for concurrent games
- Piece rotation support
- Visual grid output
- Performance optimizations for line clearing

### Long-Term
- REST API for remote processing
- AI integration for optimal placement
- Multi-player support
- Analytics dashboard

## Development

### Prerequisites
- Java 16+
- Gradle 8.x

### Project Structure
```
src/main/java/org/drw/standard/
├── TetrisGame.java    # Main game engine
├── Piece.java         # Tetromino definitions
├── Parser.java        # Input processing
└── Drop.java          # Piece placement command

build/libs/tetris.jar  # Executable JAR file
```

### Testing
```bash
./gradlew test                    # Run all tests
./gradlew test --info           # Verbose test output
```

## License
Developed for DRW's technical assessment following their coding standards.
