package org.codefreak.codefreak.arch

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.Architectures
import javax.inject.Named
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@AnalyzeClasses(packages = ["org.codefreak.codefreak"], importOptions = [ImportOption.DoNotIncludeTests::class])
internal class ArchitectureTest {

  @ArchTest
  val `layer dependencies are respected` = Architectures
      .layeredArchitecture()
      .layer("Frontend").definedBy("org.codefreak.codefreak.frontend..", "org.codefreak.codefreak.graphql..")
      .layer("Auth").definedBy("org.codefreak.codefreak.auth..")
      .layer("Service").definedBy("org.codefreak.codefreak.service..")
      .layer("Persistence").definedBy("org.codefreak.codefreak.repository..")
      .layer("Config").definedBy("org.codefreak.codefreak.config..")
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

  @ArchTest
  val `List is not used as type for entity relations` = ArchRuleDefinition
      .noFields()
      .that()
      .areAnnotatedWith(OneToMany::class.java)
      .or()
      .areAnnotatedWith(ManyToMany::class.java)
      .should()
      .haveRawType(List::class.java)
}
