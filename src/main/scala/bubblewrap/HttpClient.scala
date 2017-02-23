package bubblewrap

import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.{HostnameVerifier, SSLContext, SSLSession, X509TrustManager}

import com.ning.http.client._
import com.ning.http.client.cookie.Cookie
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}

import scala.collection.JavaConverters._

class HttpClient(clientSettings:ClientSettings = new ClientSettings()) {
  val lenientSSLContext: SSLContext = {
    val context = SSLContext.getInstance("SSL")
    context.init(null, Array(new X509TrustManager {
      override def checkClientTrusted(chain:Array[X509Certificate], authType:String) {}
      override def checkServerTrusted(chain:Array[X509Certificate], authType:String) {}
      override def getAcceptedIssuers = null
    }
    ), null)
    context
  }

  val allHostsValid = new HostnameVerifier {
    override def verify(s: String, sslSession: SSLSession): Boolean = true
  }
  
  val client = new AsyncHttpClient(new AsyncHttpClientConfigBean()
                                      .setConnectionTimeOut(clientSettings.connectionTimeout)
                                      .setRequestTimeout(clientSettings.requestTimeout)
                                      .setReadTimeout(clientSettings.readTimeout)
                                      .setReadTimeout(clientSettings.socketTimeout)
                                      .setAllowPoolingConnection(clientSettings.poolConnections)
                                      .setAllowSslConnectionPool(clientSettings.poolConnections)
                                      .setSslContext(lenientSSLContext)
                                      .setAcceptAnyCertificate(true)
                                      .setHostnameVerifier(allHostsValid)
                                      .setMaxRequestRetry(clientSettings.retries)
                                      .setFollowRedirect(false))

  def realmFrom(config: ProxyWithAuth): Realm = {
    new Realm.RealmBuilder()
      .setPrincipal(config.user)
      .setPassword(config.pass)
      .setUsePreemptiveAuth(true)
      .setScheme(config.authScheme.toNingScheme)
      .setTargetProxy(true)
      .build()
  }

  def get(url: WebUrl, config:CrawlConfig) = {
    val handler = new HttpHandler(config, url)
    val request = client.prepareGet(url.toString)
    config.proxy.foreach{
      case PlainProxy(host, port) => request.setProxyServer(new ProxyServer(host,port))
      case proxy@ProxyWithAuth(host, port, user, pass, scheme) => {
        request.setRealm(realmFrom(proxy))
        request.setProxyServer(new ProxyServer(host, port, user, pass))
      }
    }
    if(!config.customHeaders.headers.contains(HttpHeaderNames.ACCEPT_ENCODING.toString))
      request.addHeader(HttpHeaderNames.ACCEPT_ENCODING.toString, HttpHeaderValues.GZIP.toString)

    request
      .addHeader(HttpHeaderNames.USER_AGENT.toString, config.userAgent)
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
