package com.example.nptudttbdd;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Responsible for sending OTP emails using SMTP credentials configured via BuildConfig fields.
 */
public class OtpEmailSender {

    public interface Callback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void sendOtpEmail(@NonNull String recipientEmail,
                             @NonNull String otp,
                             @NonNull Callback callback) {
        if (!isConfigured()) {
            mainHandler.post(() -> callback.onError(
                    "Chưa cấu hình tài khoản email gửi OTP. Vui lòng liên hệ quản trị viên."));
            return;
        }

        String senderEmail = BuildConfig.OTP_EMAIL_ADDRESS;
        String senderPassword = BuildConfig.OTP_EMAIL_PASSWORD;

        executor.execute(() -> {
            try {
                sendEmailInternal(recipientEmail, otp, senderEmail, senderPassword);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = e.toString();
                }
                final String errorMessage = message;
                mainHandler.post(() -> callback.onError(errorMessage));
            }
        });
    }

    public boolean isConfigured() {
        return BuildConfig.OTP_EMAIL_ADDRESS != null
                && !BuildConfig.OTP_EMAIL_ADDRESS.isEmpty()
                && BuildConfig.OTP_EMAIL_PASSWORD != null
                && !BuildConfig.OTP_EMAIL_PASSWORD.isEmpty();
    }

    private void sendEmailInternal(@NonNull String recipientEmail,
                                   @NonNull String otp,
                                   @NonNull String senderEmail,
                                   @NonNull String senderPassword)
            throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", Boolean.toString(BuildConfig.OTP_EMAIL_USE_TLS));
        props.put("mail.smtp.host", BuildConfig.OTP_EMAIL_HOST);
        props.put("mail.smtp.port", Integer.toString(BuildConfig.OTP_EMAIL_PORT));
        props.put("mail.smtp.ssl.trust", BuildConfig.OTP_EMAIL_HOST);

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail, BuildConfig.OTP_EMAIL_SENDER_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Mã OTP khôi phục mật khẩu");

        String emailBody = "Xin chào,\n\n"
                + "Bạn vừa yêu cầu lấy lại mật khẩu cho tài khoản Travelover của mình. "
                + "Vui lòng sử dụng mã OTP bên dưới trong vòng 5 phút:\n\n"
                + otp + "\n\n"
                + "Nếu bạn không yêu cầu thao tác này, vui lòng bỏ qua email.\n\n"
                + "Trân trọng,\n"
                + BuildConfig.OTP_EMAIL_SENDER_NAME;

        message.setText(emailBody);
        Transport.send(message);
    }
}
