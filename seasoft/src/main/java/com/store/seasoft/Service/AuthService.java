package com.store.seasoft.Service;

import com.store.seasoft.Dto.AuthResponse;
import com.store.seasoft.Dto.LoginRequest;
import com.store.seasoft.Dto.RegisterRequest;
import com.store.seasoft.Model.Role;
import com.store.seasoft.Model.User;
import com.store.seasoft.Model.UserStatus;
import com.store.seasoft.Repository.RoleRepository;
import com.store.seasoft.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Đăng ký công khai luôn gán role CUSTOMER, không cho tự chọn ADMIN/STAFF
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Chưa seed role CUSTOMER trong database"));

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(customerRole);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        String token = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(token, user.getEmail(), user.getFullName(), customerRole.getCode());
    }

    public AuthResponse login(LoginRequest request) {
        // Ném AuthenticationException nếu sai email/mật khẩu, được xử lý ở
        // GlobalExceptionHandler
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        String token = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole().getCode());
    }
}