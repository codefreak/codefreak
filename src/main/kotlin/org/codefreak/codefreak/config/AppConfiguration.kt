package org.codefreak.codefreak.config

import java.time.ZoneId
import java.util.Locale
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import org.codefreak.codefreak.auth.AuthenticationMethod
import org.codefreak.codefreak.auth.Role
import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component("config")
@ConfigurationProperties(prefix = "codefreak")
@Validated
class AppConfiguration {

  /** Identifier of the Code FREAK instanceId. Set this if you run multiple instances on the same Docker host. */
  lateinit var instanceId: String
  var authenticationMethod = AuthenticationMethod.SIMPLE

  val l10n = L10N()
  val docker = Docker()
  val workspaces = Workspaces()
  val ide = Ide()
  val reverseProxy = ReverseProxy()
  val ldap = Ldap()
  val files = Files()
  val lti = Lti()
  val evaluation = Evaluation()
  val gitImport = GitImport()

  enum class ReverseProxyType {
    TRAEFIK, PUBLISH
  }

  class ReverseProxy {
    var type: ReverseProxyType = ReverseProxyType.PUBLISH
    /** Base URL for IDE containers */
    var url: String = "http://localhost"
  }

  class Ide {
    /** The Docker image name (and tag) to be used for the IDE containers. */
    lateinit var image: String

    /** Interval for checking for idle containers in ms. */
    var idleCheckRate: Long = 60000L

    /** Time span after which an idle container is shut down in ms. */
    var idleShutdownThreshold: Long = 600000L

    /** Interval for checking shutdown containers for removal in ms. (default is 15min) */
    var removeCheckRate: Long = 60L * 15L * 1000L

    /** Time span after which a shutdown container will be removed in ms. (default is 30 days) */
    var removeThreshold: Long = 60L * 60L * 24L * 30L * 1000L

    /** Maximum number of IDE containers that are started at the same time. Negative = unlimited. */
    var maxContainers = -1

    /** HTTP port exposed by IDE container */
    var httpPort = "3000"

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

    /**
     * List of IDE image names that will receive the docker daemon as mount
     * This should only be used for Breeze
     */
    var dockerDaemonAllowlist = arrayListOf(
        "cfreak/breeze"
    )
  }

  class Workspaces {
    /**
     * Kubernetes namespace where new workspaces will be created in.
     * Running multiple instances of Code FREAK in the same namespace is NOT supported and might lead
     * to data corruption or invalid states.
     */
    var namespace = "default"

    /**
     * Base URL that will be used to create Ingress resources for workspaces.
     * Must accept a single variable `{workspaceIdentifier}` that
     * will be replaced by the actual random workspace id.
     * Make sure the hostname points to your ingress LoadBalancer.
     * The template can also be used to create hostname-based URLs.
     * Make sure you do not exceed the max allowed characters per domain (RFC 1034).
     *
     * ```
     * baseUrlTemplate: "http://{workspaceIdentifier}.ws.mydomain.com"
     * baseUrlTemplate: "https://mydomain.com/ws/{workspaceIdentifier}"
     * ```
     */
    var baseUrlTemplate = "http://localhost/{workspaceIdentifier}"

    /**
     * Full image name that will be used for the workspace companion
     */
    var companionImage = "ghcr.io/codefreak/codefreak-cloud-companion:minimal"

    /**
     * Threshold in seconds after which workspaces without any (websocket) connections
     * are shut down.
     */
    var maxIdleThreshold = 60 * 5L

    /**
     * Interval in seconds how often workspaces are checked for idleness.
     */
    var idleCheckInterval = 20L

    /**
     * Path to a file containing an RSA private key in PEM format.
     * If this and the corresponding public key file is set authentication for workspaces
     * is enabled. Make sure this is a PKCS8 private key. You can convert your existing key
     * to PKCS8 with the following command:
     * ```
     * openssl pkcs8 -topk8 -nocrypt -in privatekey.pem -out privatekey8.pem
     * ```
     */
    var jwtPrivateKeyFile: String? = null

    /**
     * Path to a file containing an RSA public key in PEM format.
     * If this and the corresponding private key file is set authentication for workspaces
     * is enabled.
     */
    var jwtPublicKeyFile: String? = null

    /**
     * The public URL where Code FREAK will expose the JWK set for companions.
     * You only have to adjust the base-url, the path should remain as is (except you do some URL-rewriting).
     *
     * Examples:
     * ```yml
     * jwk-url: https://codefreak.domain.org/.well-known/jwks.json
     * ```
     */
    var jwkUrl = "http://localhost:8080/.well-known/jwks.json"

    /**
     * CPU limit of the workspace. Must be a valid value for Kubernetes `spec.containers[].resources.limits.cpu`.
     *
     * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers">Kubernetes | Managing Resources for Containers</a>
     */
    var cpuLimit = "1"

    /**
     * Memory limit of the workspace. Must be a valid value for Kubernetes `spec.containers[].resources.limits.memory`.
     * Currently, no value below 1Gi is recommended. Otherwise, workspaces may crash unexpectedly.
     *
     * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers">Kubernetes | Managing Resources for Containers</a>
     */
    var memoryLimit = "1Gi"
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
    var overrideRoles: Map<String, Role> = mapOf()
    var forceLdapRoles = true
  }

  class Files {
    var adapter = FileAdapter.JPA
    val fileSystem = FileSystem()

    enum class FileAdapter {
      JPA,
      FILE_SYSTEM
    }

    class FileSystem {
      lateinit var collectionStoragePath: String
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
    var backend = "docker"
    var maxConcurrentExecutions = Runtime.getRuntime().availableProcessors()
    var maxQueueSize = 1000
    var defaultTimeout = 5L * 60L
    // Use IDE image also for evaluation until cloud workspaces have been implemented
    var defaultImage = "cfreak/ide:1"
    var imageWorkdir = "/home/runner/project"
  }

  class GitImport {
    var remotes = arrayOf<GitRemote>()

    class GitRemote {
      var host = ""
      var sshBaseUrl = ""
      var sshKey = ""
      var sshKeyPass: String? = null
    }
  }

  class L10N {
    var dateTimeFormat = "dd.MM.yyyy HH:mm"
    var timeZone: ZoneId = ZoneId.systemDefault()
    var locale: Locale = Locale.getDefault()
  }
}
