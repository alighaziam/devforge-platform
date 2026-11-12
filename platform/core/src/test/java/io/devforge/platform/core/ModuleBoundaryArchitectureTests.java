package io.devforge.platform.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "io.devforge.platform.core",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryArchitectureTests {

    @ArchTest
    static final ArchRule logicalModulesMustBeAcyclic =
            slices().matching("io.devforge.platform.core.(*)..")
                    .should().beFreeOfCycles();
}
