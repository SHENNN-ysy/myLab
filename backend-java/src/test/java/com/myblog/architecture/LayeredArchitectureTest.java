package com.myblog.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** ArchUnit 分层架构规则：校验各层依赖方向与应用服务无循环依赖。 */
@AnalyzeClasses(packages = "com.myblog", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    /** 各业务模块的应用服务之间不得形成循环依赖。 */
    @ArchTest
    static final ArchRule applicationServicesMustNotFormCycles = slices()
            .matching("com.myblog.application.service.(*)..")
            .should().beFreeOfCycles();

    /** 应用核心（service/port/repository）不得依赖 infrastructure 适配层。 */
    @ArchTest
    static final ArchRule applicationCoreMustNotDependOnInfrastructure = noClasses()
            .that().resideInAnyPackage(
                    "com.myblog.application.service..",
                    "com.myblog.application.port..",
                    "com.myblog.application.repository..")
            .should().dependOnClassesThat().resideInAPackage("com.myblog.infrastructure..");

    /** 应用层不得反向依赖 controller 交付层与 starter 装配层。 */
    @ArchTest
    static final ArchRule applicationMustNotDependOnDeliveryOrStarter = noClasses()
            .that().resideInAPackage("com.myblog.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.controller..",
                    "com.myblog.starter..");

    /** common 公共层不得依赖任何外层（application/controller/infrastructure/starter）。 */
    @ArchTest
    static final ArchRule commonMustNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("com.myblog.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.application..",
                    "com.myblog.controller..",
                    "com.myblog.infrastructure..",
                    "com.myblog.starter..");

    /** infrastructure 适配层不得依赖 controller 交付层与 starter 装配层。 */
    @ArchTest
    static final ArchRule infrastructureMustNotDependOnDeliveryOrStarter = noClasses()
            .that().resideInAPackage("com.myblog.infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.controller..",
                    "com.myblog.starter..");

    /** controller 交付层只做协议转换，不得依赖 infrastructure 与 starter。 */
    @ArchTest
    static final ArchRule controllersMustNotDependOnInfrastructureOrStarter = noClasses()
            .that().resideInAPackage("com.myblog.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.myblog.infrastructure..",
                    "com.myblog.starter..");

    /** starter 装配层不得依赖 controller，避免装配层反向耦合交付层。 */
    @ArchTest
    static final ArchRule starterMustNotDependOnControllers = noClasses()
            .that().resideInAPackage("com.myblog.starter..")
            .should().dependOnClassesThat().resideInAPackage("com.myblog.controller..");
}
