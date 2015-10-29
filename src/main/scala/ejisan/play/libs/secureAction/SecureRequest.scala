package ejisan.play.libs.secureAction

import scala.concurrent.Future
import play.api.mvc.{ Request, WrappedRequest }

trait Subject {
  val sessionId: String
}

/**
 * A secure request
 *
 * @param get The user that made the request
 */
abstract class SecureRequest[A, S <: Subject](request: Request[A], val get: S, val getSuper: Option[S])
  extends WrappedRequest[A](request) {
  def getOriginal: S
}

object SecureRequest {
  def apply[A, S <: Subject](request: Request[A], subject: S, optSuperSubject: Option[S]): SecureRequest[A, S] =
    new SecureRequest(request, subject, optSuperSubject) {
      def getOriginal: S = get
    }

  def superRoled[A, S <: SubjectHasRole[S]](request: Request[A], subject: S, optSuperSubject: Option[S]): SecureRequest[A, S] = {
    val orSubject: S = optSuperSubject match {
      case Some(ss) => subject.roleTo(ss.role)
      case None => subject
    }
    new SecureRequest(request, orSubject, optSuperSubject) {
      val getOriginal: S = subject
    }
  }

  def superRoled[A, S <: SubjectHasRole[S]](srequest: SecureRequest[A, S]): SecureRequest[A, S] = {
    val orSubject: S = srequest.getSuper match {
      case Some(ss) => srequest.get.roleTo(ss.role)
      case None => srequest.get
    }
    new SecureRequest(srequest, orSubject, srequest.getSuper) {
      val getOriginal: S = srequest.get
    }
  }
}
