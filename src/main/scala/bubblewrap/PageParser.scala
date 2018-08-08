package bubblewrap

import java.io.ByteArrayInputStream
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConversions._
import scala.util.Try

case class Content(
                    url: WebUrl,
                    content: Array[Byte],
                    contentType: Option[String] = None,
                    contentCharset: Option[String] = None,
                    contentEncoding: Option[String] = None,
                    shouldDeflate: Boolean = true
                  ) extends ContentType {
  def asString = {
    if (shouldDeflate && isGzip(this)) {
      Try({
        val gzipStream = new GZIPInputStream(new ByteArrayInputStream(content))
        scala.io.Source.fromInputStream(gzipStream).mkString
      }).getOrElse(new String(content, contentCharset.getOrElse("UTF-8")))
    } else {
      new String(content, contentCharset.getOrElse("UTF-8"))
    }
  }

  def asBytes = content

  def length = content.length

  def contentLength = asString.length
}

trait ContentType {
  val gzipVariants = List(
    "application/gzip",
    "application/x-gzip",
    "application/x-gunzip",
    "application/gzipped",
    "application/gzip-compressed",
    "gzip/document"
  )

  def isGzip(content: Content) = {
    (content.contentEncoding, content.contentType) match {
      case (Some(encoding), _) => gzipVariants.exists(gz => gz.equals(encoding))
      case (None, Some(cType)) => gzipVariants.exists(gz => gz.equals(cType))
      case (_, _) => false
    }
  }
}

class Page(val content: Content) {
  def metaRefresh: Option[WebUrl] = None

  def canonicalUrl: Option[WebUrl] = None

  def outgoingLinks: List[WebUrl] = List.empty

  def url = content.url

  def contentType = content.contentType

  def contentCharset = content.contentCharset

  def contentEncoding = content.contentEncoding
}

object Page {
  def apply(content: Content,
            metaRefresh: => Option[WebUrl] = None,
            canonicalUrl: => Option[WebUrl] = None,
            outgoingLinks: => List[WebUrl] = List.empty): Page = {
    //Clean this up, did this too make as few changes to the API as possible
    val metaRefreshRef = metaRefresh
    val canonicalUrlRef = canonicalUrl
    val outgoingLinksRef = outgoingLinks
    new Page(content) {
      lazy val metaRefreshB = metaRefreshRef
      lazy val canonicalUrlB = canonicalUrlRef
      lazy val outgoingLinksB = outgoingLinksRef

      override def metaRefresh = metaRefreshB

      override def canonicalUrl = canonicalUrlB

      override def outgoingLinks = outgoingLinksB
    }
  }
}

object Content {
  def apply(url: WebUrl, body: Array[Byte], responseHeaders: ResponseHeaders): Content = {
    Content(url, body, responseHeaders.contentEncoding, responseHeaders.contentCharset, responseHeaders.contentType)
  }

  def apply(url: WebUrl, body: Array[Byte], responseHeaders: ResponseHeaders, shouldDeflate: Boolean): Content = {
    Content(url, body, responseHeaders.contentEncoding, responseHeaders.contentCharset, responseHeaders.contentType, shouldDeflate)
  }
}


class PageParser {

  import PageParser._

  def parse(content: Content): Page = {
    val doc = Jsoup.parse(content.asString)
    Page(content, metaRefresh(content.url, doc), canonicalUrl(content.url, doc), outgoingLinks = outgoingLinks(content.url, doc))
  }

  private def metaRefresh(baseUrl: WebUrl, doc: Document) = {
    doc.getElementsByAttributeValueMatching("http-equiv", Pattern.compile("refresh", Pattern.CASE_INSENSITIVE))
      .headOption
      .map(element => element.attr("content"))
      .collect {
        case MetaUrl(location) => baseUrl.resolve(location)
      }
  }

  private def canonicalUrl(baseUrl: WebUrl, doc: Document) = {
    doc.getElementsByAttributeValue("rel", "canonical")
      .headOption
      .map(element => baseUrl.resolve(element.attr("href")))
  }

  private def outgoingLinks(baseUrl: WebUrl, doc: Document): List[WebUrl] = {
    if (noFollow(doc)) return List.empty
    (doc.getElementsByTag("a") ++ doc.getElementsByTag("link"))
      .flatMap { element => extractLink(baseUrl, element) }
      .toList
  }

  private def noFollow(doc: Document) = {
    doc.getElementsByAttributeValue("name", "robots")
      .headOption
      .exists(element => element.attr("content").matches(NOFOLLOW))
  }

  private def extractLink(baseUrl: WebUrl, elem: Element) = {
    val href = elem.attr("href").trim
    val shouldFollow = elem.attr("rel") != "nofollow"
    val isCanonical = elem.attr("rel") == "canonical"
    val isWebPageLink = allowedSchemes(href) && notHasIllegalCharacters(href)
    if (isWebPageLink && shouldFollow && !isCanonical) Some(baseUrl.resolve(href)) else None
  }

  private def notHasIllegalCharacters(href: String) = !href.contains("@")

  private def allowedSchemes(href: String) = href match {
    case SCHEME(scheme) => schemes contains scheme.trim.toLowerCase
    case _ => true
  }
}

object PageParser {
  val MetaUrl = "(?i)[^;]*;\\s*URL\\s*=(.*)".r
  val NOFOLLOW = "(?i).*nofollow.*"
  val SCHEME = "(?s)([^:]+):.*".r
  val schemes = Set("http", "https")

  def apply() = new PageParser()
}
