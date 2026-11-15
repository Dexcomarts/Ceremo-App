package io.ceremo.ceremobackend.administration.application;

import io.ceremo.ceremobackend.administration.application.port.in.OccasionTypes;
import io.ceremo.ceremobackend.administration.domain.model.OccasionType;
import io.ceremo.ceremobackend.administration.domain.repository.OccasionTypeRepository;
import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.util.Objects;

public final class OccasionTypeService implements OccasionTypes {

    private final OccasionTypeRepository occasionTypes;

    public OccasionTypeService(OccasionTypeRepository occasionTypes) {
        this.occasionTypes =
            Objects.requireNonNull(occasionTypes, "occasionTypes");
    }

    @Override
    public CeremoPage<OccasionType> listActive(CeremoPageable pageable) {
        Assert.notNull("pageable", pageable);

        return Objects.requireNonNull(
            occasionTypes.findActive(pageable),
            "OccasionTypeRepository must return a page"
        );
    }
}