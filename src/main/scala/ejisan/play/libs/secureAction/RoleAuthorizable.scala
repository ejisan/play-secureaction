package ejisan.play.libs.secureAction

import scala.concurrent.Future
import play.api.mvc._
import ejisan.play.libs.Role

trait SubjectHasRole[S <: SubjectHasRole[S]] extends Subject {
  val role: Role
  def roleTo(role: Role): S
}

trait RoleAuthorizable[S <: SubjectHasRole[S]] extends Authorizable[S] { self: SecureActionBuilder[S] =>
  def role(role: Role): AuthorizableAction = new AuthorizableAction(_.role == role)
  def hasRoleAny(role: Role): AuthorizableAction = new AuthorizableAction(_.role.hasAny(role))
  def hasRoleAll(role: Role): AuthorizableAction = new AuthorizableAction(_.role.hasAll(role))
}
