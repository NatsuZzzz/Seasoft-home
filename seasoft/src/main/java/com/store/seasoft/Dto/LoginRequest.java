package com.store.seasoft.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    // Khong ep toi thieu (tai khoan cu co the dung mat khau 6 ky tu);
    // gioi han toi da de chan tan cong DoS bang chuoi rat dai qua BCrypt
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(max = 100, message = "Mật khẩu tối đa 100 ký tự")
    private String password;

    // Bo khoang trang thua truoc khi validate @Email
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
