package com.topsion.rag.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackVM(
    @NotBlank
    @Size(min = 1, max = 500)
    String feedback
) {}