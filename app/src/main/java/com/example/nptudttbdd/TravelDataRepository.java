package com.example.nptudttbdd;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TravelDataRepository {

    private static TravelDataRepository instance;

    private final List<Place> places = new ArrayList<>();
    private final List<UserAccount> users = new ArrayList<>();
    private final List<ChatMessage> adminConversation = new ArrayList<>();
    private final List<ChatMessage> ownerConversation = new ArrayList<>();

    private TravelDataRepository(Context context) {
        seedPlaces(context);
        seedUsers();
        seedAdminMessages();
        seedOwnerMessages();
    }

    public static synchronized TravelDataRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new TravelDataRepository(context.getApplicationContext());
        }
        return instance;
    }

    @NonNull
    public List<Place> getPlaces() {
        return new ArrayList<>(places);
    }

    @NonNull
    public Place getPlaceOrThrow(@NonNull String id) {
        for (Place place : places) {
            if (place.getId().equals(id)) {
                return place;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy địa điểm với id=" + id);
    }

    public void addPlace(@NonNull Place place) {
        places.add(0, place);
    }

    public void removePlace(@NonNull String placeId) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getId().equals(placeId)) {
                places.remove(i);
                return;
            }
        }
    }

    @NonNull
    public List<Place> searchPlaces(@NonNull String query) {
        String lower = query.trim().toLowerCase(Locale.getDefault());
        if (lower.isEmpty()) {
            return getPlaces();
        }
        List<Place> result = new ArrayList<>();
        for (Place place : places) {
            if (place.getName().toLowerCase(Locale.getDefault()).contains(lower)
                    || place.getLocation().toLowerCase(Locale.getDefault()).contains(lower)) {
                result.add(place);
            }
        }
        return result;
    }

    @NonNull
    public List<UserAccount> getUsers() {
        return new ArrayList<>(users);
    }

    public void toggleUserLock(@NonNull String userId) {
        for (UserAccount account : users) {
            if (account.getId().equals(userId)) {
                account.setLocked(!account.isLocked());
                return;
            }
        }
    }

    public void deleteUser(@NonNull String userId) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(userId)) {
                users.remove(i);
                return;
            }
        }
    }

    @NonNull
    public List<ChatMessage> getAdminConversation() {
        return new ArrayList<>(adminConversation);
    }

    public void appendAdminMessage(@NonNull ChatMessage message) {
        adminConversation.add(message);
    }

    @NonNull
    public List<ChatMessage> getOwnerConversation() {
        return new ArrayList<>(ownerConversation);
    }

    public void appendOwnerMessage(@NonNull ChatMessage message) {
        ownerConversation.add(message);
    }

    public int getTotalUsers() {
        return users.size();
    }

    public int getTotalPlaces() {
        return places.size();
    }

    public int getTotalReports() {
        return 24; // dữ liệu mẫu
    }

    @NonNull
    public static String formatCurrency(long price) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(price);
    }

    private void seedPlaces(Context context) {
        if (!places.isEmpty()) {
            return;
        }
        addPlaceInternal("place_giang_dien",
                "Khu du lịch Thác Giang Điền",
                "Trảng Bom, Đồng Nai",
                500_000,
                4.4f,
                326,
                context.getString(R.string.place_desc_giang_dien),
                R.drawable.thac_giang_dien,
                new String[]{"Hồ bơi ngoài trời", "BBQ ngoài trời", "Khu cắm trại", "Wifi miễn phí"});

        addPlaceInternal("place_dalat_hill",
                "Homestay Đồi Gió",
                "Phường 3, Đà Lạt",
                780_000,
                4.8f,
                512,
                "Homestay thiết kế theo phong cách Bắc Âu với view đồi thông, phù hợp gia đình và nhóm bạn.",
                R.drawable.thac_giang_dien,
                new String[]{"Ăn sáng miễn phí", "View đồi thông", "Cho thuê xe máy"});

        addPlaceInternal("place_vungtau_sunset",
                "Sunset Villa",
                "Phường Thắng Tam, Vũng Tàu",
                1_200_000,
                4.6f,
                421,
                "Biệt thự sát biển với hồ bơi riêng và khu vực BBQ, cách Bãi Sau 300m.",
                R.drawable.thac_giang_dien,
                new String[]{"Hồ bơi riêng", "BBQ", "Ban công hướng biển"});

        addPlaceInternal("place_saigon_center",
                "Căn hộ Trung tâm Sài Gòn",
                "Quận 1, TP. Hồ Chí Minh",
                950_000,
                4.5f,
                389,
                "Căn hộ cao cấp tại trung tâm, di chuyển thuận tiện đến các điểm du lịch nổi tiếng.",
                R.drawable.thac_giang_dien,
                new String[]{"Gym", "Hồ bơi", "Lễ tân 24/7"});
    }

    private void addPlaceInternal(String id,
                                  String name,
                                  String location,
                                  long price,
                                  float rating,
                                  int ratingCount,
                                  String description,
                                  @DrawableRes int imageRes,
                                  String[] amenities) {
        ArrayList<String> amenityList = new ArrayList<>();
        Collections.addAll(amenityList, amenities);
        Place place = new Place(id,
                name,
                location,
                price,
                rating,
                ratingCount,
                description,
                imageRes,
                amenityList);
        places.add(place);
    }

    private void seedUsers() {
        if (!users.isEmpty()) {
            return;
        }
        users.add(new UserAccount(UUID.randomUUID().toString(),
                "Nguyễn Văn A",
                "nguyenvana@example.com",
                "0901234567",
                false));
        users.add(new UserAccount(UUID.randomUUID().toString(),
                "Trần Thị B",
                "tranthib@example.com",
                "0987654321",
                false));
        users.add(new UserAccount(UUID.randomUUID().toString(),
                "Lê Văn C",
                "levanc@example.com",
                "0912345678",
                true));
    }

    private void seedAdminMessages() {
        if (!adminConversation.isEmpty()) {
            return;
        }
        adminConversation.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.USER,
                "Xin chào, tôi cần hỗ trợ về đơn đặt phòng!"));
        adminConversation.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.ADMIN,
                "Chào bạn! Vui lòng cung cấp mã đặt phòng để mình kiểm tra giúp nhé."));
        adminConversation.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.USER,
                "Mã đơn của mình là TVL-2401."));
    }

    private void seedOwnerMessages() {
        if (!ownerConversation.isEmpty()) {
            return;
        }
        ownerConversation.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.GUEST,
                "Chào anh/chị, phòng còn trống vào cuối tuần này không?"));
        ownerConversation.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.OWNER,
                "Chào bạn, hiện tại còn 2 phòng đôi trống vào cuối tuần nhé!"));
        ownerConversation.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.GUEST,
                "Mình muốn đặt phòng cho 2 người lớn và 1 bé, giá như thế nào ạ?"));
    }
}
