package com.middleware.platform.gateway.imagecheck;

/**
 * What {@link ImageInspector} could read from a fingerprint image. Nullable
 * fields are "not determinable" (e.g. ppi when the file declares none, pixel
 * statistics for WSQ which is not decoded).
 */
public record ImageInfo(
        ImageFormat format,
        int bytes,
        Integer width,
        Integer height,
        Integer bitDepth,
        Boolean grayscale,
        Integer ppi,
        Double compressionRatio,
        Double stdDev,
        /** Fraction of 16×16 blocks with ridge texture (std dev > 20); PNG only. */
        Double foregroundRatio,
        /** PNG colour type (0 grey, 2 RGB, 3 palette, 4 grey+alpha, 6 RGBA); null for WSQ. */
        Integer colorType,
        /** Set when the container is recognised but the content is corrupt / undecodable. */
        String decodeError
) {
    public static ImageInfo unreadable(String error) {
        return new ImageInfo(ImageFormat.UNKNOWN, 0, null, null, null, null, null, null, null, null, null, error);
    }

    public String colorTypeName() {
        if (colorType == null) return "greyscale";
        return switch (colorType) {
            case 0 -> "greyscale";
            case 2 -> "RGB colour";
            case 3 -> "indexed/palette";
            case 4 -> "greyscale with alpha";
            case 6 -> "RGBA colour";
            default -> "colour type " + colorType;
        };
    }
}
