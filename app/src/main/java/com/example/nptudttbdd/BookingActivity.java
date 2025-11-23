package com.example.nptudttbdd;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "extra_place_id";

    private final Calendar checkInCalendar = Calendar.getInstance();
    private final Calendar checkOutCalendar = Calendar.getInstance();

    private Place place;
    private TravelDataRepository repository;
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);
        String placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        if (placeId == null) {
            finish();
            return;
        }
        place = repository.getPlaceOrThrow(placeId);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView imgPlace = findViewById(R.id.imgPlace);
        TextView tvPlaceName = findViewById(R.id.tvPlaceName);
        TextView tvLocation = findViewById(R.id.tvLocation);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        TextView tvRatingValue = findViewById(R.id.tvRatingValue);
        TextView tvPricePerNight = findViewById(R.id.tvPricePerNight);
        TextView tvDescription = findViewById(R.id.tvDescription);
        EditText edtCheckIn = findViewById(R.id.edtCheckIn);
        EditText edtCheckOut = findViewById(R.id.edtCheckOut);
        EditText edtAdults = findViewById(R.id.edtAdults);
        EditText edtChildren = findViewById(R.id.edtChildren);
        Button btnConfirm = findViewById(R.id.btnConfirmBooking);

        btnBack.setOnClickListener(v -> finish());
        imgPlace.setImageResource(place.getImageResId());
        tvPlaceName.setText(place.getName());
        tvLocation.setText(place.getLocation());
        ratingBar.setRating(place.getRating());
        tvRatingValue.setText(getString(R.string.place_rating_template,
                place.getRating(),
                place.getRatingCount()));
        tvPricePerNight.setText(getString(R.string.booking_price_template,
                TravelDataRepository.formatCurrency(place.getPricePerNight())));
        String description = place.getDescription();
        if (!place.getAmenities().isEmpty()) {
            String amenities = getString(R.string.place_detail_amenities_prefix,
                    TextUtils.join(", ", place.getAmenities()));
            description = description + "\n\n" + amenities;
        }
        tvDescription.setText(description);

        edtCheckIn.setOnClickListener(v -> showDatePicker(edtCheckIn, checkInCalendar));
        edtCheckOut.setOnClickListener(v -> showDatePicker(edtCheckOut, checkOutCalendar));

        btnConfirm.setOnClickListener(v -> {
            try {
                BookingSummary summary = buildSummary(edtCheckIn.getText().toString(),
                        edtCheckOut.getText().toString(),
                        edtAdults.getText().toString(),
                        edtChildren.getText().toString());
                showSummaryDialog(summary);
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker(@NonNull EditText target, @NonNull Calendar calendar) {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    target.setText(displayFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private BookingSummary buildSummary(String checkIn,
                                        String checkOut,
                                        String adultInput,
                                        String childInput) {
        if (checkIn.isEmpty() || checkOut.isEmpty()) {
            throw new IllegalArgumentException(getString(R.string.booking_error_missing_date));
        }

        Calendar checkInDate = Calendar.getInstance();
        Calendar checkOutDate = Calendar.getInstance();
        try {
            checkInDate.setTime(displayFormat.parse(checkIn));
            checkOutDate.setTime(displayFormat.parse(checkOut));
        } catch (ParseException e) {
            throw new IllegalArgumentException(getString(R.string.booking_error_invalid_date));
        }

        if (!checkOutDate.after(checkInDate)) {
            throw new IllegalArgumentException(getString(R.string.booking_error_checkout));
        }

        long millisPerDay = 24 * 60 * 60 * 1000L;
        long nights = (checkOutDate.getTimeInMillis() - checkInDate.getTimeInMillis()) / millisPerDay;
        if (nights <= 0) {
            throw new IllegalArgumentException(getString(R.string.booking_error_checkout));
        }

        int adults = parsePositiveNumber(adultInput, 1);
        int children = parsePositiveNumber(childInput, 0);

        long totalPrice = nights * place.getPricePerNight();

        return new BookingSummary(checkIn, checkOut, nights, adults, children, totalPrice);
    }

    private int parsePositiveNumber(String input, int defaultValue) {
        if (input == null || input.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(input.trim());
            return Math.max(value, defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void showSummaryDialog(BookingSummary summary) {
        String message = getString(R.string.booking_summary_template,
                summary.checkIn,
                summary.checkOut,
                summary.nights,
                summary.adults,
                summary.children,
                TravelDataRepository.formatCurrency(summary.totalPrice));
        new AlertDialog.Builder(this)
                .setTitle(R.string.booking_summary_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static class BookingSummary {
        final String checkIn;
        final String checkOut;
        final long nights;
        final int adults;
        final int children;
        final long totalPrice;

        private BookingSummary(String checkIn,
                               String checkOut,
                               long nights,
                               int adults,
                               int children,
                               long totalPrice) {
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.nights = nights;
            this.adults = adults;
            this.children = children;
            this.totalPrice = totalPrice;
        }
    }
}