package vlt;

import java.nio.file.Path;

public record LiteralEntry(
    LiteralType type,
    Object value,        // Integer, Float, Double, Long, or Boolean
    Path filePath,
    int fileLocalOrder   // discovery order within file per type, for stable pre-shuffle ordering
) {}
