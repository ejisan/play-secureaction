package ejisan.play.libs.secureAction

import scala.language.higherKinds
import scala.concurrent.{ Future, ExecutionContext }
import play.api.mvc._

trait SecureAction[A] extends Action[A] {
  override def toString = {
    "SecureAction(parser=" + parser + ")"
  }
}

trait SecureActionBuilder[S <: Subject] { self =>
  type SRequest[A] = SecureRequest[A, S]

  protected def executionContext: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext

  implicit val ex: ExecutionContext = executionContext

  val defaultBodyParser: BodyParser[AnyContent] = BodyParsers.parse.default

  val authenticator: Authenticator[S]

  def onUnauthenticated[A](request: Request[A]): Future[Result]

  val defaultUnauthenticated = onUnauthenticated _

  protected trait SessionUtils {

    def start(key: Any)(result: => Result)(implicit request: RequestHeader): Result =
      end(result).withSession((authenticator.key -> key.toString))

    def start(subject: S)(result: => Result)(implicit request: RequestHeader): Result =
      start(subject.sessionId)(result)

    def end(result: => Result)(implicit request: RequestHeader): Result = {
      println("end session")
      result.removingFromSession(authenticator.superKey).removingFromSession(authenticator.key)
    }

    def su(key: Any)(result: => Result)(implicit request: RequestHeader): Result = {
      val strkey = key.toString
      (request.session.get(authenticator.key), request.session.get(authenticator.superKey)) match {
        case (Some(id), None) if id != strkey =>
          result.withSession(
            authenticator.key -> strkey,
            authenticator.superKey -> id
          )
        case (Some(id), Some(superId)) if superId == strkey || id == superId => endSu(result)
        case (Some(id), Some(_)) if id == strkey => result
        case (Some(id), None) => result
        case _ => end(result)
      }
    }

    def endSu(result: => Result)(implicit request: RequestHeader) =
      request.session.get(authenticator.superKey) match {
        case Some(superId) => result
          .removingFromSession(authenticator.superKey)
          .withSession(authenticator.key -> superId)
        case _ => result
      }
  }

  val session = new SessionUtils {}

  def apply(authenticated: => Result): SecureAction[AnyContent] =
    apply(_ => authenticated)

  def apply(authenticated: SRequest[AnyContent] => Result): SecureAction[AnyContent] =
    apply(defaultBodyParser)(authenticated)

  def apply(
    authenticated: SRequest[AnyContent] => Result,
    unauthenticated: Request[AnyContent] => Future[Result]): SecureAction[AnyContent] =
    apply(defaultBodyParser)(authenticated, unauthenticated)

  def apply[A](bodyParser: BodyParser[A])(
    authenticated: SRequest[A] => Result,
    unauthenticated: Request[A] => Future[Result] = defaultUnauthenticated): SecureAction[A] =
    async(bodyParser)(r => Future.successful(authenticated(r)), unauthenticated)

  def async(authenticated: => Future[Result]): SecureAction[AnyContent] =
    async(_ => authenticated)

  def async(authenticated: SRequest[AnyContent] => Future[Result]): SecureAction[AnyContent] =
    async(defaultBodyParser)(authenticated)

  def async(
    authenticated: SRequest[AnyContent] => Future[Result],
    unauthenticated: Request[AnyContent] => Future[Result]): SecureAction[AnyContent] =
    async(defaultBodyParser)(authenticated, unauthenticated)

  def async[A](bodyParser: BodyParser[A])(
    authenticated: SRequest[A] => Future[Result],
    unauthenticated: Request[A] => Future[Result] = defaultUnauthenticated): SecureAction[A] =
    createAction(bodyParser, authenticated, unauthenticated)

  protected def secureRequest[A](request: Request[A], subject: S, optSuperSubject: Option[S]): SRequest[A] =
    SecureRequest(request, subject, optSuperSubject)

  protected def createAction[A](
    bodyParser: BodyParser[A],
    authenticated: SRequest[A] => Future[Result],
    unauthenticated: Request[A] => Future[Result]): SecureAction[A] =
    new SecureAction[A] {
    def parser = bodyParser
    def apply(request: Request[A]) = try {
      authenticator(request) flatMap {
        case (Some(subject), optSuperSubject) => try {
          invokeBlock(secureRequest(request, subject, optSuperSubject), authenticated)
        } catch {
          case e: InvalidSubject =>
            invokeBlock(request, unauthenticated).map(session.endSu(_)(request))
              .map(_.flashing("_SECURE_ACTION_" -> "InvalidSubject"))
        }
        case _ => invokeBlock(request, unauthenticated).map(session.endSu(_)(request))
      }
    } catch {
      case e: SubjectHasInvalidAuthority =>
        throw new RuntimeException("You must extends Authorizable trait for using SubjectHasInvalidAuthority")
      case e: InvalidSubject =>
        throw new RuntimeException("Throwing InvalidSubject is not allowed in this block")
      // NotImplementedError is not caught by NonFatal, wrap it
      case e: NotImplementedError => throw new RuntimeException(e)
      // LinkageError is similarly harmless in Play Framework, since automatic reloading could easily trigger it
      case e: LinkageError => throw new RuntimeException(e)
    }
    override def executionContext = SecureActionBuilder.this.executionContext
  }

  def invokeBlock[A](request: SRequest[A], block: SRequest[A] => Future[Result]) = block(request)

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]) = block(request) map { result =>
    result.removingFromSession(authenticator.key)(request)
  }
}
