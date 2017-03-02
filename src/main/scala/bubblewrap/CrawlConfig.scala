package bubblewrap

import org.asynchttpclient.Realm

case class CrawlConfig(proxy:Option[Proxy], userAgent:String, maxSize: Long, minSize: Long, cookies:Cookies = Cookies None, customHeaders: RequestHeaders = RequestHeaders None)
case class RequestHeaders(headers: Map[String,String]) {
  def +(header:(String,String)) = this.copy(headers + header)
}
object RequestHeaders {
  def None = RequestHeaders(Map.empty)
}
case class ClientSettings(socketTimeout:Int = 20000, connectionTimeout:Int = 30000, requestTimeout:Int = 60000,
                          readTimeout:Int = 60000, maxConnectionPerHost:Int = 16,
                          maxTotalConnections:Int = 128, poolConnections:Boolean = true, retries:Int = 0)
case class Cookies(cookies:Map[String,String])
object Cookies {
  def None = Cookies(Map.empty)
}

trait PageResponse

case class SuccessResponse(page: Content) extends PageResponse
case class FailureResponse(error: Throwable) extends PageResponse
case class ExceededSize(size:Long) extends RuntimeException(s"Exceeds allowed max size ${size}")


case class HttpResponse(status: Int, pageResponse: PageResponse, headers: ResponseHeaders , responseTime: Long) {
  def redirectLocation = headers.redirectLocation
  def contentType = headers.contentType
}

sealed trait AuthScheme {
  def toNingScheme: Realm.AuthScheme
}
case object BASIC extends AuthScheme {
  def toNingScheme = Realm.AuthScheme.BASIC
}
case object NTLM extends AuthScheme {
  def toNingScheme = Realm.AuthScheme.NTLM
}

object AuthScheme {
  def apply(value: String): AuthScheme = if(value == "ntlm") NTLM else BASIC
}


sealed trait Proxy
case class PlainProxy(host:String, port:Int) extends Proxy
case class ProxyWithAuth(host:String, port:Int, user:String, pass:String, authScheme: AuthScheme) extends Proxy

object Proxy {
  def apply(host:String, port:Int) = PlainProxy(host, port)
  def apply(host:String, port:Int, user:String, pass:String) = ProxyWithAuth(host, port, user, pass, BASIC)
  def apply(host:String, port:Int, user:String, pass:String, authScheme: AuthScheme) = ProxyWithAuth(host, port, user, pass, authScheme)
}




