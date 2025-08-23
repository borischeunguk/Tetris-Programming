package org.drw.standard;

/**
 * Represents a single piece being dropped at a specific horizontal position.
 * This is an immutable data carrier class.
 *
 * @param piece The type of Tetris piece being dropped.
 * @param x     The 0-indexed horizontal starting column for the drop.
 */
public record Drop(Piece piece, int x) { }