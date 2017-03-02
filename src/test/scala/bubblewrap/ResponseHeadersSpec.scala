package bubblewrap

import io.netty.handler.codec.http.DefaultHttpHeaders
import org.asynchttpclient.HttpResponseHeaders
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class ResponseHeadersSpec extends FlatSpec {
  "ResponseHeader" should "read content length header and say whether it exceeds size or not" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders(new DefaultHttpHeaders().add("Content-Length", "10")))

    headers.exceedsSize(10) should be(false)
    headers.exceedsSize(11) should be(false)
    headers.exceedsSize(9) should be(true)
  }

  it should "read redirect location and give back" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders(new DefaultHttpHeaders().add("Location", "/home")))

    headers.redirectLocation should be(Some("/home"))
  }

  it should "give back nothing in case there are no Location header" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders(new DefaultHttpHeaders()))

    headers.redirectLocation should be(None)
  }

  it should "parse out content encoding from Content-Type header" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders(new DefaultHttpHeaders().add("Content-Type", "text/html; charset=EUC-JP")))

    headers.contentCharset should be(Some("EUC-JP"))
  }

  it should "parse out content encoding optionally enclosed in quotes" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders(new DefaultHttpHeaders().add("Content-Type", "text/xml; charset=\"UTF-8\"")))

    headers.contentCharset should be(Some("UTF-8"))
  }

  it should "give out headers as map" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders(new DefaultHttpHeaders()
                                        .add("Content-Type", "text/xml; charset=\"UTF-8\"")
                                        .add("X-Custom-Header", "Custom value")))

    headers.headers should be(Map(
      "Content-Type" -> List("text/xml; charset=\"UTF-8\""),
      "X-Custom-Header" -> List("Custom value")
    ))
  }
}
