package bubblewrap

import java.net.URL
import java.util.concurrent.TimeUnit

import io.netty.handler.ssl.{SslContextBuilder, SslProvider}
import io.netty.util.HashedWheelTimer
import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig, Realm}
import io.netty.handler.codec.http.HttpHeaders.Names._
import io.netty.handler.codec.http.HttpHeaders.Values._
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.asynchttpclient.cookie.Cookie

import scala.collection.JavaConverters._

class HttpClient(clientSettings:ClientSettings = new ClientSettings()) {
  val lenientSSLContext = SslContextBuilder.forClient().sslProvider(SslProvider.JDK).trustManager(InsecureTrustManagerFactory.INSTANCE).build()

  val timer = new HashedWheelTimer(100,TimeUnit.MILLISECONDS,2)
  val client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                                      .setConnectTimeout(clientSettings.connectionTimeout)
                                      .setRequestTimeout(clientSettings.requestTimeout)
                                      .setReadTimeout(clientSettings.readTimeout)
                                      .setSslContext(lenientSSLContext)
                                      .setAcceptAnyCertificate(true)
                                      .setMaxRequestRetry(clientSettings.retries)
                                      .setFollowRedirect(false)
                                      .setNettyTimer(timer).build())


  def get(url: WebUrl, config:CrawlConfig) = {
    val handler = new HttpHandler(config, url)
    val request = client.prepareGet(url.toString)
    config.proxy.foreach{
      case PlainProxy(host, port) => request.setProxyServer(new ProxyServer.Builder(host,port).build())
      case proxy@ProxyWithAuth(host, port, user, pass, scheme) => {
        val proxyServerBuilder = new ProxyServer.Builder(host,port)
        val realm = new Realm.Builder(user,pass).setScheme(scheme.toNingScheme).setUsePreemptiveAuth(true).build()
        proxyServerBuilder.setRealm(realm)
        request.setProxyServer(proxyServerBuilder.build())
        request.setRealm(realm)
      }
    }
    if(!config.customHeaders.headers.contains(ACCEPT_ENCODING))
      request.addHeader(ACCEPT_ENCODING, GZIP)

    request
      .addHeader(USER_AGENT, config.userAgent)
      .setCookies(HttpClient.cookies(config,url.toString).asJava)

    config.customHeaders.headers.foreach(header => request.addHeader(header._1, header._2))
    request.execute(handler)
    handler.httpResponse.future
  }
}

object HttpClient {
  val oneYear = 360l * 24 * 60 * 60 * 1000
  def cookies(config:CrawlConfig, url:String) = {
    config.cookies.cookies
      .map(cookie => Cookie.newValidCookie(cookie._1,cookie._2, false, host(url),"/", oneYear, url.startsWith("https"),true))
      .toList
  }
  def host(url:String) = new URL(url).getHost
}
