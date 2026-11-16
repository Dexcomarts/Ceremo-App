package io.ceremo.ceremobackend.administration.application;

import io.ceremo.ceremobackend.administration.application.port.in.OccasionTypes;
import io.ceremo.ceremobackend.administration.domain.model.OccasionType;
import io.ceremo.ceremobackend.administration.domain.repository.OccasionTypeRepository;
import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OccasionTypeServiceTest {

    private final OccasionTypeRepository repository =
        mock(OccasionTypeRepository.class);

    private final OccasionTypes service =
        new OccasionTypeService(repository);

    private final CeremoPageable firstPage =
        new CeremoPageable(0, 10);

    @Test
    void returnsTheRequestedPageWithoutChangingItsContentOrMetadata() {
        CeremoPageable requestedPage = new CeremoPageable(1, 1);

        OccasionType wedding = new OccasionType(
            UUID.fromString("019a67b0-0000-7000-8000-000000000001"),
            "WEDDING",
            "Wedding",
            null,
            true
        );

        CeremoPage<OccasionType> expected =
            CeremoPage.builder(List.of(wedding))
                .currentPage(1)
                .pageSize(1)
                .totalElementsCount(3)
                .build();

        when(repository.findActive(requestedPage)).thenReturn(expected);

        assertSame(expected, service.listActive(requestedPage));
        verify(repository).findActive(requestedPage);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void returnsAnEmptyCataloguePage() {
        CeremoPage<OccasionType> expected =
            CeremoPage.<OccasionType>builder(List.of())
                .currentPage(0)
                .pageSize(10)
                .totalElementsCount(0)
                .build();

        when(repository.findActive(firstPage)).thenReturn(expected);

        assertSame(expected, service.listActive(firstPage));
        verify(repository).findActive(firstPage);
    }

    @Test
    void rejectsMissingPagingBeforeAccessingTheRepository() {
        assertThrows(
            MissingMandatoryValueException.class,
            () -> service.listActive(null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void propagatesRepositoryFailureInsteadOfReturningAnEmptyPage() {
        IllegalStateException failure =
            new IllegalStateException("Repository unavailable");

        when(repository.findActive(firstPage)).thenThrow(failure);

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> service.listActive(firstPage)
        );

        assertSame(failure, thrown);
        verify(repository).findActive(firstPage);
    }

    @Test
    void rejectsAMissingRepository() {
        assertThrows(
            NullPointerException.class,
            () -> new OccasionTypeService(null)
        );
    }

    @Test
    void rejectsANullRepositoryResult() {
        when(repository.findActive(firstPage)).thenReturn(null);

        NullPointerException failure = assertThrows(
            NullPointerException.class,
            () -> service.listActive(firstPage)
        );

        assertEquals(
            "OccasionTypeRepository must return a page",
            failure.getMessage()
        );
    }
}