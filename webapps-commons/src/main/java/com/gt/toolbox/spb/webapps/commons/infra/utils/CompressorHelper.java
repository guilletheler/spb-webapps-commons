package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;
import org.xerial.snappy.Snappy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * A comprehensive utility class for object serialization, compression, and encoding.
 * <p>
 * This helper provides a two-way conversion pipeline:
 * 
 * <pre>
 *   Object &lt;--&gt; JSON &lt;--&gt; Compression &lt;--&gt; Base64 String
 * </pre>
 * 
 * It is designed to transform complex Java objects into a compact, URL-safe, and text-based format,
 * which is ideal for storage in databases, caches, or for transmission over networks.
 * <p>
 * Key features include:
 * <ul>
 * <li><b>Object-to-String Conversion:</b> High-level methods ({@code toCompressedBase64}) handle
 * the entire serialization, compression, and encoding process.</li>
 * <li><b>String-to-Object Conversion:</b> Corresponding methods ({@code fromCompressedBase64})
 * reverse the process to restore the original object.</li>
 * <li><b>Configurable Compression:</b> Supports multiple compression algorithms via the
 * {@link CompressionType} enum (GZIP, Snappy, LZMA), with Snappy as the default for a balance of
 * speed and compression ratio.</li>
 * <li><b>Low-Level Stream Helpers:</b> Provides direct access to compression and decompression
 * logic for byte arrays and streams ({@code InputStream}, {@code OutputStream}), allowing for
 * efficient handling of large data.</li>
 * </ul>
 * <p>
 * This is a non-instantiable utility class with static methods.
 *
 * @see CompressionType
 */
public final class CompressorHelper {

    public enum CompressionType {
        GZIP, SNAPPY, LZMA
    }

    // ObjectMapper is thread-safe. It's configured to handle Java 8 time types.
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CompressorHelper() {}

    /**
     * Serializes an object to JSON, compresses it using the default Snappy algorithm, and encodes
     * it as a Base64 string.
     *
     * @param object The object to convert. Can be null.
     * @param <T> The type of the object.
     * @return A Base64 encoded string representing the compressed JSON of the object, or null if
     *         the input was null. @ if any error occurs during serialization or compression.
     */
    public static <T> String toCompressedBase64(T object) {
        return toCompressedBase64(object, CompressionType.SNAPPY);
    }

    /**
     * Decodes a Base64 string, decompresses it using the default Snappy algorithm, and deserializes
     * it from JSON to an object.
     *
     * @param base64String The Base64 encoded string. Can be null or empty.
     * @param targetType The class of the target object.
     * @param <T> The type of the target object.
     * @return The deserialized object, or null if the input string was null or empty. @ if the
     *         string is malformed or any error occurs during decoding, decompression, or
     *         deserialization.
     */
    public static <T> T fromCompressedBase64(String base64String, Class<T> targetType) {
        return fromCompressedBase64(base64String, targetType, CompressionType.SNAPPY);
    }

    /**
     * Serializes an object to JSON, compresses it with the specified algorithm, and encodes it as a
     * Base64 string.
     *
     * @param object The object to convert. Can be null.
     * @param compressionType The compression algorithm to use.
     * @param <T> The type of the object.
     * @return A Base64 encoded string representing the compressed JSON of the object, or null if
     *         the input was null. @ if any error occurs during serialization or compression.
     */
    public static <T> String toCompressedBase64(T object, CompressionType compressionType) {
        if (object == null) {
            return null;
        }

        byte[] compressedBytes = toCompressed(object, compressionType);

        // 3. Encode the compressed bytes to a Base64 string
        return Base64.getEncoder().encodeToString(compressedBytes);

    }

