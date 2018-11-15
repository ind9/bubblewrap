package bubblewrap

import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

class HttpClientIntegrationSpec extends FlatSpec {
  it should "fetch some page" in {

    val url = "https://www.indix.com"
    val client: HttpClient = new HttpClient()
    val body = client.get(WebUrl.from(url), CrawlConfig(None, "", 100000000, 10, Cookies.None)).map { response =>
      response.pageResponse match {
        case SuccessResponse(page) => new String(page.content)
        case FailureResponse(error) => error.printStackTrace(); List.empty[WebUrl]
      }
    }

    Await.result(body, 100000 millis)
  }

  it should "fetch some Gzip-ed page" in {
    val url = "https://www.yahoo.com/style/sitemaps/sitemap_index_us_en-US.xml.gz"
    val client: HttpClient = new HttpClient()
    val body = client.get(WebUrl.from(url), CrawlConfig(None, "" , 100000000, 10, Cookies.None)).map { response =>
      response.pageResponse match {
        case SuccessResponse(page) => page.asString
        case FailureResponse(error) => error.printStackTrace(); List.empty[WebUrl]
      }
    }
    Await.result(body, 100000 millis)
  }
}
