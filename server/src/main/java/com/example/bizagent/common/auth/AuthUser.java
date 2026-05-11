package com.example.bizagent.common.auth;

import java.util.List;

public record AuthUser(Long id, String username, List<String> permissions) {
}
