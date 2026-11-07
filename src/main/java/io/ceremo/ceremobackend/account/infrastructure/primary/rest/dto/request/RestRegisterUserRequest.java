package io.ceremo.ceremobackend.account.infrastructure.primary.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RestRegisterUserRequest(
    @NotBlank @Size(max = 120) String displayName,
    @NotBlank @Size(max = 40) String termsVersion,
    @NotNull @AssertTrue Boolean termsAccepted
) {

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unknown registration field");
    }
}
