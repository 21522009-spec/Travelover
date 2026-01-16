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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerEditPlaceActivity extends AppCompatActivity implements ImagePreviewAdapter.Callback {

    public static final String EXTRA_PLACE_ID = "placeId";
    private static final int REQ_PICK_IMAGES = 2201;

    private static final int MAX_IMAGES = 5;
    private static final int MAX_DIMENSION = 1024;
    private static final int MAX_BYTES_PER_IMAGE = 350 * 1024;

    private String placeId;

    private MaterialToolbar toolbar;
    private EditText edtName;
    private EditText edtLocation;
    private EditText edtPrice;
    private EditText edtDescription;
    private SwitchMaterial switchActive;

    private ImageView imgPreview;
    private RecyclerView rvImages;
    private Button btnChooseImages;
    private Button btnSave;

    private View loadingOverlay;
    private ProgressBar progressBar;

    private ImagePreviewAdapter imageAdapter;
    private final ArrayList<ImagePreviewItem> previewItems = new ArrayList<>();

    private final ArrayList<Uri> selectedLocalUris = new ArrayList<>();
    private final ArrayList<String> existingUrlsOrDataUris = new ArrayList<>();

    private boolean hasNewImages = false;

    private DatabaseReference placesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_edit_place);
        ChatButtonManager.attach(this);

        placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        if (placeId == null || placeId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu placeId.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtName = findViewById(R.id.edtPlaceName);
        edtLocation = findViewById(R.id.edtPlaceLocation);
        edtPrice = findViewById(R.id.edtPrice);
        edtDescription = findViewById(R.id.edtDescription);
        switchActive = findViewById(R.id.switchActive);

        imgPreview = findViewById(R.id.imgPreview);
        rvImages = findViewById(R.id.rvSelectedImages);
        btnChooseImages = findViewById(R.id.btnChooseImage);
        btnSave = findViewById(R.id.btnSavePlace);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressBar = findViewById(R.id.progressBar);

        imageAdapter = new ImagePreviewAdapter(this);
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(imageAdapter);

        placesRef = FirebaseDatabase.getInstance().getReference("Places");

        btnChooseImages.setOnClickListener(v -> pickImages());
        btnSave.setOnClickListener(v -> submitUpdate());

        loadPlace();
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnChooseImages.setEnabled(!loading);
    }

    private void loadPlace() {
        setLoading(true);

        placesRef.child(placeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (!snapshot.exists()) {
                    Toast.makeText(OwnerEditPlaceActivity.this, "Không tìm thấy phòng.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String name = snapshot.child("name").getValue(String.class);
                String location = snapshot.child("location").getValue(String.class);
                Long price = snapshot.child("pricePerNight").getValue(Long.class);
                String desc = snapshot.child("description").getValue(String.class);
                Boolean active = snapshot.child("active").getValue(Boolean.class);

                edtName.setText(name == null ? "" : name);
                edtLocation.setText(location == null ? "" : location);
                edtPrice.setText(price == null ? "" : String.valueOf(price));
                edtDescription.setText(desc == null ? "" : desc);
                switchActive.setChecked(active == null ? true : active);

                existingUrlsOrDataUris.clear();
                DataSnapshot urlsSnap = snapshot.child("imageUrls");
                if (urlsSnap.exists()) {
                    for (DataSnapshot u : urlsSnap.getChildren()) {
                        String url = u.getValue(String.class);
                        if (!TextUtils.isEmpty(url)) existingUrlsOrDataUris.add(url);
                    }
                }
                if (existingUrlsOrDataUris.isEmpty()) {
                    String cover = snapshot.child("imageUrl").getValue(String.class);
                    if (!TextUtils.isEmpty(cover)) existingUrlsOrDataUris.add(cover);
                }

                previewItems.clear();
                for (String url : existingUrlsOrDataUris) {
                    previewItems.add(new ImagePreviewItem(url));
                }
                imageAdapter.submit(previewItems);

                if (!previewItems.isEmpty()) {
                    ImagePreviewItem first = previewItems.get(0);
                    if (!TextUtils.isEmpty(first.remoteUrl)) {
                        ImageLoader.load(imgPreview, first.remoteUrl, R.drawable.ic_placeholder);
                    } else {
                        imgPreview.setImageResource(R.drawable.ic_placeholder);
                    }
                } else {
                    imgPreview.setImageResource(R.drawable.ic_placeholder);
                }

                // reset selection state
                hasNewImages = false;
                selectedLocalUris.clear();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(OwnerEditPlaceActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
            selectedLocalUris.clear();

            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    if (selectedLocalUris.size() >= MAX_IMAGES) break;
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) selectedLocalUris.add(uri);
                }
            } else {
                Uri uri = data.getData();
                if (uri != null) selectedLocalUris.add(uri);
            }

            if (selectedLocalUris.isEmpty()) {
                Toast.makeText(this, "Bạn chưa chọn ảnh.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (clipData != null && clipData.getItemCount() > MAX_IMAGES) {
                Toast.makeText(this, "Chỉ hỗ trợ tối đa " + MAX_IMAGES + " ảnh để tránh nặng dữ liệu.", Toast.LENGTH_LONG).show();
            }

            hasNewImages = true;

            previewItems.clear();
            for (Uri uri : selectedLocalUris) {
                previewItems.add(new ImagePreviewItem(uri));
            }
            imageAdapter.submit(previewItems);

            Glide.with(imgPreview).load(selectedLocalUris.get(0)).centerCrop().into(imgPreview);
        }
    }

    private void submitUpdate() {
        String name = safeText(edtName);
        String location = safeText(edtLocation);
        String priceStr = safeText(edtPrice);
        String description = safeText(edtDescription);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        long price;
        try {
            price = Long.parseLong(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean active = switchActive.isChecked();

        if (hasNewImages && selectedLocalUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        // If user didn't pick new images but removed all existing -> prevent empty
        if (!hasNewImages && existingUrlsOrDataUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng giữ lại ít nhất 1 ảnh (hoặc chọn ảnh mới).", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (hasNewImages) {
            encodeImagesThenUpdate(price, name, location, description, active);
        } else {
            Map<String, Object> update = new HashMap<>();
            update.put("name", name);
            update.put("location", location);
            update.put("pricePerNight", price);
            update.put("description", description);
            update.put("active", active);
            update.put("imageUrls", existingUrlsOrDataUris);
            update.put("imageUrl", existingUrlsOrDataUris.isEmpty() ? "" : existingUrlsOrDataUris.get(0));

            placesRef.child(placeId).updateChildren(update)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Đã cập nhật phòng.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Không thể cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    });
        }
    }

    private void encodeImagesThenUpdate(long price,
                                        @NonNull String name,
                                        @NonNull String location,
                                        @NonNull String description,
                                        boolean active) {

        new Thread(() -> {
            try {
                ArrayList<String> newDataUris = new ArrayList<>();
                for (Uri uri : selectedLocalUris) {
                    newDataUris.add(encodeUriToDataUri(uri));
                }

                runOnUiThread(() -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("name", name);
                    update.put("location", location);
                    update.put("pricePerNight", price);
                    update.put("description", description);
                    update.put("active", active);
                    update.put("imageUrls", newDataUris);
                    update.put("imageUrl", newDataUris.isEmpty() ? "" : newDataUris.get(0));

                    placesRef.child(placeId).updateChildren(update)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Đã cập nhật phòng.", Toast.LENGTH_SHORT).show();
                                setLoading(false);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Không thể cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                setLoading(false);
                            });
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Không thể xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
            }
        }).start();
    }

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

    private static String safeText(@NonNull EditText edt) {
        return edt.getText() == null ? "" : edt.getText().toString().trim();
    }

    @Override
    public void onRemove(int position) {
        if (position < 0 || position >= previewItems.size()) return;
        previewItems.remove(position);
        imageAdapter.submit(previewItems);

        if (hasNewImages) {
            if (position < selectedLocalUris.size()) selectedLocalUris.remove(position);
        } else {
            if (position < existingUrlsOrDataUris.size()) existingUrlsOrDataUris.remove(position);
        }

        if (!previewItems.isEmpty()) {
            ImagePreviewItem first = previewItems.get(0);
            if (first.isLocal()) {
                Glide.with(imgPreview).load(first.localUri).centerCrop().into(imgPreview);
            } else {
                ImageLoader.load(imgPreview, first.remoteUrl, R.drawable.ic_placeholder);
            }
        } else {
            imgPreview.setImageResource(R.drawable.ic_placeholder);
        }
    }

    @Override
    public void onClick(int position) {
        if (position < 0 || position >= previewItems.size()) return;
        ImagePreviewItem item = previewItems.get(position);
        if (item.isLocal()) {
            Glide.with(imgPreview).load(item.localUri).centerCrop().into(imgPreview);
        } else {
            ImageLoader.load(imgPreview, item.remoteUrl, R.drawable.ic_placeholder);
        }
    }
}
