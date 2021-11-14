import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * X509 trust manager that performs no chain validation at all.
 * Should only ever be used for testing
 */
private class InsecureTrustManager : X509TrustManager {
  override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
  override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
  override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

fun OkHttpClient.Builder.enableInsecureTls() {
  val trustAllCertsManager = InsecureTrustManager()
  val sslContext: SSLContext = SSLContext.getInstance("SSL")
  sslContext.init(null, arrayOf(trustAllCertsManager), SecureRandom())
  sslSocketFactory(sslContext.socketFactory, trustAllCertsManager)
  hostnameVerifier { _, _ -> true }
}

/**
 * Add the given header to all requests that the client will perform.
 */
fun OkHttpClient.Builder.addHeader(headerName: String, headerValue: String) {
  addInterceptor { chain ->
    val authReq: Request = chain
      .request()
      .newBuilder()
      .addHeader(headerName, headerValue)
      .build()
    chain.proceed(authReq)
  }
}
