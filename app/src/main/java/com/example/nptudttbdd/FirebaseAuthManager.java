package com.example.nptudttbdd;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;

public class FirebaseAuthManager {

    public interface RegisterCallback {
        void onSuccess(@NonNull FirebaseUser firebaseUser, @NonNull UserProfile profile);

        void onError(@NonNull String message);
    }

    public interface LoginCallback {
        void onSuccess(@NonNull FirebaseUser firebaseUser, UserProfile profile);

        void onError(@NonNull String message);
    }

    public interface CompletionCallback {
        void onComplete();

        void onError(@NonNull String message);
    }

    public interface OtpRequestCallback {
        void onOtpSent();

        void onError(@NonNull String message);
    }

    public static final String USERS_NODE = "Users";
    private static final String PASSWORD_RESET_OTPS_NODE = "PasswordResetOtps";
    private static final long OTP_VALIDITY_DURATION_MS = 5 * 60 * 1000;

    private final FirebaseAuth auth;
    private final DatabaseReference usersRef;
    private final DatabaseReference passwordResetOtpsRef;
    private final OtpEmailSender otpEmailSender;
    private final Random random;

    public FirebaseAuthManager() {
        this(FirebaseAuth.getInstance(), FirebaseDatabase.getInstance());
    }

    public FirebaseAuthManager(@NonNull FirebaseAuth auth, @NonNull FirebaseDatabase database) {
        this.auth = auth;
        this.usersRef = database.getReference(USERS_NODE);
        this.passwordResetOtpsRef = database.getReference(PASSWORD_RESET_OTPS_NODE);
        this.otpEmailSender = new OtpEmailSender();
        this.random = new Random();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }

    public void registerUser(String name,
                             String email,
                             String password,
                             RegisterCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> handleRegisterSuccess(authResult, name, email, callback))
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
    }

    private void handleRegisterSuccess(AuthResult authResult,
                                       String name,
                                       String email,
                                       RegisterCallback callback) {
        FirebaseUser firebaseUser = authResult.getUser();
        if (firebaseUser == null) {
            callback.onError("Không thể lấy thông tin người dùng.");
            return;
        }

        UserProfile profile = new UserProfile(firebaseUser.getUid(),
                name,
                email,
                "user",
                "");
        saveUserProfile(profile, new CompletionCallback() {
            @Override
            public void onComplete() {
                callback.onSuccess(firebaseUser, profile);
            }

            @Override
            public void onError(@NonNull String message) {
                callback.onError(message);
            }
        });
    }

    public void loginUser(String email,
                          String password,
                          LoginCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> handleLoginSuccess(authResult.getUser(), callback))
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
    }

    private void handleLoginSuccess(FirebaseUser firebaseUser,
                                    LoginCallback callback) {
        if (firebaseUser == null) {
            callback.onError("Không thể lấy thông tin người dùng.");
            return;
        }

        usersRef.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile profile = snapshot.getValue(UserProfile.class);
                callback.onSuccess(firebaseUser, profile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(getErrorMessage(error.toException()));
            }
        });
    }

    public void sendPasswordReset(String email, CompletionCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onComplete())
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
    }

    public void requestPasswordResetOtp(@NonNull String email, @NonNull OtpRequestCallback callback) {
        findUserByEmail(email, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot userSnapshot = getFirstChild(snapshot);
                if (userSnapshot == null) {
                    callback.onError("Không tìm thấy tài khoản với email này.");
                    return;
                }

                String uid = userSnapshot.getKey();
                if (uid == null || uid.isEmpty()) {
                    callback.onError("Không thể xác định tài khoản người dùng.");
                    return;
                }

                String otp = generateOtp();
                String otpHash = hashOtp(otp);
                long expiresAt = System.currentTimeMillis() + OTP_VALIDITY_DURATION_MS;
                PasswordResetOtp otpData = new PasswordResetOtp(otpHash, expiresAt, false);

                passwordResetOtpsRef.child(uid)
                        .setValue(otpData)
                        .addOnSuccessListener(unused -> otpEmailSender.sendOtpEmail(email, otp,
                                new OtpEmailSender.Callback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onOtpSent();
                                    }

                                    @Override
                                    public void onError(@NonNull String message) {
                                        passwordResetOtpsRef.child(uid).removeValue();
                                        callback.onError(message);
                                    }
                                }))
                        .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(getErrorMessage(error.toException()));
            }
        });
    }

    public void verifyOtpAndSendResetEmail(@NonNull String email,
                                           @NonNull String otp,
                                           @NonNull CompletionCallback callback) {
        findUserByEmail(email, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot userSnapshot = getFirstChild(snapshot);
                if (userSnapshot == null) {
                    callback.onError("Không tìm thấy tài khoản với email này.");
                    return;
                }

                String uid = userSnapshot.getKey();
                if (uid == null || uid.isEmpty()) {
                    callback.onError("Không thể xác định tài khoản người dùng.");
                    return;
                }

                passwordResetOtpsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot otpSnapshot) {
                        PasswordResetOtp otpData = otpSnapshot.getValue(PasswordResetOtp.class);
                        if (otpData == null || otpData.isUsed()) {
                            callback.onError("Mã OTP không hợp lệ hoặc đã được sử dụng.");
                            return;
                        }

                        long now = System.currentTimeMillis();
                        if (otpData.getExpiresAt() < now) {
                            passwordResetOtpsRef.child(uid).removeValue();
                            callback.onError("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
                            return;
                        }

                        String expectedHash = otpData.getOtpHash();
                        if (!hashOtp(otp).equals(expectedHash)) {
                            callback.onError("Mã OTP không chính xác.");
                            return;
                        }

                        passwordResetOtpsRef.child(uid).child("used").setValue(true)
                                .addOnSuccessListener(unused -> auth.sendPasswordResetEmail(email)
                                        .addOnSuccessListener(unused1 -> passwordResetOtpsRef.child(uid)
                                                .removeValue()
                                                .addOnSuccessListener(unused2 -> callback.onComplete())
                                                .addOnFailureListener(e -> callback.onError(getErrorMessage(e))))
                                        .addOnFailureListener(e -> callback.onError(getErrorMessage(e))))
                                .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(getErrorMessage(error.toException()));
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(getErrorMessage(error.toException()));
            }
        });
    }

    private void findUserByEmail(@NonNull String email, @NonNull ValueEventListener listener) {
        Query query = usersRef.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(listener);
    }

    private DataSnapshot getFirstChild(@NonNull DataSnapshot snapshot) {
        for (DataSnapshot child : snapshot.getChildren()) {
            return child;
        }
        return null;
    }

    @NonNull
    private String generateOtp() {
        int value = 100000 + random.nextInt(900000);
        return String.format(Locale.getDefault(), "%06d", value);
    }

    @NonNull
    private String hashOtp(@NonNull String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return otp;
        }
    }

    public void saveUserProfile(@NonNull UserProfile profile, @NonNull CompletionCallback callback) {
        String uid = profile.getUid();
        if (uid.isEmpty()) {
            callback.onError("Thiếu UID người dùng để lưu hồ sơ.");
            return;
        }

        usersRef.child(uid)
                .setValue(profile)
                .addOnSuccessListener(unused -> callback.onComplete())
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
    }

    public DatabaseReference getUserProfileReference(@NonNull String uid) {
        return usersRef.child(uid);
    }

    private String getErrorMessage(Exception e) {
        if (e == null) {
            return "Đã xảy ra lỗi không xác định.";
        }
        String message = e.getLocalizedMessage();
        return message != null ? message : e.toString();
    }
}