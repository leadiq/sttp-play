package com.softwaremill.sttp.playws

import java.io.ByteArrayInputStream

import com.softwaremill.sttp.ResponseAs.EagerResponseHandler
import com.softwaremill.sttp._
import play.api.Application
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient}
import play.api.libs.ws.{ProxyServer => Unused, Response => Totem, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Try}

class PlayWSClientBackend[S](wsClient: WSClient, mustCloseClient: Boolean, rm: MonadAsyncError[Future])
  extends SttpBackend[Future, S] {
  private def convertRequest[T](request: Request[T, S]): WSRequestHolder = {
    val holder = wsClient.url(request.uri.toString())
    val maybeBody = bodyToWSBody(request.body)

    val holderWithBody = maybeBody.foldLeft(holder)(_.withBody(_))

    holderWithBody
      .withMethod(request.method.m)
      .withHeaders(
        request.headers: _*
      )
      .withRequestTimeout(request.options.readTimeout.toMillis.toInt)
  }

  private def bodyToWSBody[T](body: RequestBody[S]): Option[WSBody] = {
    body match {
      case NoBody => None
      case StringBody(b, _, _) =>
        Some(InMemoryBody(b.getBytes))
      case ByteArrayBody(b, _) =>
        Some(InMemoryBody(b))
      case ByteBufferBody(b, _) =>
        Some(InMemoryBody(b.array()))
      case InputStreamBody(b, _) =>
        Some(InMemoryBody(Iterator continually b.read takeWhile (_ != -1) map (_.toByte) toArray))
      case PathBody(b, _) =>
        Some(FileBody(b.toFile))
      case StreamBody(s) =>
        streamToRequestBody(s)
      case MultipartBody(_) =>
        // TODO Support multi-part bodies
        None
    }
  }

  private def readResponse[T](res: WSResponse, responseAs: ResponseAs[T, S]): Future[Response[T]] = {
    val status = res.status

    val body = if (codeIsSuccess(status)) {
      responseMonad.map(responseHandler(res).handle(responseAs, responseMonad))(Right(_))
    } else {
      responseMonad.map(responseHandler(res).handle(asString, responseMonad))(Left(_))
    }

    val headers = res.allHeaders.flatMap {
      case (name, values) => values.map(name -> _)
    }

    responseMonad.map(body)(Response(_, res.status, res.statusText, headers.toList, Nil))
  }

  private def responseHandler(res: WSResponse) = new EagerResponseHandler[S] {

    override def handleBasic[T](bra: BasicResponseAs[T, S]): Try[T] = bra match {
      case IgnoreResponse =>
        Try(())
      case ResponseAsString(_) =>
        Try(res.body)
      case ResponseAsByteArray =>
        Try(res.body.getBytes())
      case ras @ ResponseAsStream() =>
        responseBodyToStream(res).map(ras.responseIsStream)
      case ResponseAsFile(file, overwrite) =>
        Try(ResponseAs.saveFile(file, new ByteArrayInputStream(res.body.getBytes()), overwrite))

    }
  }

  def responseBodyToStream(res: WSResponse): Try[S] =
    Failure(new IllegalStateException("Streaming isn't supported"))

  private def streamToRequestBody(s: S): Option[WSBody] = None

  def send[T](r: Request[T, S]): Future[Response[T]] = {
    val request = convertRequest(r)

    rm.flatMap(request.execute())(readResponse(_, r.response))
  }

  def close(): Unit =
    wsClient match {
      case c: NingWSClient if mustCloseClient => c.close()
    }

  def responseMonad: MonadError[Future] = rm
}

object Play23WSClientBackend {
  private def defaultClient(wsClientConfig: WSClientConfig): WSClient = {
    new NingWSClient(new NingAsyncHttpClientConfigBuilder(wsClientConfig).build())
  }

  private def apply(client: WSClient, mustCloseClient: Boolean = false)(
    implicit ec: ExecutionContext
  ): SttpBackend[Future, Nothing] =
    new FollowRedirectsBackend[Future, Nothing](new PlayWSClientBackend(client, mustCloseClient, new FutureMonad))

  def apply(config: WSClientConfig)(implicit ec: ExecutionContext): SttpBackend[Future, Nothing] =
    Play23WSClientBackend(
      Play23WSClientBackend.defaultClient(config),
      mustCloseClient = true
    )

  def apply()(implicit ec: ExecutionContext, app: Application): SttpBackend[Future, Nothing] =
    Play23WSClientBackend(new DefaultWSConfigParser(app.configuration, app.classloader).parse())

  def usingClient(wsClient: WSClient)(implicit ec: ExecutionContext): SttpBackend[Future, Nothing] =
    Play23WSClientBackend(wsClient)
}
