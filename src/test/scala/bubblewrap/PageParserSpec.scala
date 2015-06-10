package bubblewrap

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class PageParserSpec extends FlatSpec {
  "PageParser" should "parse absolute url from meta refresh" in {
    val parser = new PageParser()
    val html = """<html><meta http-equiv="refresh" content="0;URL=http://www.example.com/refresh"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).metaRefresh should be(Some(WebUrl("http://www.example.com/refresh")))
  }

  it should "parse relative url from meta refresh" in {
    val parser = new PageParser()
    val html = """<html><meta http-equiv="refresh" content="0;URL=/refresh"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).metaRefresh should be(Some(WebUrl("http://www.example.com/refresh")))
  }

  it should "parse none from meta refresh if meta not present" in {
    val parser = new PageParser()
    val html = """<html><body>Hi</body></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).metaRefresh should be(None)
  }

  it should "parse url from meta refresh case insensitivity of pattern" in {
    val parser = new PageParser()
    val html = """<html><meta http-equiv="refresh" content="0;url =http://www.example.com/refresh"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).metaRefresh should be(Some(WebUrl("http://www.example.com/refresh")))
  }

  it should "parse absolute url from rel canonical" in {
    val parser = new PageParser()
    val html = """<html><link rel="canonical" href="http://www.example.com/canonical"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).canonicalUrl should be(Some(WebUrl("http://www.example.com/canonical")))
  }

  it should "parse relative url from rel canonical" in {
    val parser = new PageParser()
    val html = """<html><link rel="canonical" href="/canonical"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).canonicalUrl should be(Some(WebUrl("http://www.example.com/canonical")))
  }

  it should "parse none from rel canonical if link not present" in {
    val parser = new PageParser()
    val html = """<html><body>Hi</body></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).canonicalUrl should be(None)
  }

  it should "parse url from rel canonical case insensitivity" in {
    val parser = new PageParser()
    val html = """<html><link rel="CANONICAL" href="http://www.example.com/canonical"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).canonicalUrl should be(Some(WebUrl("http://www.example.com/canonical")))
  }

  it should "extract outgoing links from a and link tags" in {
    val parser = new PageParser()
    val html = """<html><link href="http://www.example.com/link"/><a href="http://www.example.com/a"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).outgoingLinks should contain(WebUrl("http://www.example.com/link"))
    parser.parse(WebUrl("http://www.example.com"), html).outgoingLinks should contain(WebUrl("http://www.example.com/a"))
  }

  it should "extract relative outgoing links from a and link tags" in {
    val parser = new PageParser()
    val html = """<html><link href="/link"/><a href="/a"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).outgoingLinks should contain(WebUrl("http://www.example.com/link"))
    parser.parse(WebUrl("http://www.example.com"), html).outgoingLinks should contain(WebUrl("http://www.example.com/a"))
  }

  it should "extract outgoing links from a and link tags ignoring nofollow and canonical" in {
    val parser = new PageParser()
    val html = """<html><link rel="nofollow" href="http://www.example.com/link"/><a rel="canonical" href="http://www.example.com/a"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).outgoingLinks should be(List.empty)
  }

  it should "extract outgoing links from a and link tags ignoring non-webpage-links" in {
    val parser = new PageParser()
    val html = """<html><a href="javascript:var x=5;"/><a href="mailto:a@a.com"/><a href="b@b.com"/></html>"""

    parser.parse(WebUrl("http://www.example.com"), html).outgoingLinks should be(List.empty)
  }
}
