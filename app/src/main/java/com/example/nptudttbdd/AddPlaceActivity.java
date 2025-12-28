package com.example.nptudttbdd;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPlaceActivity extends AppCompatActivity implements ImagePreviewAdapter.Callback {

    private static final int REQ_PICK_IMAGES = 2101;

    // Without Firebase Storage, we embed images as compressed Base64 (data URI) in Realtime Database.
    // Keep the number of images small for performance.
    private static final int MAX_IMAGES = 5;
    private static final int MAX_DIMENSION = 1024;
    private static final int MAX_BYTES_PER_IMAGE = 350 * 1024;

    private ImageView btnBack;
    private EditText edtName;
    private EditText edtLocation;
    private EditText edtPrice;
    private EditText edtDescription;
    private ImageView imgPreview;
    private RecyclerView rvSelectedImages;
    private Button btnChooseImages;
    private Button btnSave;

    private View loadingOverlay;
    private ProgressBar progressBar;

    private final ArrayList<Uri> selectedUris = new ArrayList<>();
    private final ArrayList<ImagePreviewItem> previewItems = new ArrayList<>();
    private ImagePreviewAdapter imageAdapter;

    private DatabaseReference placesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);
        ChatButtonManager.attach(this);

        btnBack = findViewById(R.id.btnBack);
        edtName = findViewById(R.id.edtPlaceName);
        edtLocation = findViewById(R.id.edtPlaceLocation);
        edtPrice = findViewById(R.id.edtPrice);
        edtDescription = findViewById(R.id.edtDescription);
        imgPreview = findViewById(R.id.imgPreview);
        rvSelectedImages = findViewById(R.id.rvSelectedImages);
        btnChooseImages = findViewById(R.id.btnChooseImage);
        btnSave = findViewById(R.id.btnSavePlace);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressBar = findViewById(R.id.progressBar);

        imageAdapter = new ImagePreviewAdapter(this);
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedImages.setAdapter(imageAdapter);

        placesRef = FirebaseDatabase.getInstance().getReference("Places");

        btnBack.setOnClickListener(v -> finish());
        btnChooseImages.setOnClickListener(v -> pickImages());
        btnSave.setOnClickListener(v -> submit());
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnChooseImages.setEnabled(!loading);
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQ_PICK_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_IMAGES && resultCode == RESULT_OK && data != null) {
            selectedUris.clear();

            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    if (selectedUris.size() >= MAX_IMAGES) break;
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) selectedUris.add(uri);
                }
            } else {
                Uri uri = data.getData();
                if (uri != null) selectedUris.add(uri);
            }

            if (selectedUris.isEmpty()) {
                Toast.makeText(this, "Bạn chưa chọn ảnh.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (clipData != null && clipData.getItemCount() > MAX_IMAGES) {
                Toast.makeText(this, "Chỉ hỗ trợ tối đa " + MAX_IMAGES + " ảnh để tránh nặng dữ liệu.", Toast.LENGTH_LONG).show();
            }

            previewItems.clear();
            for (Uri uri : selectedUris) {
                previewItems.add(new ImagePreviewItem(uri));
            }
            imageAdapter.submit(previewItems);

            Glide.with(imgPreview).load(selectedUris.get(0)).centerCrop().into(imgPreview);
        }
    }

    private void submit() {
        String name = safeText(edtName);
        String location = safeText(edtLocation);
        String priceStr = safeText(edtPrice);
        String description = safeText(edtDescription);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, getString(R.string.add_place_error_required), Toast.LENGTH_SHORT).show();
            return;
        }

        long price;
        try {
            price = Long.parseLong(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.add_place_error_price), Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (ownerId.trim().isEmpty()) {
            Toast.makeText(this, "Bạn cần đăng nhập Owner để thêm phòng.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedUris.isEmpty()) {
            Toast.makeText(this, getString(R.string.add_place_error_no_image), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String placeId = placesRef.push().getKey();
        if (placeId == null) {
            Toast.makeText(this, "Không tạo được placeId.", Toast.LENGTH_SHORT).show();
            setLoading(false);
            return;
        }

        // Default amenities: take from repository template (fallback)
        TravelDataRepository repository = TravelDataRepository.getInstance(this);
        Place reference;
        try {
            reference = repository.getPlaceOrThrow("place_giang_dien");
        } catch (Exception ignored) {
            reference = repository.getPlaces().isEmpty() ? null : repository.getPlaces().get(0);
        }
        ArrayList<String> amenities = reference != null
                ? new ArrayList<>(reference.getAmenities())
                : new ArrayList<>();
        if (amenities.isEmpty()) {
            amenities.add("Wifi miễn phí");
        }

        encodeImagesThenSave(placeId, ownerId, name, location, price, description, amenities);
    }

    private void encodeImagesThenSave(@NonNull String placeId,
                                      @NonNull String ownerId,
                                      @NonNull String name,
                                      @NonNull String location,
                                      long price,
                                      @NonNull String description,
                                      @NonNull List<String> amenities) {

        new Thread(() -> {
            try {
                ArrayList<String> dataUris = new ArrayList<>();
                for (Uri uri : selectedUris) {
                    String dataUri = encodeUriToDataUri(uri);
                    dataUris.add(dataUri);
                }

                runOnUiThread(() -> savePlace(placeId, ownerId, name, location, price, description, amenities, dataUris));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Không thể xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
            }
        }).start();
    }

    /**
     * Convert an image uri into a compressed JPEG data URI (base64).
     */
    @NonNull
    private String encodeUriToDataUri(@NonNull Uri uri) throws Exception {
        InputStream input = null;
        try {
            input = getContentResolver().openInputStream(uri);
            if (input == null) throw new Exception("Không mở được ảnh.");

            Bitmap bmp = BitmapFactory.decodeStream(input);
            if (bmp == null) throw new Exception("Ảnh không hợp lệ.");

            bmp = scaleDownIfNeeded(bmp, MAX_DIMENSION);

            byte[] jpegBytes = compressToMaxBytes(bmp, MAX_BYTES_PER_IMAGE);
            String b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);

            return "data:image/jpeg;base64," + b64;
        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception ignored) {
            }
        }
    }

    @NonNull
    private static Bitmap scaleDownIfNeeded(@NonNull Bitmap src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) return src;

        if (w <= maxDim && h <= maxDim) return src;

        float ratio = Math.min(maxDim / (float) w, maxDim / (float) h);
        int nw = Math.max(1, Math.round(w * ratio));
        int nh = Math.max(1, Math.round(h * ratio));
        return Bitmap.createScaledBitmap(src, nw, nh, true);
    }

    @NonNull
    private static byte[] compressToMaxBytes(@NonNull Bitmap bmp, int maxBytes) {
        int quality = 85;
        byte[] data;
        while (true) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            data = baos.toByteArray();
            if (data.length <= maxBytes || quality <= 35) break;
            quality -= 10;
        }
        return data;
    }

    private void savePlace(@NonNull String placeId,
                           @NonNull String ownerId,
                           @NonNull String name,
                           @NonNull String location,
                           long price,
                           @NonNull String description,
                           @NonNull List<String> amenities,
                           @NonNull ArrayList<String> imageUrlsOrDataUris) {

        Map<String, Object> placeData = new HashMap<>();
        placeData.put("id", placeId);
        placeData.put("name", name);
        placeData.put("location", location);
        placeData.put("pricePerNight", price);
        placeData.put("rating", 0);
        placeData.put("ratingCount", 0);
        placeData.put("description", description);
        placeData.put("amenities", amenities);
        placeData.put("ownerId", ownerId);
        placeData.put("active", true);

        // We keep the same field names (imageUrl/imageUrls) but store *data URIs* when Storage is not available.
        placeData.put("imageUrls", imageUrlsOrDataUris);
        placeData.put("imageUrl", imageUrlsOrDataUris.isEmpty() ? "" : imageUrlsOrDataUris.get(0));

        placesRef.child(placeId).setValue(placeData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, getString(R.string.add_place_success), Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }

    private static String safeText(@NonNull EditText edt) {
        return edt.getText() == null ? "" : edt.getText().toString().trim();
    }

    @Override
    public void onRemove(int position) {
        if (position < 0 || position >= previewItems.size()) return;
        previewItems.remove(position);
        imageAdapter.submit(previewItems);

        if (position < selectedUris.size()) selectedUris.remove(position);

        if (!selectedUris.isEmpty()) {
            Glide.with(imgPreview).load(selectedUris.get(0)).centerCrop().into(imgPreview);
        } else {
            imgPreview.setImageResource(R.drawable.ic_placeholder);
        }
    }

    @Override
    public void onClick(int position) {
        if (position < 0 || position >= previewItems.size()) return;
        ImagePreviewItem item = previewItems.get(position);
        if (item.localUri != null) {
            Glide.with(imgPreview).load(item.localUri).centerCrop().into(imgPreview);
        }
    }
}
