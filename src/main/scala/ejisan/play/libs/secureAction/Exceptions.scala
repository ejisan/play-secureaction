package ejisan.play.libs.secureAction

import javax.security.auth.login.{ CredentialException, FailedLoginException }

class SubjectHasInvalidAuthority extends CredentialException

class InvalidSubject extends FailedLoginException
