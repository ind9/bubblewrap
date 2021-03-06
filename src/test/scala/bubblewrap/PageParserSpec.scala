package bubblewrap

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class PageParserSpec extends FlatSpec {
  "PageParser" should "parse absolute url from meta refresh" in {
    val html = """<html><meta http-equiv="refresh" content="0;URL=http://www.example.com/refresh"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).metaRefresh should be(Some(WebUrl("http://www.example.com/refresh")))
  }

  it should "parse relative url from meta refresh" in {
    val html = """<html><meta http-equiv="refresh" content="0;URL=/refresh"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).metaRefresh should be(Some(WebUrl("http://www.example.com/refresh")))
  }

  it should "parse none from meta refresh if meta not present" in {
    val html = """<html><body>Hi</body></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).metaRefresh should be(None)
  }

  it should "parse url from meta refresh case insensitivity of pattern" in {
    val html = """<html><meta http-equiv="refresh" content="0;url =http://www.example.com/refresh"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).metaRefresh should be(Some(WebUrl("http://www.example.com/refresh")))
  }

  it should "parse absolute url from rel canonical" in {
    val html = """<html><link rel="canonical" href="http://www.example.com/canonical"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).canonicalUrl should be(Some(WebUrl("http://www.example.com/canonical")))
  }

  it should "parse relative url from rel canonical" in {
    val html = """<html><link rel="canonical" href="/canonical"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).canonicalUrl should be(Some(WebUrl("http://www.example.com/canonical")))
  }

  it should "parse none from rel canonical if link not present" in {
    val html = """<html><body>Hi</body></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).canonicalUrl should be(None)
  }

  it should "parse url from rel canonical case insensitivity" in {
    val html = """<html><link rel="CANONICAL" href="http://www.example.com/canonical"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).canonicalUrl should be(Some(WebUrl("http://www.example.com/canonical")))
  }

  it should "extract outgoing links from a and link tags" in {
    val html = """<html><link href="http://www.example.com/link"/><a href="http://www.example.com/a"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).outgoingLinks should contain(WebUrl("http://www.example.com/link"))
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).outgoingLinks should contain(WebUrl("http://www.example.com/a"))
  }

  it should "extract relative outgoing links from a and link tags" in {
    val html = """<html><link href="/link"/><a href="/a"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).outgoingLinks should contain(WebUrl("http://www.example.com/link"))
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).outgoingLinks should contain(WebUrl("http://www.example.com/a"))
  }

  it should "extract outgoing links from a and link tags ignoring nofollow and canonical" in {
    val html = """<html><link rel="nofollow" href="http://www.example.com/link"/><a rel="canonical" href="http://www.example.com/a"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).outgoingLinks should be(List.empty)
  }

  it should "extract outgoing links from a and link tags ignoring non-webpage-links" in {
    val html =
      """<html>
        |<a href="javascript:var x=5;"/>
        |<a href="Javascript:var x=5;"/>
        |<a href="mailto:a.com"/>
        |<a href="callto:224332"/>
        |<a href="tel:2234332"/>
        |<a href="android-app:http://play.com/app"/>
        |<a href="ios-app:http://itunes.com/app"/>
        |<a href="data:=3A232221wec"/>
        |<a href="feed:<xml id=abc/>"/>
        |<a href="skype:user_id"/>
        |<a href="javascipt: user_id =
        |test"/>
        |</html>""".stripMargin

    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).outgoingLinks should be(List.empty)
  }

  it should "extract outgoing links while ignoring links on page with nofollow" in {
    val html = """<html><meta name="robots" content="noodp,noydir,nofollow" /><link href="http://www.example.com/link"/><a href="http://www.example.com/a"/></html>"""
    PageParser().parse(Content(WebUrl("http://www.example.com"), html.getBytes())).outgoingLinks should be(List.empty)
  }
}
