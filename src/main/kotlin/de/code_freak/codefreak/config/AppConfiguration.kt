package de.code_freak.codefreak.config

import de.code_freak.codefreak.auth.AuthenticationMethod
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration("config")
@ConfigurationProperties(prefix = "code-freak")
class AppConfiguration {

  /** Identifier of the Code FREAK instanceId. Set this if you run multiple instances on the same Docker host. */
  lateinit var instanceId: String
  var authenticationMethod = AuthenticationMethod.SIMPLE

  val docker = Docker()
  val ide = Ide()
  val traefik = Traefik()
  val latex = Latex()
  val frontend = Frontend()

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
  }

  class Docker {
    lateinit var host: String
    lateinit var certPath: String
    lateinit var caCertPath: String
    lateinit var clientKeyPath: String
    lateinit var clientCertPath: String

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
     * Define how images will be pulled on application startup (inspired by Gitlab Runner)
     * - never = Images must be already present on the docker daemon or container creation will fail
     * - if-not-present = Pull images if no version is available
     * - always = Always pull image (may override existing ones)
     */
    lateinit var pullPolicy: String
  }

  class Ldap {
    var url: String? = null
  }
}
