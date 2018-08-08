package bubblewrap

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

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

  it should "have same contentLength and length in UTF-8 encoding" in {
    val content = Content(null, "test string".getBytes("UTF-8"))
    content.length should be(content.contentLength)
  }

}
