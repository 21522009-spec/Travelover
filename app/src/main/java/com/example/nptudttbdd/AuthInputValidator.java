package com.example.nptudttbdd;

import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;

public final class AuthInputValidator {

    private AuthInputValidator() {
    }

    public static String getTrimmedText(EditText editText) {
        return editText.getText().toString().trim();
    }

    public static boolean ensureRequired(EditText editText, String errorMessage) {
        if (TextUtils.isEmpty(getTrimmedText(editText))) {
            editText.setError(errorMessage);
            editText.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean ensureValidEmail(EditText editText,
                                           String emptyErrorMessage,
                                           String invalidErrorMessage) {
        String email = getTrimmedText(editText);
        if (TextUtils.isEmpty(email)) {
            editText.setError(emptyErrorMessage);
            editText.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editText.setError(invalidErrorMessage);
            editText.requestFocus();
            return false;
        }
        return true;
    }
}
