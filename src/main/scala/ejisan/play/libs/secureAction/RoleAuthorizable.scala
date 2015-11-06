package ejisan.play.libs.secureAction

import scala.concurrent.Future
import play.api.mvc._
import ejisan.play.libs.Role

trait SubjectHasRole[S <: SubjectHasRole[S]] extends Subject {
  val role: Role
  def roleTo(role: Role): S
}

trait RoleAuthorizable[S <: SubjectHasRole[S]] extends Authorizable[S] { self: SecureActionBuilder[S] =>
  def roleHasAny(role: Role*): AuthorizableAction = new AuthorizableAction(_.role.hasAny(role:_*))
  def rolehasAnySingular(role: Role*): AuthorizableAction = new AuthorizableAction(_.role.hasAnySingular(role:_*))
  def roleIs(role: Role): AuthorizableAction = new AuthorizableAction(_.role.is(role))
  @deprecated("Instead of using the method `role`, use `roleIs`.", "1.2.0")
  def role(role: Role): AuthorizableAction = roleIs(role)
}
