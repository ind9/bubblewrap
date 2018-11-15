package bubblewrap

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import TestUtils.{readAsBytes, readAsString}

class ContentSpec  extends FlatSpec{
  "Content" should "use the content encoding to encode the bytes back to string"  in {
    val koreanString = "동서게임 MS정품유선컨트롤러/PCGTA5호환 - 11번가"
    val content = Content(WebUrl("http://www.example.com/dummy"),koreanString.getBytes("EUC-KR"),contentCharset = Some("EUC-KR"))

    content.asString should be(koreanString)
  }

  it should "use UTF-8 incase no contentCharset is given" in {
    val koreanString = "동서게임 MS정품유선컨트롤러/PCGTA5호환 - 11번가"
    val content = Content(WebUrl("http://www.example.com/dummy"),koreanString.getBytes("UTF-8"))

    content.asString should be(koreanString)
  }

  it should "read Gzipped content (with UTF-8 encoding & text/html content type) to string" in {
    val encoding = "UTF-8"
    val gzipped = readAsBytes("/fixtures/1-gzipped.html")
    val ungzipped = readAsBytes("/fixtures/1-ungzipped.html")
    val content = Content(WebUrl("http://www.example.com/dummy"), gzipped, contentType = Some("text/html; charset=utf-8"), contentCharset = Some(encoding), contentEncoding = Some("gzip"))

    content.asString should be (new String(ungzipped, encoding))
  }

  it should "read Gzipped content (with UTF-8 encoding & gzip content type) to string" in {
    val encoding = "UTF-8"
    val gzipped = readAsBytes("/fixtures/1-gzipped.html")
    val ungzipped = readAsBytes("/fixtures/1-ungzipped.html")
    val content = Content(WebUrl("http://www.example.com/dummy"), gzipped, contentType = Some("gzip"), contentCharset = Some(encoding), contentEncoding = Some("text/html; charset=UTF-8"))

    content.asString should be (new String(ungzipped, encoding))
  }

  it should "read Gzipped content (with ISO-8859-1 encoding) to string" in {
    val encoding = "ISO-8859-1"
    val gzipped = readAsBytes("/fixtures/2-gzipped-iso8859-1.html")
    val ungzipped = readAsBytes("/fixtures/2-ungzipped-iso8859-1.html")
    val content = Content(WebUrl("http://www.example.com/dummy"), gzipped, contentType = Some("application/gzip; charset=iso-8859-1"), contentCharset = Some(encoding), contentEncoding = None)

    content.asString should be (new String(ungzipped, encoding))
  }

  it should "read Gzipped content (with unknown encoding) to string" in {
    val gzipped = readAsBytes("/fixtures/3-gzipped.html")
    val ungzipped = readAsBytes("/fixtures/3-ungzipped.html")
    val content = Content(WebUrl("http://www.example.com/dummy"), gzipped, contentType = Some("gzip"), contentCharset = None, contentEncoding = Some("text/html"))

    content.asString should be (new String(ungzipped))
  }
}
