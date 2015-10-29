package ejisan.play.libs.security

import scala.concurrent.Future
import play.api.mvc.{ Controller, Request, Result }
import ejisan.play.libs.secureAction._

trait SubjectResolver[S <: Subject] {
  def apply(username: String, password: String): Future[Option[S]]
  def apply(key1: String, optKey2: Option[String] = None): Future[(Option[S], Option[S])]
}

class DefaultAuthenticator[S <: Subject](subjectResolver: SubjectResolver[S])
  extends Authenticator[S] {
  def apply(username: String, password: String): Future[Option[S]] =
    subjectResolver(username, password)

  def resolve(session: Option[String], superSession: Option[String]) = (session, superSession) match {
    case (Some(key), optSuperKey) => subjectResolver(key, optSuperKey)
    case _ => Future.successful((None, None))
  }
}

trait ControllerSecuritySupport[S <: Subject] { self: Controller =>
  val subjectResolver: SubjectResolver[S]
  val SecureAction: SecureActionBuilder[S]
  def onUnauthenticated[A](request: Request[A]): Future[Result]
}
