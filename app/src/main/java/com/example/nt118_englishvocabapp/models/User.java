// java
package com.example.nt118_englishvocabapp.models; // Hoặc package 'models' của bạn

import com.google.gson.annotations.SerializedName;

public class User {

    // @SerializedName("_id") // <-- Dùng cái này nếu bạn cần ID ở client
    // private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("phone")
    private String phone;

    // Thêm các trường khác nếu server trả về
    // @SerializedName("createdAt") 
    // private String createdAt;

    // --- Getters ---
    // (Bạn cần Getters để Gson/Retrofit có thể đọc)

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    // --- Setters (Không bắt buộc, nhưng nên có) ---
    // (Dùng khi bạn muốn cập nhật đối tượng User ở local)

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // ... (Thêm các getters/setters khác nếu cần)
}