package ejisan.play.libs.secureAction

import scala.concurrent.Future
import play.api.mvc.Request
import play.api.Play

trait Authenticator[S <: Subject] {
  val app = Play.maybeApplication
  val key: String =
    app.flatMap(_.configuration.getString("security.identity.key")).getOrElse("id")
  val superKey: String =
    app.flatMap(_.configuration.getString("security.identity.superKey")).getOrElse("super_id")
  def apply[A](request: Request[A]): Future[(Option[S], Option[S])] =
    resolve(request.session.get(key), request.session.get(superKey))
  def resolve(session: Option[String], superSession: Option[String]): Future[(Option[S], Option[S])]
}
