package com.example.bizagent.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthTokenService {

    private static final String SECRET = env("BIZAGENT_AUTH_SECRET", "change-me-in-production");
    private static final long EXPIRES_SECONDS = 12 * 60 * 60;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String createToken(AuthUser user) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("uid", user.id());
            payload.put("username", user.username());
            payload.put("permissions", user.permissions());
            payload.put("exp", Instant.now().getEpochSecond() + EXPIRES_SECONDS);
            String body = base64Url(objectMapper.writeValueAsBytes(payload));
            String signature = sign(body);
            return body + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Token生成失败", e);
        }
    }

    public AuthUser parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
                throw new SecurityException("Token签名无效");
            }
            Map<?, ?> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), Map.class);
            long expiresAt = Long.parseLong(String.valueOf(payload.get("exp")));
            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new SecurityException("Token已过期");
            }
            Long userId = Long.valueOf(String.valueOf(payload.get("uid")));
            String username = String.valueOf(payload.get("username"));
            Object rawPermissions = payload.get("permissions");
            List<?> permissionValues = rawPermissions instanceof List<?> list ? list : List.of();
            List<String> permissions = permissionValues.stream()
                    .map(String::valueOf)
                    .toList();
            return new AuthUser(userId, username, permissions);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Token解析失败", e);
        }
    }

    private String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