    public static <T> byte[] toCompressed(T object, CompressionType compressionType) {
        try {
            // 1. Serialize Object to JSON string
            byte[] jsonBytes = objectMapper.writeValueAsBytes(object);

            // 2. Compress the JSON bytes using the selected algorithm
            byte[] compressedBytes = compress(jsonBytes, compressionType);
            return compressedBytes;
        } catch (IOException e) {
            // This catches exceptions from ObjectMapper and Snappy.
            throw new CompressionException("Failed to convert object to compressed byte[]",
                    e);
        }
    }

    /**
     * Decodes a Base64 string, decompresses it with the specified algorithm, and deserializes it
     * from JSON to an object.
     *
     * @param base64String The Base64 encoded string. Can be null or empty.
     * @param targetType The class of the target object.
     * @param compressionType The compression algorithm to use for decompression.
     * @param <T> The type of the target object.
     * @return The deserialized object, or null if the input string was null or empty. @ if the
     *         string is malformed or any error occurs during decoding, decompression, or
     *         deserialization.
     */
    public static <T> T fromCompressedBase64(String base64String, Class<T> targetType,
            CompressionType compressionType) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }

        try {
            // 1. Decode Base64 string to compressed bytes
            byte[] compressedBytes = Base64.getDecoder().decode(base64String);

            return fromCompressed(compressedBytes, targetType, compressionType);

        } catch (IllegalArgumentException e) {
            throw new CompressionException("Failed to convert compressed Base64 string to object",
                    e);
        }
    }

    public static <T> T fromCompressed(byte[] compressedBytes, Class<T> targetType) {
        return fromCompressed(compressedBytes, targetType);
    }

    public static <T> T fromCompressed(byte[] compressedBytes, Class<T> targetType,
            CompressionType compressionType) {
        try {
            // 2. Decompress the bytes using the selected algorithm
            byte[] decompressedBytes = decompress(compressedBytes, compressionType);

            // 3. Deserialize JSON string back to an object
            return objectMapper.readValue(decompressedBytes, targetType);
        } catch (IOException | IllegalArgumentException e) {
            throw new CompressionException("Failed to convert compressed Base64 string to object",
                    e);
        }
    }

    public static byte[] compress(byte[] data, CompressionType compressionType) throws IOException {
        return switch (compressionType) {
            case GZIP -> compressGzip(data);
            case SNAPPY -> Snappy.compress(data);
            case LZMA -> compressLzma(data);
        };
    }

    public static byte[] decompress(byte[] compressedBytes, CompressionType compressionType)
            throws IOException {
        return switch (compressionType) {
            case GZIP -> decompressGzip(compressedBytes);
            case SNAPPY -> Snappy.uncompress(compressedBytes);
            case LZMA -> decompressLzma(compressedBytes);
        };
    }

    public static byte[] compressGzip(byte[] data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        compressGzip(data, byteStream);
        return byteStream.toByteArray();
    }

    public static void compressGzip(byte[] data, OutputStream outputStream) throws IOException {
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(outputStream)) {
            gzipStream.write(data);
        }
    }

    public static byte[] decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
        return decompressGzip(byteStream);
    }

    public static byte[] decompressGzip(InputStream byteStream) throws IOException {
        try (GZIPInputStream gzipStream = new GZIPInputStream(byteStream)) {
            return gzipStream.readAllBytes();
        }
    }

    public static byte[] compressLzma(byte[] data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        compressLzma(data, byteStream);
        return byteStream.toByteArray();
    }

    public static void compressLzma(byte[] data, OutputStream byteStream) throws IOException {
        try (XZOutputStream xzStream = new XZOutputStream(byteStream, new LZMA2Options())) {
            xzStream.write(data);
        }
    }

    public static byte[] decompressLzma(byte[] compressedData) throws IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
        return decompressLzma(byteStream);
    }

    public static byte[] decompressLzma(InputStream byteStream) throws IOException {
        try (XZInputStream xzStream = new XZInputStream(byteStream)) {
            return xzStream.readAllBytes();
        }
    }

    /**
     * Custom runtime exception for wrapping errors that occur during the conversion process.
     */
    public static class CompressionException extends RuntimeException {
        public CompressionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
