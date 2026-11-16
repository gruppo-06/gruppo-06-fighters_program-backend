package it.fightersprogram;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "it.fightersprogram", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] MODULES =
            {"catalog", "collection", "identity", "economy", "trading", "matches"};

    @ArchTest
    static final ArchRule engineIsPureJava = classes()
            .that().resideInAPackage("it.fightersprogram.engine..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("it.fightersprogram.engine..", "java..");

    @ArchTest
    static final ArchRule commonDoesNotDependOnModules = noClasses()
            .that().resideInAPackage("it.fightersprogram.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "it.fightersprogram.catalog..", "it.fightersprogram.collection..",
                    "it.fightersprogram.identity..", "it.fightersprogram.economy..",
                    "it.fightersprogram.trading..", "it.fightersprogram.matches..");

    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching("it.fightersprogram.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAssignableTo(Repository.class);

    @ArchTest
    static final ArchRule modulesDoNotUseEachOthersInternals = noClasses()
            .should(new InternalsOfOtherModules(MODULES));
}
