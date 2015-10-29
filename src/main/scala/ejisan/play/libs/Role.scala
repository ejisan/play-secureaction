package ejisan.play.libs

import scala.language.implicitConversions
import play.api.libs.json._
import play.api.data.validation.ValidationError

class Role private (
  val flag: Int
) extends AnyVal {
  override def toString: String = s"Role(${flag})"
  /** Returns having role. */
  def hasAny(role: Role): Boolean = (flag & role.flag) > 0
  def hasAll(role: Role): Boolean = (flag ^ role.flag) == 0
  /** Adds role with [Role] object. */
  def +(role: Role): Role = new Role(flag | role.flag)
  /** Removes role */
  def -(role: Role): Role = new Role(flag & ~role.flag)
  def ++ = new Role(flag << 1)
  def -- = new Role(flag >> 1)
}

object Role {

  object conversion {
    implicit def intToRole(flag: Int): Role = Role.nonSingle(flag)
    implicit def roleToInt(role: Role): Int = role.flag

    implicit object roleJsonFormat extends Format[Role] {
      def reads(json: JsValue): JsResult[Role] = json match {
        case JsNumber(bd) => JsSuccess(Role.nonSingle(bd.toInt))
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))) }

      def writes(role: Role): JsValue = JsNumber(role.flag)
    }
  }

  private[this] def count(bits: Long): Int = {
    var bs = (bits & 0x55555555l) + (bits >> 1l & 0x55555555l)
    bs = (bs & 0x33333333l) + (bs >> 2l & 0x33333333l);
    bs = (bs & 0x0f0f0f0fl) + (bs >> 4l & 0x0f0f0f0fl);
    bs = (bs & 0x00ff00ffl) + (bs >> 8l & 0x00ff00ffl);
    ((bs & 0x0000ffffl) + (bs >> 16l & 0x0000ffffl)).toInt
  }

  def apply(): Role = new Role(1)

  def apply(flag: Int): Role = {
    assert(count(flag) == 1, "It must be single flag")
    new Role(flag)

  }

  def nonSingle(flag: Int): Role = {
    new Role(flag)
  }

  def unapply(value: Role): Option[Int] = Some(value.flag)
}
