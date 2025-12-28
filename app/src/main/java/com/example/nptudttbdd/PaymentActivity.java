package com.example.nptudttbdd;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvPlaceName;
    private TextView tvPlaceLocation;
    private TextView tvDates;
    private TextView tvGuests;
    private TextView tvPriceLineLeft;
    private TextView tvPriceLineRight;
    private TextView tvTotalPrice;
    private MaterialButton btnPay;
    private View loadingOverlay;
    private RadioGroup rgPaymentMethod;

    private String placeId;
    private String ownerId;
    private String checkIn;
    private String checkOut;
    private int adults;
    private int children;
    private long totalPrice;
    private long nights;
    private long pricePerNight;

    private DatabaseReference placesRef;
    private DatabaseReference bookingsRef;
    private DatabaseReference paymentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        ChatButtonManager.attach(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvPlaceLocation = findViewById(R.id.tvPlaceLocation);
        tvDates = findViewById(R.id.tvDates);
        tvGuests = findViewById(R.id.tvGuests);
        tvPriceLineLeft = findViewById(R.id.tvPriceLineLeft);
        tvPriceLineRight = findViewById(R.id.tvPriceLineRight);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnPay = findViewById(R.id.btnPay);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);

        String placeName = getIntent().getStringExtra("placeName");
        String location = getIntent().getStringExtra("location");
        placeId = getIntent().getStringExtra("placeId");
        checkIn = getIntent().getStringExtra("checkIn");
        checkOut = getIntent().getStringExtra("checkOut");
        adults = getIntent().getIntExtra("adults", 1);
        children = getIntent().getIntExtra("children", 0);
        totalPrice = getIntent().getLongExtra("totalPrice", 0);
        nights = getIntent().getLongExtra("nights", 0);
        pricePerNight = getIntent().getLongExtra("pricePerNight", 0);

        if (placeName == null) placeName = "";
        if (location == null) location = "";

        if (nights <= 0) nights = 1;
        if (pricePerNight <= 0 && nights > 0) pricePerNight = totalPrice / nights;

        tvPlaceName.setText(placeName);
        tvPlaceLocation.setText(location);
        tvDates.setText("Ngày đến: " + (checkIn == null ? "" : checkIn) + " • Ngày đi: " + (checkOut == null ? "" : checkOut));
        tvGuests.setText("Số khách: " + adults + " người lớn" + (children > 0 ? ", " + children + " trẻ em" : ""));

        String priceLineLeft = nights + " đêm × " + TravelDataRepository.formatCurrency(pricePerNight);
        tvPriceLineLeft.setText(priceLineLeft);
        tvPriceLineRight.setText(TravelDataRepository.formatCurrency(totalPrice));
        tvTotalPrice.setText(TravelDataRepository.formatCurrency(totalPrice));

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        placesRef = database.getReference("Places");
        bookingsRef = database.getReference("Bookings");
        paymentsRef = database.getReference("Payments");

        btnPay.setOnClickListener(v -> processPayment());
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!loading);
        rgPaymentMethod.setEnabled(!loading);
        for (int i = 0; i < rgPaymentMethod.getChildCount(); i++) {
            rgPaymentMethod.getChildAt(i).setEnabled(!loading);
        }
    }

    private String getSelectedPaymentMethod() {
        int checkedId = rgPaymentMethod.getCheckedRadioButtonId();
        if (checkedId == R.id.rbCard) return "card";
        if (checkedId == R.id.rbCash) return "cash";
        return "wallet";
    }

    private void processPayment() {
        setLoading(true);

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (userId.isEmpty()) {
            Toast.makeText(PaymentActivity.this, "Bạn cần đăng nhập để thanh toán.", Toast.LENGTH_SHORT).show();
            setLoading(false);
            return;
        }

        if (placeId == null || placeId.trim().isEmpty()) {
            ownerId = "unknown";
            createBookingAndPayment(userId);
            return;
        }

        placesRef.child(placeId).child("ownerId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ownerId = snapshot.getValue(String.class);
                if (ownerId == null || ownerId.trim().isEmpty()) {
                    ownerId = "unknown";
                }
                createBookingAndPayment(userId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaymentActivity.this, "Đã xảy ra lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void createBookingAndPayment(@NonNull String userId) {
        String bookingId = bookingsRef.push().getKey();
        String paymentId = paymentsRef.push().getKey();
        if (bookingId == null || paymentId == null) {
            Toast.makeText(PaymentActivity.this, "Đã xảy ra lỗi. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            setLoading(false);
            return;
        }

        BookingInfo bookingInfo = new BookingInfo(
                bookingId,
                userId,
                ownerId,
                placeId == null ? "" : placeId,
                checkIn == null ? "" : checkIn,
                checkOut == null ? "" : checkOut,
                adults,
                children,
                totalPrice,
                "confirmed"
        );

        bookingsRef.child(bookingId).setValue(bookingInfo)
                .addOnSuccessListener(unused -> {
                    String method = getSelectedPaymentMethod();
                    PaymentInfo paymentInfo = new PaymentInfo(
                            paymentId,
                            bookingId,
                            userId,
                            ownerId,
                            totalPrice,
                            method,
                            System.currentTimeMillis()
                    );

                    paymentsRef.child(paymentId).setValue(paymentInfo)
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(PaymentActivity.this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                                setLoading(false);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(PaymentActivity.this, "Lỗi: Không thể lưu thông tin thanh toán.", Toast.LENGTH_SHORT).show();
                                setLoading(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PaymentActivity.this, "Lỗi: Không thể lưu thông tin đặt phòng.", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }

    public static class BookingInfo {
        public String bookingId;
        public String userId;
        public String ownerId;
        public String placeId;
        public String checkIn;
        public String checkOut;
        public int adults;
        public int children;
        public long totalPrice;
        public String status;

        public BookingInfo() {}

        public BookingInfo(String bookingId, String userId, String ownerId, String placeId,
                           String checkIn, String checkOut, int adults, int children,
                           long totalPrice, String status) {
            this.bookingId = bookingId;
            this.userId = userId;
            this.ownerId = ownerId;
            this.placeId = placeId;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.adults = adults;
            this.children = children;
            this.totalPrice = totalPrice;
            this.status = status;
        }
    }

    public static class PaymentInfo {
        public String paymentId;
        public String bookingId;
        public String userId;
        public String ownerId;
        public long amount;
        public String method;
        public long timestamp;

        public PaymentInfo() {}

        public PaymentInfo(String paymentId, String bookingId, String userId, String ownerId,
                           long amount, String method, long timestamp) {
            this.paymentId = paymentId;
            this.bookingId = bookingId;
            this.userId = userId;
            this.ownerId = ownerId;
            this.amount = amount;
            this.method = method;
            this.timestamp = timestamp;
        }
    }
}
