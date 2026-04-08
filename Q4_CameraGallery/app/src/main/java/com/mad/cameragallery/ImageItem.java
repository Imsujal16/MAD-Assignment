package com.mad.cameragallery;

import android.net.Uri;

public class ImageItem {
    private final Uri uri;
    private final String fileName;
    private final String displayName;
    private final String folderName;
    private final String locationPath;
    private final String galleryMeta;
    private final long size;
    private final long lastModified;
    private final String dateTaken;

    public ImageItem(
            Uri uri,
            String fileName,
            String displayName,
            String folderName,
            String locationPath,
            String galleryMeta,
            long size,
            long lastModified,
            String dateTaken
    ) {
        this.uri = uri;
        this.fileName = fileName;
        this.displayName = displayName;
        this.folderName = folderName;
        this.locationPath = locationPath;
        this.galleryMeta = galleryMeta;
        this.size = size;
        this.lastModified = lastModified;
        this.dateTaken = dateTaken;
    }

    public Uri getUri() {
        return uri;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getLocationPath() {
        return locationPath;
    }

    public String getGalleryMeta() {
        return galleryMeta;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getDateTaken() {
        return dateTaken;
    }
}
