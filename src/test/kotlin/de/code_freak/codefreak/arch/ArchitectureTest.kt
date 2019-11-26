package de.code_freak.codefreak.arch

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.Architectures
import org.junit.runner.RunWith
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.inject.Named
import com.tngtech.archunit.core.importer.ImportOption

@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(packages = ["de.code_freak.codefreak"], importOptions = [ImportOption.DoNotIncludeTests::class])
internal class ArchitectureTest {

  @ArchTest
  val `layer dependencies are respected` = Architectures
      .layeredArchitecture()
      .layer("Frontend").definedBy("de.code_freak.codefreak.frontend..", "de.code_freak.codefreak.graphql..")
      .layer("Auth").definedBy("de.code_freak.codefreak.auth..")
      .layer("Service").definedBy("de.code_freak.codefreak.service..")
      .layer("Persistence").definedBy("de.code_freak.codefreak.repository..")
      .layer("Config").definedBy("de.code_freak.codefreak.config..")
      .whereLayer("Frontend").mayOnlyBeAccessedByLayers("Config")
      .whereLayer("Service").mayOnlyBeAccessedByLayers("Frontend", "Auth", "Config")
      .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Auth", "Config")

  @ArchTest
  val `services are named correctly` = ArchRuleDefinition
      .classes()
      .that()
      .areAnnotatedWith(Service::class.java)
      .should()
      .haveNameMatching(".*Service")

  @ArchTest
  val `transactional methods are public` = ArchRuleDefinition
      .methods()
      .that()
      .areAnnotatedWith(Transactional::class.java)
      .should()
      .bePublic()
      .because("@Transactional on non-public methods is silently ignored")

  @ArchTest
  val `if an annotation exists in Spring and in Java, only one of them is used` = ArchRuleDefinition
      .noMethods()
      .should()
      .beAnnotatedWith(javax.transaction.Transactional::class.java)
      .orShould()
      .beAnnotatedWith(Named::class.java)
}
