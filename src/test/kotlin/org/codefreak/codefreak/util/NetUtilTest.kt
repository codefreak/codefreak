package org.codefreak.codefreak.util

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.hasSize
import org.junit.Test

internal class NetUtilTest {
  @Test
  fun tcpConnectionSequence() {
    val tcpConnections = """
  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
   0: 0100007F:A32B 00000000:0000 0A 00000000:00000000 00:00000000 00000000  1000        0 56747 1 000000003007590e 99 0 0 10 0
    """.trimIndent()
    val connections = NetUtil.tcpConnectionSequence(tcpConnections).toList()
    assertThat(connections, hasSize(1))
    assertThat(
        connections,
        contains(
            allOf(
                hasProperty("entryNumber", `is`(0L)),
                hasProperty("localIpv4", `is`(16777343L)),
                hasProperty("localPort", `is`(41771L)),
                hasProperty("remoteIpv4", `is`(0L)),
                hasProperty("remotePort", `is`(0L)),
                hasProperty("connectionState", `is`(NetUtil.ConnectionState.TCP_LISTEN))
            )
        )
    )
  }
}
