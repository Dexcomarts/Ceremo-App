package io.ceremo.ceremobackend.account;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class AccountArchitectureTest {

    @Test
    void keepsFrameworksAndAdaptersOutsideTheCore() {
        var classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.ceremo.ceremobackend");

        noClasses().that().resideInAnyPackage(
                "..account.domain..", "..account.application..",
                "..logout.domain..", "..logout.application..",
                "..tenant.domain..", "..tenant.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..", "jakarta.persistence..", "org.hibernate..",
                "lombok..", "..infrastructure..", "..wire..")
            .check(classes);

        noClasses().that().resideInAPackage("..infrastructure.primary.rest..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..domain.repository..", "jakarta.persistence..", "org.springframework.data..")
            .check(classes);

        noClasses().that().resideInAnyPackage("..account.domain..", "..tenant.domain..", "..logout.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..tenant.application..", "..account.application..", "..logout.application..")
            .check(classes);

        org.assertj.core.api.Assertions.assertThat(classes.stream().map(type -> type.getPackageName()))
            .noneMatch(name -> name.contains(".usermanagement"));
    }
}
