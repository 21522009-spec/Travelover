package com.example.nptudttbdd;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TravelDataRepository {

    private static TravelDataRepository instance;

    private final List<Place> places = new ArrayList<>();
    private final List<UserAccount> users = new ArrayList<>();
    private final List<ChatMessage> adminConversation = new ArrayList<>();
    private final List<OwnerConversation> ownerConversations = new ArrayList<>();
    private final Map<String, List<ChatMessage>> ownerConversationMessages = new LinkedHashMap<>();
    private final Set<String> favoritePlaceIds = new LinkedHashSet<>();

    private TravelDataRepository(Context context) {
        seedPlaces(context);
        seedUsers();
        seedAdminMessages();
        seedOwnerConversations();
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

    /**
     * Add or replace place by id. Used for syncing dynamic places (e.g., from Firebase).
     */
    public void upsertPlace(@NonNull Place place) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getId().equals(place.getId())) {
                places.set(i, place);
                return;
            }
        }
        places.add(0, place);
    }

    public boolean containsPlaceId(@NonNull String placeId) {
        for (Place p0 : places) {
            if (p0.getId().equals(placeId)) return true;
        }
        return false;
    }


    public void removePlace(@NonNull String placeId) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getId().equals(placeId)) {
                places.remove(i);
                favoritePlaceIds.remove(placeId);
                return;
            }
        }
    }

    public boolean toggleFavorite(@NonNull String placeId) {
        getPlaceOrThrow(placeId);
        if (favoritePlaceIds.contains(placeId)) {
            favoritePlaceIds.remove(placeId);
            return false;
        }
        favoritePlaceIds.add(placeId);
        return true;
    }

    public void setFavorite(@NonNull String placeId, boolean favorite) {
        getPlaceOrThrow(placeId);
        if (favorite) {
            favoritePlaceIds.add(placeId);
        } else {
            favoritePlaceIds.remove(placeId);
        }
    }

    public boolean isFavorite(@NonNull String placeId) {
        return favoritePlaceIds.contains(placeId);
    }

    @NonNull
    public List<Place> getFavoritePlaces() {
        List<Place> result = new ArrayList<>();
        for (String id : favoritePlaceIds) {
            try {
                result.add(getPlaceOrThrow(id));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
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

    public void addUser(@NonNull UserAccount user) {
        users.add(user);
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
    public List<OwnerConversation> getOwnerConversations() {
        return new ArrayList<>(ownerConversations);
    }

    @NonNull
    public String getDefaultOwnerConversationId() {
        return ownerConversations.isEmpty() ? "" : ownerConversations.get(0).getId();
    }

    @NonNull
    public List<ChatMessage> getOwnerConversation(@NonNull String conversationId) {
        List<ChatMessage> messages = ownerConversationMessages.get(conversationId);
        if (messages == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(messages);
    }

    public void appendOwnerMessage(@NonNull String conversationId, @NonNull ChatMessage message) {
        List<ChatMessage> messages = ownerConversationMessages.get(conversationId);
        if (messages == null) {
            messages = new ArrayList<>();
            ownerConversationMessages.put(conversationId, messages);
        }
        messages.add(message);

        OwnerConversation conversation = findOwnerConversationById(conversationId);
        if (conversation != null) {
            conversation.setLastMessage(message.getMessage());
            conversation.setLastTime(formatTimestamp(System.currentTimeMillis()));
            conversation.setHasNewMessage(false);
        }
    }

    public void addReview(@NonNull String placeId, float rating, @NonNull String content) {
        Place place = getPlaceOrThrow(placeId);
        PlaceReview review = new PlaceReview(UUID.randomUUID().toString(),
                placeId,
                rating,
                content,
                System.currentTimeMillis());
        place.addReview(review);
    }

    @NonNull
    public List<PlaceReview> getReviewsForPlace(@NonNull String placeId) {
        return getPlaceOrThrow(placeId).getReviews();
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
                new String[]{"Hồ bơi ngoài trời", "Khu vực cắm trại"});

        addPlaceInternal("place_dalat_hill",
                "Homestay Đồi Gió",
                "Phường 3, Đà Lạt",
                780_000,
                4.8f,
                512,
                "Homestay thiết kế theo phong cách Bắc Âu với view đồi thông, phù hợp gia đình và nhóm bạn.",
                R.drawable.doi_gio_hu,
                new String[]{"Ăn sáng miễn phí", "View đồi thông", "Cho thuê xe máy"});

        addPlaceInternal("place_vungtau_sunset",
                "Sunset Villa",
                "Phường Thắng Tam, Vũng Tàu",
                1_200_000,
                4.6f,
                421,
                "Biệt thự sát biển với hồ bơi riêng và khu vực BBQ, cách Bãi Sau 300m.",
                R.drawable.sunset_villa_vungtau,
                new String[]{"Hồ bơi riêng", "Ban công hướng biển"});

        addPlaceInternal("place_saigon_center",
                "Căn hộ Trung tâm Sài Gòn",
                "Quận 1, TP. Hồ Chí Minh",
                950_000,
                4.5f,
                389,
                "Căn hộ cao cấp tại trung tâm, di chuyển thuận tiện đến các điểm du lịch nổi tiếng.",
                R.drawable.can_ho_trung_tam,
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
                "Anh A",
                "mra@example.com",
                "0901234567",
                false));
        users.add(new UserAccount(UUID.randomUUID().toString(),
                "Chị B",
                "mrsb@example.com",
                "0987654321",
                false));
        users.add(new UserAccount(UUID.randomUUID().toString(),
                "Bác C",
                "unclec@example.com",
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

    private void seedOwnerConversations() {
        if (!ownerConversations.isEmpty()) {
            return;
        }

        OwnerConversation conversationOne = new OwnerConversation("owner_conversation_guesthouse",
                "Host Gia Hưng",
                "Mình muốn đặt phòng cho 2 người lớn và 1 nhỏ, giá như thế nào ạ?",
                "15:46",
                false);
        OwnerConversation conversationTwo = new OwnerConversation("owner_conversation_admin",
                "Admin Travelover",
                "Mã xác nhận là: 783022",
                "15:30",
                true);
        OwnerConversation conversationThree = new OwnerConversation("owner_conversation_mrlong",
                "Mr.Long",
                "Cuối tuần này mình còn phòng không shop?",
                "Hôm nay",
                false);
        OwnerConversation conversationFour = new OwnerConversation("owner_conversation_misslan",
                "Miss.Lan",
                "Cảm ơn bạn!",
                "25 Thg 9",
                false);

        ownerConversations.add(conversationOne);
        ownerConversations.add(conversationTwo);
        ownerConversations.add(conversationThree);
        ownerConversations.add(conversationFour);

        ownerConversationMessages.put(conversationOne.getId(), new ArrayList<>());
        ownerConversationMessages.put(conversationTwo.getId(), new ArrayList<>());
        ownerConversationMessages.put(conversationThree.getId(), new ArrayList<>());
        ownerConversationMessages.put(conversationFour.getId(), new ArrayList<>());
    }

    private void seedOwnerMessages() {
        String defaultConversationId = getDefaultOwnerConversationId();
        if (defaultConversationId.isEmpty()) {
            return;
        }
        List<ChatMessage> messages = ownerConversationMessages.get(defaultConversationId);
        if (messages == null || !messages.isEmpty()) {
            return;
        }
        messages.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.OWNER,
                "Chào shop, chỗ mình còn phòng trống vào cuối tuần này không ạ?"));
        messages.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.GUEST,
                "Chào bạn, hiện tại còn 2 phòng đôi trống vào cuối tuần nhé!"));
        messages.add(new ChatMessage(UUID.randomUUID().toString(),
                ChatMessage.Sender.OWNER,
                "Mình muốn đặt phòng cho 2 người lớn và 1 nhỏ, giá như thế nào ạ?"));

        OwnerConversation conversation = findOwnerConversationById(defaultConversationId);
        if (conversation != null) {
            conversation.setLastMessage(messages.get(messages.size() - 1).getMessage());
            conversation.setLastTime("15:46");
        }
    }

    private OwnerConversation findOwnerConversationById(@NonNull String id) {
        for (OwnerConversation conversation : ownerConversations) {
            if (conversation.getId().equals(id)) {
                return conversation;
            }
        }
        return null;
    }

    @NonNull
    private String formatTimestamp(long timestamp) {
        java.text.DateFormat formatter = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        return formatter.format(timestamp);
    }
}
