package ejisan.play.libs.secureAction

import scala.concurrent.Future
import play.api.mvc._

trait Authorizable[S <: Subject] { self: SecureActionBuilder[S] =>

  def onUnauthorized[A](request: SRequest[A]): Future[Result]

  class AuthorizableAction(authorizer: S => Boolean) extends SecureActionBuilder[S] {
    val authenticator = self.authenticator
    def onUnauthenticated[A](request: Request[A]) = self.onUnauthenticated(request)

    def apply(
      authorized: SRequest[AnyContent] => Result,
      unauthorized: SRequest[AnyContent] => Future[Result],
      unauthenticated: Request[AnyContent] => Future[Result]): SecureAction[AnyContent] =
      apply(defaultBodyParser)(authorized, unauthorized, unauthenticated)

    def apply[A](bodyParser: BodyParser[A])(
      authorized: SRequest[A] => Result,
      unauthorized: SRequest[A] => Future[Result],
      unauthenticated: Request[A] => Future[Result]): SecureAction[A] =
      async(bodyParser)(r => Future.successful(authorized(r)), unauthorized, unauthenticated)

    def async(
      authorized: SRequest[AnyContent] => Future[Result],
      unauthorized: SRequest[AnyContent] => Future[Result],
      unauthenticated: Request[AnyContent] => Future[Result]): SecureAction[AnyContent] =
      async(defaultBodyParser)(authorized, unauthorized, unauthenticated)

    def async[A](bodyParser: BodyParser[A])(
      authorized: SRequest[A] => Future[Result],
      unauthorized: SRequest[A] => Future[Result],
      unauthenticated: Request[A] => Future[Result]): SecureAction[A] =
      createAction(bodyParser, authorized, unauthorized, unauthenticated)

    override protected def createAction[A](
      bodyParser: BodyParser[A],
      authenticated: SRequest[A] => Future[Result],
      unauthenticated: Request[A] => Future[Result]): SecureAction[A] =
      createAction(bodyParser, authenticated, onUnauthorized, unauthenticated)

    protected def createAction[A](
      bodyParser: BodyParser[A],
      authorized: SRequest[A] => Future[Result],
      unauthorized: SRequest[A] => Future[Result],
      unauthenticated: Request[A] => Future[Result]): SecureAction[A] = new SecureAction[A] {
      def parser = bodyParser
      def apply(request: Request[A]) = try {
        authenticator(request) flatMap {
          case (Some(subject), optSuperSubject) =>
            val sreq = secureRequest(request, subject, optSuperSubject)
            if (authorizer(subject))
              invokeBlock(sreq, authorized)
            else
              invokeBlock(sreq, unauthorized)
          case _ => invokeBlock(request, unauthenticated).map(session.endSu(_)(request))
        }
      } catch {
        // NotImplementedError is not caught by NonFatal, wrap it
        case e: NotImplementedError => throw new RuntimeException(e)
        // LinkageError is similarly harmless in Play Framework, since automatic reloading could easily trigger it
        case e: LinkageError => throw new RuntimeException(e)
      }
      override def executionContext = self.executionContext
    }
  }

  def authorize(authorizer: S => Boolean) = new AuthorizableAction(authorizer)
}
