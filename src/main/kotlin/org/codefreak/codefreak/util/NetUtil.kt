package org.codefreak.codefreak.util

object NetUtil {
  private val TCP_CONNECTION_REGEX = Regex("^\\s*(\\d+):\\s+(\\p{XDigit}+:\\p{XDigit}+)\\s+(\\p{XDigit}+:\\p{XDigit}+)\\s+(\\p{XDigit}+)")

  /**
   * https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/tree/include/net/tcp_states.h
   */
  enum class ConnectionState {
    TCP_ESTABLISHED,
    TCP_SYN_SENT,
    TCP_SYN_RECV,
    TCP_FIN_WAIT1,
    TCP_FIN_WAIT2,
    TCP_TIME_WAIT,
    TCP_CLOSE,
    TCP_CLOSE_WAIT,
    TCP_LAST_ACK,
    TCP_LISTEN,
    TCP_CLOSING,
    TCP_NEW_SYN_RECV;

    companion object {
      fun fromInt(value: Int) = values()[value - 1]
    }
  }

  /**
   * https://www.kernel.org/doc/html/latest/networking/proc_net_tcp.html
   * This data class does not contain all fields but the most interesting ones
   * For the meaning of [connectionState] check
   * https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/tree/include/net/tcp_states.h
   */
  data class TcpConnection(
    val entryNumber: Long,
    val localIpv4: Long,
    val localPort: Long,
    val remoteIpv4: Long,
    val remotePort: Long,
    val connectionState: ConnectionState
  )

  /**
   * Parse the full content of /proc/net/tcp into a sequence of [TcpConnection] instances
   */
  fun tcpConnectionSequence(lines: String): Sequence<TcpConnection> =
      lines.splitToSequence('\n').mapIndexedNotNull { index, line ->
        try {
          parseTcpConnection(line)
        } catch (e: IllegalArgumentException) {
          // first line will contain the header with labels
          if (index != 0) {
            throw e
          }
          null
        }
      }

  /**
   * Parse a line from /proc/net/tcp into [TcpConnection] instance
   */
  private fun parseTcpConnection(line: String): TcpConnection {
    val matches = TCP_CONNECTION_REGEX.find(line)?.groupValues
        ?: throw IllegalArgumentException("Provided string is not a line from /proc/net/tcp")
    val (localIp, localPort) = parseHexIpAndPort(matches[2])
    val (remoteIp, remotePort) = parseHexIpAndPort(matches[3])
    return TcpConnection(
        matches[1].toLong(),
        localIp,
        localPort,
        remoteIp,
        remotePort,
        ConnectionState.fromInt(matches[4].toInt(16))
    )
  }

  /**
   * Convert an IP:PORT combination from hex into a decimal (IP, PORT) Pair
   */
  private fun parseHexIpAndPort(value: String): Pair<Long, Long> {
    val parts = value.split(':')
    if (parts.size != 2) {
      throw IllegalArgumentException("Expected exactly one ':' in string. Found ${parts.size - 1}.")
    }
    return Pair(
        parts[0].toLong(16),
        parts[1].toLong(16)
    )
  }
}
