package com.lifetracker.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Clean Architecture as a failing build, not a folder convention.
 *
 * Lives in :infrastructure because that module depends on application -> domain,
 * so the whole graph is on the test classpath here.
 */
@AnalyzeClasses(
        packages = "com.lifetracker",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    // ---------- Layering ----------

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            // Domain and Application are still empty while the codebase is greenfield.
            // Tolerate empty layers so the boundary check stays green until code lands;
            // the access rules below still fire the moment a layer has classes.
            .withOptionalLayers(true)
            .layer("Domain").definedBy("com.lifetracker.domain..")
            .layer("Application").definedBy("com.lifetracker.application..")
            .layer("Infrastructure").definedBy("com.lifetracker.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domain_has_no_framework = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "com.fasterxml.jackson..",
                    "tools.jackson..",            // Jackson 3 moved packages
                    "liquibase..",
                    "..infrastructure.."
            )
            .because("the domain is plain Java. The module graph should already make this "
                    + "impossible; this fires if someone adds the dependency to fix a "
                    + "compile error");

    @ArchTest
    static final ArchRule application_has_no_web_or_persistence = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.web..",
                    "..infrastructure.."
            )
            .because("use cases talk to ports, not to HTTP or Hibernate");

    // ---------- Money ----------

    @ArchTest
    static final ArchRule no_floating_point_fields = noFields()
            .should().haveRawType(Double.class)
            .orShould().haveRawType(double.class)
            .orShould().haveRawType(Float.class)
            .orShould().haveRawType(float.class)
            .because("floating point is never correct for currency, including in DTOs at "
                    + "the boundary");

    @ArchTest
    static final ArchRule no_floating_point_returns = noMethods()
            .should().haveRawReturnType(Double.class)
            .orShould().haveRawReturnType(double.class)
            .orShould().haveRawReturnType(Float.class)
            .orShould().haveRawReturnType(float.class)
            .because("a double leaving a method is a double entering somewhere else");

    @ArchTest
    static final ArchRule bigdecimal_stays_inside_money = noClasses()
            .that().resideInAPackage("..domain..")
            .and().doNotHaveSimpleName("Money")
            .should().dependOnClassesThat().haveFullyQualifiedName("java.math.BigDecimal")
            .because("BigDecimal is an implementation detail of Money. A raw BigDecimal "
                    + "field on an aggregate bypasses every guarantee Money makes");

    // ---------- Persistence isolation ----------

    @ArchTest
    static final ArchRule jpa_entities_stay_in_persistence = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule entities_do_not_leak = noClasses()
            .that().resideOutsideOfPackage("..infrastructure.persistence..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
            .because("entities are a storage detail. Map to domain types at the boundary, "
                    + "or a Hibernate lazy proxy ends up in your business logic");

    // ---------- Ports ----------

    @ArchTest
    static final ArchRule repository_ports_are_interfaces = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().resideInAPackage("..domain..")
            .should().beInterfaces()
            .because("the domain owns the port; infrastructure owns the adapter");
}