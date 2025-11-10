// java
package com.example.nt118_englishvocabapp.network;

// Dùng Gson để nó bỏ qua các trường null khi gửi JSON
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {

    @SerializedName("currentPassword")
    @Expose
    private String currentPassword;

    @SerializedName("newUsername")
    @Expose
    private String newUsername; // Sẽ là null nếu không thay đổi

    @SerializedName("newPassword")
    @Expose
    private String newPassword; // Sẽ là null nếu không thay đổi

    // Constructor để cập nhật cả hai
    public UpdateProfileRequest(String currentPassword, String newUsername, String newPassword) {
        this.currentPassword = currentPassword;
        this.newUsername = (newUsername != null && !newUsername.isEmpty()) ? newUsername : null;
        this.newPassword = (newPassword != null && !newPassword.isEmpty()) ? newPassword : null;
    }

    // (Bạn có thể thêm các constructor khác nếu chỉ muốn đổi 1 thứ)
}