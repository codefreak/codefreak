package de.code_freak.codefreak.config

import de.code_freak.codefreak.auth.AuthenticationMethod
import de.code_freak.codefreak.auth.Role
import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

@Configuration("config")
@ConfigurationProperties(prefix = "code-freak")
@Validated
class AppConfiguration {

  /** Identifier of the Code FREAK instanceId. Set this if you run multiple instances on the same Docker host. */
  lateinit var instanceId: String
  var authenticationMethod = AuthenticationMethod.SIMPLE

  val docker = Docker()
  val ide = Ide()
  val traefik = Traefik()
  val latex = Latex()
  val frontend = Frontend()
  val ldap = Ldap()
  val files = Files()
  val lti = Lti()
  val evaluation = Evaluation()
  val gitImport = GitImport()

  class Frontend {
    /**
     * This can be used to inject custom javascript code (needs to include script tags).
     * It is injected after dependencies (like jQuery) and before template specific scripts.
     */
    lateinit var customScript: String
  }

  class Traefik {
    lateinit var url: String
  }

  class Latex {
    /** The Docker image name (and tag) to be used for the latex container. */
    lateinit var image: String
  }

  class Ide {
    /** The Docker image name (and tag) to be used for the IDE containers. */
    lateinit var image: String

    /** Interval for checking for idle containers in ms. Default 1 minute. */
    var idleCheckRate = 60000L

    /** Time span after which an idle container is shut down. Default 10 minutes. */
    var idleShutdownThreshold = 600000L

    /** Maximum number of IDE containers that are started at the same time. Negative = unlimited. */
    var maxContainers = -1

    /**
     * Memory limit in bytes
     * Equal to --memory-swap in docker run
     * 0 means no limit
     */
    var memory = 0L

    /**
     * Number of CPUs per container
     * Equal to --cpus in docker run
     * 0 means no limit
     */
    var cpus = 0L

    /**
     * Name of the network the container will be attached to
     * Default is the "bridge" network (Docker default)
     */
    lateinit var network: String
  }

  class Docker {
    lateinit var host: String
    lateinit var certPath: String
    lateinit var caCertPath: String
    lateinit var clientKeyPath: String
    lateinit var clientCertPath: String

    /**
     * Define how images will be pulled on application startup (inspired by Gitlab Runner)
     * - never = Images must be already present on the docker daemon or container creation will fail
     * - if-not-present = Pull images if no version is available
     * - always = Always pull image (may override existing ones)
     */
    lateinit var pullPolicy: String
  }

  class Ldap {
    var url: String? = null
    var rootDn: String? = null
    var activeDirectory = false
    var firstNameAttribute: String? = "sn"
    var lastNameAttribute: String? = "givenName"
    var roleMappings: Map<String, Role> = mapOf()
    var userSearchBase = "ou=people"
    var userSearchFilter = "(uid={0})"
    var groupSearchBase = "ou=groups"
    var groupSearchFilter = "member={0}"
    /** Manually set the roles for a specific username */
    var overrideRoles: Map<String, List<Role>> = mapOf()
  }

  class Files {
    var adapter = FileAdapter.JPA

    enum class FileAdapter {
      JPA
    }
  }

  class Lti {
    var enabled: Boolean = false
    @NotNull var keyStore: Resource? = null
    var keyStorePassword: String? = null
    var keyStoreType = "jceks"
    @NotEmpty var providers = arrayListOf<LtiProvider>()

    class LtiProvider {
      var name: String? = null
      @NotBlank var issuer: String? = null
      @NotBlank var clientId: String? = null
      @NotBlank var authUrl: String? = null
      @NotBlank var tokenUrl: String? = null
      @NotBlank var jwkUrl: String? = null
      @NotBlank var keyStoreEntryName: String? = null
      var keyStoreEntryPin = ""
    }
  }

  class Evaluation {
    var maxConcurrentExecutions = 5
    var maxQueueSize = 1000

    val codeclimate = Codeclimate()

    class Codeclimate {
      var image = "cfreak/codeclimate"
    }
  }

  class GitImport {
    var enabled = false
    var remotes = arrayOf<GitRemote>()

    class GitRemote {
      var host = ""
      var sshBaseUrl = ""
      var sshKey = ""
      var sshKeyPass: String? = null
    }
  }
}
