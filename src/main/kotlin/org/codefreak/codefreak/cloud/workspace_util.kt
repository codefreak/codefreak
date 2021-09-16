package org.codefreak.codefreak.cloud

import java.net.URI
import org.codefreak.codefreak.util.withoutLeadingSlash
import org.codefreak.codefreak.util.withoutTrailingSlash
import org.springframework.web.util.UriComponentsBuilder

fun buildWorkspaceUri(workspaceBaseUrl: String, path: String? = null, query: String? = null, websocket: Boolean = false): String {
  val baseUri = URI.create(workspaceBaseUrl)
  val scheme = if (websocket) {
    if (baseUri.scheme == "https") "wss" else "ws"
  } else {
    baseUri.scheme
  }
  return UriComponentsBuilder.newInstance()
      .scheme(scheme)
      .host(baseUri.authority)
      .joinPath(baseUri.path, path)
      .query(query)
      .build()
      .encode()
      .toUriString()
}

private fun UriComponentsBuilder.joinPath(vararg paths: String?): UriComponentsBuilder {
  path(paths.filterNotNull().joinToString(separator = "/") { it.withoutLeadingSlash().withoutTrailingSlash() })
  return this
}
