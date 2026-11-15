package io.ceremo.ceremobackend.administration.domain.repository;

import io.ceremo.ceremobackend.administration.domain.model.OccasionType;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;

public interface OccasionTypeRepository {

    /**
     * Returns active occasion types ordered by name, then id.
     * Applies pagination in the database.
     * The total count includes only active occasion types.
     */
    CeremoPage<OccasionType> findActive(CeremoPageable pageable);
}