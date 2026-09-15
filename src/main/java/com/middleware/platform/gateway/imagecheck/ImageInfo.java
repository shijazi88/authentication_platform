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
        /** Set when the container is recognised but the content is corrupt / undecodable. */
        String decodeError
) {
    public static ImageInfo unreadable(String error) {
        return new ImageInfo(ImageFormat.UNKNOWN, 0, null, null, null, null, null, null, null, error);
    }
}
