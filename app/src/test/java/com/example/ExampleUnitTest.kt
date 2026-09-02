package com.example

import com.example.model.AppLanguage
import com.example.model.ConnectionConfig
import com.example.model.HermesStrings
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testRemoteGatewayUrlCalculation_customUrl() {
    val config = ConnectionConfig(
      tailscaleIp = "100.84.12.93",
      port = 8080,
      remoteGatewayUrl = "https://gateway.example.com/api/",
      useCustomGatewayUrl = true
    )
    assertEquals("https://gateway.example.com/api", config.effectiveGatewayUrl)
  }

  @Test
  fun testRemoteGatewayUrlCalculation_tailscaleIp() {
    val configHttp = ConnectionConfig(
      tailscaleIp = "100.84.12.93",
      port = 8080,
      useCustomGatewayUrl = false,
      useHttps = false
    )
    assertEquals("http://100.84.12.93:8080", configHttp.effectiveGatewayUrl)

    val configHttps = configHttp.copy(useHttps = true)
    assertEquals("https://100.84.12.93:8080", configHttps.effectiveGatewayUrl)
  }

  @Test
  fun testHermesStringsLocalization() {
    val enTitle = HermesStrings.remoteGatewayModeTitle(AppLanguage.EN)
    val arTitle = HermesStrings.remoteGatewayModeTitle(AppLanguage.AR)
    assertNotEquals(enTitle, arTitle)
    assertTrue(enTitle.contains("Remote Gateway"))
    assertTrue(arTitle.contains("البوابة عن بعد"))
  }
}
