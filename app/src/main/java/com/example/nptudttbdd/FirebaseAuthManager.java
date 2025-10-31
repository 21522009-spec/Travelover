package com.example.nptudttbdd;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    public static final String USERS_NODE = "Users";

    private final FirebaseAuth auth;
    private final DatabaseReference usersRef;

    public FirebaseAuthManager() {
        this(FirebaseAuth.getInstance(), FirebaseDatabase.getInstance());
    }

    public FirebaseAuthManager(@NonNull FirebaseAuth auth, @NonNull FirebaseDatabase database) {
        this.auth = auth;
        this.usersRef = database.getReference(USERS_NODE);
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

        UserProfile profile = new UserProfile(firebaseUser.getUid(), name, email, "user");
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