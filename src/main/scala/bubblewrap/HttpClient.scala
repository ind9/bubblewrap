package bubblewrap

import java.net.URL
import java.util.concurrent.TimeUnit

import io.netty.handler.codec.http.HttpHeaders.Names._
import io.netty.handler.codec.http.HttpHeaders.Values._
import io.netty.handler.codec.http.cookie.ClientCookieDecoder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContextBuilder, SslProvider}
import io.netty.util.HashedWheelTimer
import org.asynchttpclient.filter.ThrottleRequestFilter
import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig, Realm}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Promise}

class HttpClient(clientSettings: ClientSettings = ClientSettings()) {
  val lenientSSLContext = SslContextBuilder.forClient().sslProvider(SslProvider.JDK).trustManager(InsecureTrustManagerFactory.INSTANCE).build()

  val timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 100)
  val client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                                          .setConnectTimeout(clientSettings.connectionTimeout)
                                          .setRequestTimeout(clientSettings.requestTimeout)
                                          .setReadTimeout(clientSettings.readTimeout)
                                          .setSslContext(lenientSSLContext)
                                          .setUseInsecureTrustManager(true)
                                          .setMaxRequestRetry(clientSettings.retries)
                                          .setFollowRedirect(false)
                                          .addRequestFilter(new ThrottleRequestFilter(clientSettings.maxTotalConnections, clientSettings.requestThrottleTimeout))
                                          .setKeepAlive(clientSettings.keepAlive).setNettyTimer(timer)
                                          .build())


  def get(url: WebUrl, config: CrawlConfig)(implicit ec: ExecutionContext) = {
    val httpResponse = Promise[HttpResponse]()
    val handler = new HttpHandler(config, url, httpResponse)
    val request = client.prepareGet(url.toString)
    config.proxy.foreach {
      case PlainProxy(host, port) => request.setProxyServer(new ProxyServer.Builder(host, port).build())
      case proxy@ProxyWithAuth(host, port, user, pass, scheme) => {
        val proxyServerBuilder = new ProxyServer.Builder(host, port)
        val realm = new Realm.Builder(user, pass).setScheme(scheme.toNingScheme).setUsePreemptiveAuth(true).build()
        proxyServerBuilder.setRealm(realm)
        request.setProxyServer(proxyServerBuilder.build())
        request.setRealm(realm)
      }
    }
    if (!config.customHeaders.headers.contains(ACCEPT_ENCODING))
      request.addHeader(ACCEPT_ENCODING, GZIP)

    request
      .addHeader(USER_AGENT, config.userAgent)
      .setCookies(HttpClient.cookies(config, url.toString).asJava)
    config.customHeaders.headers.foreach(header => request.addHeader(header._1.trim(), header._2))
    request.execute(handler)
    val responseFuture = httpResponse.future
    // HashedWheelTimer maintains NettyResponseFutures till timeout task reaps them.
    // AsyncHandlers are designed in a way to just store code, but storing HTTPResponse inside handler will bloat up the heap.
    responseFuture.onComplete(_ => handler.httpResponse = null)
    responseFuture
  }

  def shutDown() = {
    client.close()
  }
}

object HttpClient {
  val oneYear = 360l * 24 * 60 * 60 * 1000

  def cookies(config: CrawlConfig, url: String) = {
    config.cookies.cookies
      .map(cookie => {
        val _cookie = ClientCookieDecoder.LAX.decode(s"${cookie._1}=${cookie._2}")
        _cookie.setHttpOnly(true)
        _cookie.setSecure(url.startsWith("https"))
        _cookie.setMaxAge(oneYear)
        _cookie.setWrap(false)
        _cookie.setDomain(host(url))
        _cookie.setPath("/")
        _cookie
      })
      .toList
  }

  def host(url: String) = new URL(url).getHost
}
