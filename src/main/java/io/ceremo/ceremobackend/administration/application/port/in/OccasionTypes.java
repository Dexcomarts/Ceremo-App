package io.ceremo.ceremobackend.administration.application.port.in;

import io.ceremo.ceremobackend.administration.domain.model.OccasionType;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;

public interface OccasionTypes {

    CeremoPage<OccasionType> listActive(CeremoPageable pageable);
}