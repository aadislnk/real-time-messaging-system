package com.aadi.sandesh.dto.usersafety;


import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BlockUserRequest(@NotNull UUID userId) {
}