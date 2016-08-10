package ejisan.play.libs.security

import scala.concurrent.Future
import play.api.mvc.{ Controller, Request, Result }
import play.api.Configuration
import ejisan.play.libs.secureAction._

trait SecuritySupport[S <: Subject] extends ControllerSecuritySupport[S] {
  self: Controller =>
  val SecureAction = new SecureActionBuilder[S] {
    val authenticator = new DefaultAuthenticator(subjectResolver, configuration)
    def onUnauthenticated[A](r: Request[A]) = self.onUnauthenticated(r)
  }
}

trait AuthorizableSecurity[S <: Subject] { self: SecuritySupport[S] =>
  def onUnauthorized[A](request: SecureRequest[A, S]): Future[Result]
  override val SecureAction = new SecureActionBuilder[S] with Authorizable[S] {
    val authenticator = new DefaultAuthenticator(subjectResolver, configuration)
    def onUnauthenticated[A](r: Request[A]) = self.onUnauthenticated(r)
    def onUnauthorized[A](r: SRequest[A]) = self.onUnauthorized(r)
  }
}

trait RoleAuthorizableSecurity[S <: SubjectHasRole[S]] extends AuthorizableSecurity[S] {
  self: SecuritySupport[S] =>
  override val SecureAction = new SecureActionBuilder[S] with RoleAuthorizable[S] {
    val authenticator = new DefaultAuthenticator(subjectResolver, configuration)
    def onUnauthenticated[A](r: Request[A]) = self.onUnauthenticated(r)
    def onUnauthorized[A](r: SRequest[A]) = self.onUnauthorized(r)
  }
}

trait RoleOverridableSecurity[S <: SubjectHasRole[S]] extends RoleAuthorizableSecurity[S] {
  self: SecuritySupport[S] =>
  override val SecureAction = new SecureActionBuilder[S] with RoleAuthorizable[S] {
    val authenticator = new DefaultAuthenticator(subjectResolver, configuration)
    def onUnauthenticated[A](r: Request[A]) = self.onUnauthenticated(r)
    def onUnauthorized[A](r: SRequest[A]) = self.onUnauthorized(r)
    protected override def secureRequest[A](r: Request[A], sbj: S, ossbj: Option[S]): SRequest[A] =
      SecureRequest.superRoled(r, sbj, ossbj)
  }
}
