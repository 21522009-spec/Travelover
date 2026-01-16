package com.example.nptudttbdd;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImagePreviewItem {
    @Nullable
    public final Uri localUri;

    @Nullable
    public final String remoteUrl;

    public ImagePreviewItem(@NonNull Uri localUri) {
        this.localUri = localUri;
        this.remoteUrl = null;
    }

    public ImagePreviewItem(@NonNull String remoteUrl) {
        this.localUri = null;
        this.remoteUrl = remoteUrl;
    }

    public boolean isLocal() {
        return localUri != null;
    }
}
