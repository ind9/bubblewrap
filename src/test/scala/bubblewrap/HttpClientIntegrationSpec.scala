package bubblewrap

import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

class HttpClientIntegrationSpec extends FlatSpec {
  it should "fetch some page" in {

    val url = "https://www.amazon.co.uk/gp/yourstore/pym/ref=pd_pyml_rhf/255-0060092-5819207"
       val client: HttpClient = new HttpClient()

        //val proxy = ProxyWithAuth("198.7.107.254", 60000, "indix", "yFL4g3558j", AuthScheme("basic"))
        //val proxy = ProxyWithAuth("indix.crawlera.com", 8010, "indix", "4j7npYynth", AuthScheme("basic"))
        val proxy = PlainProxy("ix-route.stag.proxy.indix.tv",13001)
        val body = client.get(WebUrl.from(url), CrawlConfig(Some(proxy), "", 100000000, 10, Cookies.None)).map { response =>
          println(response.status)
          response.pageResponse match {
            case SuccessResponse(page) => println(new String(page.content))
            case FailureResponse(error) => error.printStackTrace(); List.empty[WebUrl]
          }
        }

    Await.result(body, 100000 millis)
    Thread.sleep(30000)
/*
   val client1 = new AsyncHttpClient(new AsyncHttpClientConfigBean()
     .setConnectionTimeOut(100000)
     .setRequestTimeout(100000)
     .setReadTimeout(100000))
    //val respo = client1.prepareGet(url).setProxyServer(new ProxyServer(ProxyServer.Protocol.HTTP,"198.7.107.254",60000, "indix", "yFL4g3558j")).execute().get()
    val respo = client1.prepareGet(url).setProxyServer(new ProxyServer("localhost",8081)).execute().get()

    println(respo.getResponseBody)
    Thread.sleep(200000)*/
  }
}