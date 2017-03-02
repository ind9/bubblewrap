package bubblewrap

import org.asynchttpclient.HttpResponseHeaders

import scala.collection.JavaConversions._


class ResponseHeaders(httpResponseHeaders: HttpResponseHeaders) {
  val Charset = ".*charset\\s*=\\s*\\\"?([^;\\\"\\s]+)\\\"?\\s*".r

  def exceedsSize(size:Long) = httpResponseHeaders.getHeaders.getAll("Content-Length").exists(_.toLong > size)
  def redirectLocation = httpResponseHeaders.getHeaders.getAll("Location").headOption
  def contentCharset = contentType.collect {
    case Charset(enc) => enc
  }
  def contentType = httpResponseHeaders.getHeaders.getAll("Content-Type").headOption
  def contentEncoding = httpResponseHeaders.getHeaders.getAll("Content-Encoding").headOption
  def headers:Map[String,List[String]] = httpResponseHeaders.getHeaders.iterator().map(t => t.getKey -> httpResponseHeaders.getHeaders.getAll(t.getKey).toList).toMap
}