package ejisan.libs.security

import java.util.Arrays
import java.security.{ SecureRandom, NoSuchAlgorithmException }
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

class SecureHash(
  final val algorithms: Seq[(String, String)],
  @inline final val defaultVersion: Int = 1,
  @inline final val iteration: Int = 10000,
  @inline final val saltLength: Int = 64,
  @inline final val hashLength: Int = 64
) {
  require(defaultVersion > 0, "Version number must be over 1.")
  require(algorithms.length > 0, "No algorithms")
  algorithms foreach { case (hashAlgorithm, saltAlgorithm) => try {
    SecretKeyFactory.getInstance(hashAlgorithm)
    SecureRandom.getInstance(saltAlgorithm)
  } catch {
    case e: NoSuchAlgorithmException =>
      throw new IllegalArgumentException("Unsupported algorithm. " + e.getMessage)
  }}

  /** Hash delimiter */
  @inline protected val delimiter: Char = ':'
  /** Generate hash from text with default options */
  final def apply(text: String): String = generate(text)
  /** Validate text and hash with default options */
  final def apply(text: String, hash: String): Boolean = validate(text, hash)
  /** Generate hash from text */
  def generate(
    text: String,
    version: Int = defaultVersion,
    iteration: Int = iteration,
    saltLength: Int = saltLength,
    hashLength: Int = hashLength
  ): String = {
    val (hashAlgorithm, saltAlgorithm) = try {
      algorithms(version - 1)
    } catch {
      case e: IndexOutOfBoundsException =>
        throw new RuntimeException(s"Version $version is not supporting")
    }
    val salt = this.salt(saltLength, saltAlgorithm)
    val hash = this.hash(text.toCharArray, salt, iteration, hashLength, hashAlgorithm)
    s"$iteration:${toHex(salt)}:${toHex(hash)}:$version"
  }
  /** Validate text and hash */
  def validate(text: String, targetHash: String, givenVersion: Option[Int] = None): Boolean = {
    val (iteration, saltHex, hashHex, version) = parse(targetHash, givenVersion)
    val (hashAlgorithm, _) = try {
      algorithms(version - 1)
    } catch {
      case e: IndexOutOfBoundsException =>
        throw new RuntimeException(s"Version $version is not supporting")
    }
    Arrays.equals(
      this.hash(text.toCharArray, fromHex(saltHex), iteration, hashHex.length, hashAlgorithm),
      fromHex(hashHex))
  }
  /** Parse generated hash */
  final def parse(targetHash: String, givenVersion: Option[Int] = None): (Int, String, String, Int) = try {
    targetHash.split(delimiter) match {
      case Array(it, salt, hash) =>
        (it.toInt, salt, hash, givenVersion.getOrElse(defaultVersion))
      case Array(it, salt, hash, version) =>
        (it.toInt, salt, hash, givenVersion.getOrElse(version.toInt))
    }
  } catch {
    case e: NumberFormatException =>
      throw new RuntimeException("Cannot parse String to Int. " + e.getMessage)
  }
  /** Generate random salt */
  protected[this] final def salt(length: Int, algorithm: String): Array[Byte] = {
    val salt = Array.ofDim[Byte](length / 2)
    SecureRandom.getInstance(algorithm).nextBytes(salt)
    salt
  }
  /** Generate hash */
  protected[this] final def hash(text: Array[Char], salt: Array[Byte], iteration: Int, length: Int, algorithm: String): Array[Byte] =
    SecretKeyFactory
      .getInstance(algorithm)
      .generateSecret(new PBEKeySpec(text, salt, iteration, length * 4))
      .getEncoded()
  /** Byte array to hex */
  protected[this] final def toHex(bytes: Array[Byte]): String =
    bytes.map("%02X" format _).mkString
  /** Hex to byte array */
  protected[this] final def fromHex(hex: String): Array[Byte] =
    hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
}

object SecureHash extends SecureHash(Seq(("PBKDF2WithHmacSHA512", "NativePRNGNonBlocking")), 1, 10000, 64, 64)
