package com.example.nptudttbdd;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * Unified image loader for:
 * - http(s) url
 * - data URI base64 (data:image/jpeg;base64,...)
 * - raw base64 (fallback)
 * - local file path (legacy)
 */
public final class ImageLoader {

    private ImageLoader() {}

    public static void load(@NonNull ImageView target, String source, @DrawableRes int placeholderRes) {
        if (TextUtils.isEmpty(source)) {
            target.setImageResource(placeholderRes);
            return;
        }

        String s = source.trim();

        // Remote URL
        if (s.startsWith("http://") || s.startsWith("https://")) {
            Glide.with(target)
                    .load(s)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .into(target);
            return;
        }

        // Data URI base64
        if (s.startsWith("data:image")) {
            int comma = s.indexOf(',');
            String base64 = comma >= 0 ? s.substring(comma + 1) : s;
            loadBase64(target, base64, placeholderRes);
            return;
        }

        // Looks like raw base64 (fallback)
        if (looksLikeBase64(s)) {
            loadBase64(target, s, placeholderRes);
            return;
        }

        // Local file path (legacy)
        File f = new File(s);
        if (f.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(s);
            if (bmp != null) {
                target.setImageBitmap(bmp);
                return;
            }
        }

        target.setImageResource(placeholderRes);
    }

    private static void loadBase64(@NonNull ImageView target, @NonNull String base64, @DrawableRes int placeholderRes) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Glide.with(target)
                    .asBitmap()
                    .load(bytes)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .into(target);
        } catch (Exception e) {
            target.setImageResource(placeholderRes);
        }
    }

    private static boolean looksLikeBase64(@NonNull String s) {
        // Heuristic: long enough, only base64 chars, and divisible-ish by 4.
        if (s.length() < 128) return false;
        int valid = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '+' || c == '/' || c == '='
                    || c == '\n' || c == '\r';
            if (ok) valid++;
        }
        return valid >= (int) (s.length() * 0.95);
    }
}
