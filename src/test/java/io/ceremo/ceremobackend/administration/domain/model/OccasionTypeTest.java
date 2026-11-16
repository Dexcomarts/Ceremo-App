package io.ceremo.ceremobackend.administration.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;
import io.ceremo.ceremobackend.shared.error.domain.StringTooLongException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OccasionTypeTest {

    private static final UUID ID =
        UUID.fromString("019a67b0-0000-7000-8000-000000000001");

    @Test
    void preservesTheOccasionDefinition() {
        OccasionType occasion = new OccasionType(
            ID, "WEDDING", "Wedding", "Marriage celebrations", true
        );

        assertAll(
            () -> assertEquals(ID, occasion.id()),
            () -> assertEquals("WEDDING", occasion.code()),
            () -> assertEquals("Wedding", occasion.name()),
            () -> assertEquals(
                "Marriage celebrations", occasion.description()
            ),
            () -> assertTrue(occasion.active())
        );
    }

    @Test
    void acceptsAnInactiveOccasionWithoutADescription() {
        OccasionType occasion =
            new OccasionType(ID, "FESTIVAL", "Festival", null, false);

        assertNull(occasion.description());
        assertFalse(occasion.active());
    }

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(
            MissingMandatoryValueException.class,
            () -> new OccasionType(
                null, "WEDDING", "Wedding", null, true
            )
        );

        for (String value : new String[] {null, "", " "}) {
            assertThrows(
                MissingMandatoryValueException.class,
                () -> new OccasionType(
                    ID, value, "Wedding", null, true
                )
            );

            assertThrows(
                MissingMandatoryValueException.class,
                () -> new OccasionType(
                    ID, "WEDDING", value, null, true
                )
            );
        }
    }

    @Test
    void acceptsValuesAtTheirLengthLimits() {
        assertDoesNotThrow(
            () -> new OccasionType(
                ID,
                "C".repeat(60),
                "N".repeat(120),
                "D".repeat(1000),
                true
            )
        );
    }

    @Test
    void rejectsValuesAboveTheirLengthLimits() {
        String longCode = "C".repeat(61);
        String longName = "N".repeat(121);
        String longDescription = "D".repeat(1001);
        assertAll(
            () -> assertThrows(
                StringTooLongException.class,
                () -> new OccasionType(
                    ID, longCode, "Wedding", null, true
                )
            ),
            () -> assertThrows(
                StringTooLongException.class,
                () -> new OccasionType(
                    ID, "WEDDING", longName, null, true
                )
            ),
            () -> assertThrows(
                StringTooLongException.class,
                () -> new OccasionType(
                    ID, "WEDDING", "Wedding", longDescription, true
                )
            )
        );
    }
}