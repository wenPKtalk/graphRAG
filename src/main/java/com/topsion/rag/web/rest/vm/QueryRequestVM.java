package com.topsion.rag.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequestVM(
    @NotBlank
    @Size(min = 1, max = 1000)
    String question,
    
    String sessionId
) {}