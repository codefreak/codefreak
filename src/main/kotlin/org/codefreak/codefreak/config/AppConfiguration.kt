package org.codefreak.codefreak.config

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

  /** Identifier of the Code FREAK instanceId. */
  lateinit var instanceId: String
  var authenticationMethod = AuthenticationMethod.SIMPLE

  val workspaces = Workspaces()
  val ldap = Ldap()
  val files = Files()
  val lti = Lti()
  val evaluation = Evaluation()
  val gitImport = GitImport()

  class Workspaces {
    /**
     * Kubernetes namespace where new workspaces will be created in.
     * Running multiple instances of Code FREAK in the same namespace is NOT supported and might lead
     * to data corruption or invalid states.
     */
    var namespace = "default"

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

    /**
     * Limit for ephemeral storage of the workspace. Must be a valid value for Kubernetes `spec.containers[].resources.limits.ephemeral-storage`.
     *
     * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers">Kubernetes | Managing Resources for Containers</a>
     */
    var diskLimit = "5Gi"

    var ingress = Ingress()

    class Ingress {
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
       * Enable TLS on the ingress object. To generate https workspace URLs you should
       * also adjust the `baseUrlTemplate` to start with `https://...`. If you have
       * a valid (wildcard) certificate for the workspace hosts please store it in a secret
       * and point `tlsSecretName` to the proper secret.
       */
      var tlsEnabled = false

      /**
       * Name of the tls secret object that should be attached to workspace ingresses.
       * Will only be used if `tlsEnabled` is set to `true`.
       */
      var tlsSecretName: String? = null

      /**
       * Disable certificate validation when the backend is connecting to a workspace.
       * This should only ever be used for testing and is highly insecure in production environments
       * as it allows MITM attacks!!
       */
      var disableTlsVerification = false
    }
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
    var maxConcurrentExecutions = Runtime.getRuntime().availableProcessors()
    var maxQueueSize = 1000
    var defaultTimeout = 5L * 60L
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
}
