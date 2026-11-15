package io.ceremo.ceremobackend.administration.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import java.util.UUID;

public record OccasionType(
    UUID id,
    String code,
    String name,
    String description,
    boolean active
) {
    public OccasionType {
        Assert.notNull("id", id);
        Assert.field("code", code).notBlank().maxLength(60);
        Assert.field("name", name).notBlank().maxLength(120);
        Assert.field("description", description).maxLength(1000);
    }
}
