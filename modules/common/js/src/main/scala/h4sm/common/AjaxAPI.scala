package h4sm.common

import cats.syntax.all._

import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.XMLHttpRequest
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

abstract class AjaxAPI(implicit ec: ExecutionContext) extends API[Future] {
  type Request = XMLHttpRequest

  /** Define a set of headers to pull from responses and thread into our following
    * requests. This is mostly useful for bearer Authorization tokens. If you add
    * "Authorization" to this set, then each time an Authorization header is present
    * in a response, this header will be added to the client's state and sent in the
    * request headers for subsequent requests.
    */
  def threadHeaders: Set[String] = Set()

  protected def mapHeaders(req: XMLHttpRequest): Map[String, String] =
    threadHeaders.foldLeft(Map[String, String]()) {
      case (m, name) if req.getResponseHeader(name) != null =>
        m + (name -> req.getResponseHeader(name))
      case (m, _) => m
    }

  case class Resp(val a: XMLHttpRequest) extends Response {
    def as[T: Decoder]: Future[T] =
      parser.decode[T](a.responseText).leftWiden[Throwable].liftTo[Future]
  }

  def get(url: String): Future[Resp] =
    Ajax.get(url, withCredentials = true).map(Resp.apply(_))

  def post(url: String): Future[Resp] =
    Ajax.post(url).map(Resp(_))

  def delete(url: String): Future[Resp] =
    Ajax.delete(url).map(Resp(_))

  def post[T: Encoder](url: String, a: T): Future[Resp] =
    Ajax.post(url, a.asJson.noSpaces, withCredentials = true).map(Resp(_))

  def getH(route: String, hs: Map[String, String]): Future[(Map[String, String], Resp)] =
    Ajax.get(route, headers = hs, withCredentials = true).map(r => (hs, Resp(r)))

  def postH[A: Encoder](
      route: String,
      a: A,
      hs: Map[String, String],
  ): Future[(Map[String, String], Resp)] =
    Ajax.post(route, a.asJson.noSpaces, headers = hs).map(r => (hs ++ mapHeaders(r), Resp(r)))

  def postR[A: Encoder](route: String, a: A): Future[(Map[String, String], Resp)] =
    Ajax.post(route, a.asJson.noSpaces).map(r => (mapHeaders(r), Resp(r)))
}
