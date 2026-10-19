package com.aadi.sandesh.dto.event;


import jakarta.validation.constraints.NotNull;

public record TypingEvent(@NotNull Boolean typing) {
}