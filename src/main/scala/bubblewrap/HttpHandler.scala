package bubblewrap


import java.io.IOException
import java.util.concurrent.TimeoutException

import io.netty.handler.codec.http.DefaultHttpHeaders
import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient.{AsyncHandler, HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus}

import scala.concurrent.Promise

class HttpHandler(config:CrawlConfig, url: WebUrl) extends AsyncHandler[Unit]{
  private[bubblewrap] var body = new ResponseBody
  var statusCode:Int = 200
  val httpResponse = Promise[HttpResponse]()
  var headers: ResponseHeaders = new ResponseHeaders(new HttpResponseHeaders(new DefaultHttpHeaders))
  val startTime = System.currentTimeMillis()
  val UNKNOWN_ERROR = 1006
  val TIMEOUT_ERROR = 9999
  val REMOTE_CLOSE = 9998
  val CAPTCHA_CONTENT = 9997

  private def errorCode(error: Throwable) = error match {
    case timeout:TimeoutException => TIMEOUT_ERROR
    case other:IOException if other.getMessage.contains("Remotely closed") => REMOTE_CLOSE
    case other => UNKNOWN_ERROR
  }

  override def onThrowable(error: Throwable) = httpResponse.success(HttpResponse(errorCode(error), FailureResponse(error), headers, responseTime))


  override def onCompleted(): Unit = {
    val content = Content(url, body.content, headers, config.shouldDeflate)
    if (content.contentLength <= config.minSize && statusCode == 200) statusCode = CAPTCHA_CONTENT
    //emptying the body, so that the content doesn't get referenced in the HashedWheelTimerBucket
    body = null
    httpResponse.success(HttpResponse(statusCode, SuccessResponse(content), headers, responseTime))
  }

  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): State = {
    val part = bodyPart.getBodyPartBytes
    body.write(part)
    if (body.exceedsSize(config.maxSize)){
      httpResponse.success(HttpResponse(statusCode, FailureResponse(ExceededSize(config.maxSize)), headers, responseTime))
      return State.ABORT
    }
    State.CONTINUE
  }

  override def onStatusReceived(status: HttpResponseStatus): State = {
    statusCode = status.getStatusCode
    State.CONTINUE
  }

  override def onHeadersReceived(httpResponseHeaders: HttpResponseHeaders): State = {
    headers = new ResponseHeaders(httpResponseHeaders)
    if(headers.exceedsSize(config.maxSize)) {
      httpResponse.success(HttpResponse(statusCode, FailureResponse(ExceededSize(config.maxSize)), headers, responseTime))
      return State.ABORT
    }
    State.CONTINUE
  }

  private def responseTime = System.currentTimeMillis() - startTime
}
