package com.myblog.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.myblog", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule applicationServicesMustNotFormCycles = slices()
            .matching("com.myblog.application.service.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule applicationCoreMustNotDependOnInfrastructure = noClasses()
            .that().resideInAnyPackage(
                    "com.myblog.application.service..",
                    "com.myblog.application.port..",
                    "com.myblog.application.repository..")
            .should().dependOnClassesThat().resideInAPackage("com.myblog.infrastructure..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnDeliveryOrStarter = noClasses()
            .that().resideInAPackage("com.myblog.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.controller..",
                    "com.myblog.starter..");

    @ArchTest
    static final ArchRule commonMustNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("com.myblog.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.application..",
                    "com.myblog.controller..",
                    "com.myblog.infrastructure..",
                    "com.myblog.starter..");

    @ArchTest
    static final ArchRule infrastructureMustNotDependOnDeliveryOrStarter = noClasses()
            .that().resideInAPackage("com.myblog.infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.controller..",
                    "com.myblog.starter..");

    @ArchTest
    static final ArchRule controllersMustNotDependOnInfrastructureOrStarter = noClasses()
            .that().resideInAPackage("com.myblog.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.infrastructure..",
                    "com.myblog.starter..");

    @ArchTest
    static final ArchRule starterMustNotDependOnControllers = noClasses()
            .that().resideInAPackage("com.myblog.starter..")
            .should().dependOnClassesThat().resideInAPackage("com.myblog.controller..");
}
