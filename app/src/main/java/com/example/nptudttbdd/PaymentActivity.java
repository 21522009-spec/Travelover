package com.example.nptudttbdd;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvPlaceInfo;
    private TextView tvDates;
    private TextView tvGuests;
    private TextView tvTotalPrice;
    private Button btnPay;
    private ProgressBar progressBar;

    private String placeId;
    private String ownerId;
    private String checkIn;
    private String checkOut;
    private int adults;
    private int children;
    private long totalPrice;

    private DatabaseReference placesRef;
    private DatabaseReference bookingsRef;
    private DatabaseReference paymentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        tvPlaceInfo = findViewById(R.id.tvPlaceInfo);
        tvDates = findViewById(R.id.tvDates);
        tvGuests = findViewById(R.id.tvGuests);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnPay = findViewById(R.id.btnPay);
        progressBar = findViewById(R.id.progressBar);

        // Retrieve data passed from BookingActivity
        String placeName = getIntent().getStringExtra("placeName");
        String location = getIntent().getStringExtra("location");
        placeId = getIntent().getStringExtra("placeId");
        checkIn = getIntent().getStringExtra("checkIn");
        checkOut = getIntent().getStringExtra("checkOut");
        adults = getIntent().getIntExtra("adults", 1);
        children = getIntent().getIntExtra("children", 0);
        totalPrice = getIntent().getLongExtra("totalPrice", 0);

        // Display booking details
        tvPlaceInfo.setText(placeName + " - " + location);
        tvDates.setText("Ngày đến: " + checkIn + " - Ngày đi: " + checkOut);
        tvGuests.setText("Số khách: " + adults + " người lớn" + (children > 0 ? ", " + children + " trẻ em" : ""));
        tvTotalPrice.setText("Tổng số tiền: " + TravelDataRepository.formatCurrency(totalPrice));

        // Initialize Firebase references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        placesRef = database.getReference("Places");
        bookingsRef = database.getReference("Bookings");
        paymentsRef = database.getReference("Payments");

        btnPay.setOnClickListener(v -> processPayment());
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!loading);
    }

    private void processPayment() {
        // Prevent multiple clicks
        setLoading(true);

        // Get current user
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (userId.isEmpty()) {
            Toast.makeText(PaymentActivity.this, "Bạn cần đăng nhập để thanh toán.", Toast.LENGTH_SHORT).show();
            setLoading(false);
            return;
        }

        // Fetch ownerId of the place from Firebase
        placesRef.child(placeId).child("ownerId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ownerId = snapshot.getValue(String.class);
                if (ownerId == null || ownerId.isEmpty()) {
                    Toast.makeText(PaymentActivity.this, "Không tìm thấy thông tin chủ sở hữu của địa điểm.", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    return;
                }

                // Generate new IDs for booking and payment
                String bookingId = bookingsRef.push().getKey();
                String paymentId = paymentsRef.push().getKey();
                if (bookingId == null || paymentId == null) {
                    Toast.makeText(PaymentActivity.this, "Đã xảy ra lỗi. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    return;
                }

                // Create booking data
                BookingInfo bookingInfo = new BookingInfo(bookingId, userId, ownerId, placeId,
                        checkIn, checkOut, adults, children, totalPrice, "confirmed");

                // Save booking to database
                bookingsRef.child(bookingId).setValue(bookingInfo)
                        .addOnSuccessListener(unused -> {
                            // Create payment data
                            PaymentInfo paymentInfo = new PaymentInfo(paymentId, bookingId, userId, ownerId,
                                    totalPrice, "online", System.currentTimeMillis());
                            // Save payment to database
                            paymentsRef.child(paymentId).setValue(paymentInfo)
                                    .addOnSuccessListener(unused2 -> {
                                        Toast.makeText(PaymentActivity.this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                                        setLoading(false);
                                        // Finish payment and return to previous screen
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaymentActivity.this, "Đã xảy ra lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    // BookingInfo inner class to structure booking data for Firebase
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

        // Default constructor required for calls to DataSnapshot.getValue(...)
        public BookingInfo() {
        }

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

    // PaymentInfo inner class to structure payment data for Firebase
    public static class PaymentInfo {
        public String paymentId;
        public String bookingId;
        public String userId;
        public String ownerId;
        public long amount;
        public String method;
        public long timestamp;

        public PaymentInfo() {
        }

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
