package bubblewrap

import com.ning.http.client.{AsyncHttpClient, ProxyServer}
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

class HttpClientIntegrationSpec extends FlatSpec {
  it should "fetch some page" in {

    val client: HttpClient = new HttpClient()

    val url = "http://www.bestbuy.com/site/lenovo-yoga-710-2-in-1-15-6-touch-screen-laptop-intel-core-i5-8gb-memory-nvidia-geforce-940mx-256gb-ssd-pearl-black/5579128.p?skuId=5579128"
    val proxy = ProxyWithAuth("198.7.107.254", 60000, "indix", "yFL4g3558j", AuthScheme("ntlm"))
    val body = client.get(WebUrl.from(url), CrawlConfig(Some(proxy), "raj", 100000000, 10, Cookies.None)).map { response =>
      response.pageResponse match {
        case SuccessResponse(page) => PageParser().parse(page).outgoingLinks
        case FailureResponse(error) => error.printStackTrace(); List.empty[WebUrl]
      }
    }
    Await.result(body, 1000000 millis).foreach(println)

    val client1 = new AsyncHttpClient()
    val respo = client1.prepareGet(url).setProxyServer(new ProxyServer(ProxyServer.Protocol.HTTP,"198.7.107.254",60000, "indix", "yFL4g3558j")).execute().get()
    println(respo.getStatusCode)
  }
}