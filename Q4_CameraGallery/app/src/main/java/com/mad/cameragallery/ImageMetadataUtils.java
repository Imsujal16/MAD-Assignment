package com.mad.cameragallery;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.text.format.Formatter;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ImageMetadataUtils {
    private static final String UNKNOWN = "Unknown";
    private static final String INTERNAL_STORAGE = "Internal storage";

    private ImageMetadataUtils() {
    }

    public static String resolveDateTaken(ContentResolver resolver, Uri uri, long lastModified) {
        try (InputStream inputStream = resolver.openInputStream(uri)) {
            if (inputStream != null) {
                ExifInterface exifInterface = new ExifInterface(inputStream);
                String value = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                if (value == null) {
                    value = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                }
                if (value != null) {
                    SimpleDateFormat parser = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
                    Date parsedDate = parser.parse(value);
                    if (parsedDate != null) {
                        return formatDate(parsedDate.getTime());
                    }
                }
            }
        } catch (IOException | ParseException ignored) {
            // Fall back to last modified when EXIF metadata is not available.
        }

        return lastModified > 0 ? formatDate(lastModified) : UNKNOWN;
    }

    public static String formatFileSize(Context context, long size) {
        return size > 0 ? Formatter.formatShortFileSize(context, size) : UNKNOWN;
    }

    public static String formatDate(long timestamp) {
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(timestamp));
    }

    public static String formatCompactDate(long timestamp) {
        return timestamp > 0
                ? new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(timestamp))
                : UNKNOWN;
    }

    public static String resolveReadablePath(Uri uri) {
        String documentId = extractDocumentId(uri);
        if (documentId == null || documentId.isBlank()) {
            String lastSegment = uri != null ? uri.getLastPathSegment() : null;
            return lastSegment != null ? Uri.decode(lastSegment) : UNKNOWN;
        }

        String[] parts = documentId.split(":", 2);
        String volume = parts.length > 1 ? parts[0] : "";
        String relativePath = parts.length > 1 ? parts[1] : parts[0];

        String rootLabel = volume.equalsIgnoreCase("primary")
                ? INTERNAL_STORAGE
                : volume.toUpperCase(Locale.US);

        if (relativePath == null || relativePath.isBlank()) {
            return rootLabel;
        }

        return rootLabel + " / " + relativePath.replace("/", " / ");
    }

    public static String resolveParentPath(Uri uri) {
        return parentPath(resolveReadablePath(uri));
    }

    public static String parentPath(String readablePath) {
        if (readablePath == null || readablePath.isBlank()) {
            return UNKNOWN;
        }

        int lastDivider = readablePath.lastIndexOf(" / ");
        return lastDivider > 0 ? readablePath.substring(0, lastDivider) : readablePath;
    }

    public static String leafName(String readablePath) {
        if (readablePath == null || readablePath.isBlank()) {
            return UNKNOWN;
        }

        int lastDivider = readablePath.lastIndexOf(" / ");
        return lastDivider >= 0 ? readablePath.substring(lastDivider + 3) : readablePath;
    }

    public static String buildFriendlyName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Saved image";
        }

        String baseName = stripExtension(fileName);
        if (baseName.matches("[0-9a-fA-F]{16,}")) {
            return "Photo " + baseName.substring(0, Math.min(8, baseName.length())).toUpperCase(Locale.US);
        }

        if (baseName.matches("IMG_\\d{8}_\\d{6}")) {
            return "Captured photo";
        }

        if (baseName.equalsIgnoreCase(fileName)) {
            return baseName;
        }

        return baseName;
    }

    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private static String extractDocumentId(Uri uri) {
        if (uri == null) {
            return null;
        }

        try {
            return DocumentsContract.getDocumentId(uri);
        } catch (Exception ignored) {
            // Not a document URI, try tree URI next.
        }

        try {
            return DocumentsContract.getTreeDocumentId(uri);
        } catch (Exception ignored) {
            return null;
        }
    }
}
