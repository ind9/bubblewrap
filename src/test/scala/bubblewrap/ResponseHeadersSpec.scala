package bubblewrap

import com.ning.http.client.{FluentCaseInsensitiveStringsMap, HttpResponseHeaders}
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class ResponseHeadersSpec extends FlatSpec {
  "ResponseHeader" should "read content length header and say whether it exceeds size or not" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders() {
      override def getHeaders = new FluentCaseInsensitiveStringsMap().add("Content-Length", "10")
    })

    headers.exceedsSize(10) should be(false)
    headers.exceedsSize(11) should be(false)
    headers.exceedsSize(9) should be(true)
  }

  it should "read redirect location and give back" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders() {
      override def getHeaders = new FluentCaseInsensitiveStringsMap().add("Location", "/home")
    })

    headers.redirectLocation should be(Some("/home"))
  }

  it should "give back nothing in case there are no Location header" in {
    val headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders() {
      override def getHeaders = new FluentCaseInsensitiveStringsMap()
    })

    headers.redirectLocation should be(None)
  }
}
