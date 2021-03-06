package unfilteredx

import unfiltered.response.ResponseHeader
import java.nio.charset.Charset
import java.io.ByteArrayOutputStream
import java.util.{Scanner, Locale}
import java.net.URLDecoder

sealed abstract class DispositionType(val name: String) {
  override def equals(obj: Any) = obj match {
    case DispositionType(n) => n == name
    case _ => false
  }
}

object DispositionType {
  def apply(name: String): DispositionType = name.toLowerCase(Locale.ENGLISH) match {
    case INLINE.name => INLINE
    case ATTACHMENT.name => ATTACHMENT
    case _ => sys.error("Unknown")
  }

  def unapply(dt: DispositionType) = Some(dt.name)

  case object INLINE extends DispositionType("inline")

  case object ATTACHMENT extends DispositionType("attachment")

}

case class CharsetFilename(filename: String, charset: Option[Charset] = None) {
  def format = {
    charset.map(c => c.name() + "''" + Rfc3986.encode(filename, c)).getOrElse(filename)
  }
}

object CharsetFilename {
  def decoded(s: String) = {
    s.split("''", 2) match {
      case Array(charset, f) => {
        val c = Charset.forName(charset)
        CharsetFilename(Rfc3986.decode(f, c), Some(c))
      }
      case Array(f) => CharsetFilename(f, None)
    }
  }
}

/**
 * http://tools.ietf.org/html/rfc6266
 *
 */
case class ContentDisposition(dispositionType: DispositionType, filename: Option[String] = None, filenameSTAR: Option[CharsetFilename] = None) {
  override def toString = {
    val sb = new StringBuilder()
    sb.append(dispositionType.name)
    if (filename.isDefined) {
      sb.append("; filename=")
      sb.append('"')
      sb.append(filename.get)
      sb.append('"')
    }
    if (filenameSTAR.isDefined) {
      sb.append("; filename*=")
      sb.append(filenameSTAR.get.format)
    }
    sb.toString()
  }

  def toResponseHeader = ResponseHeader(ContentDisposition.headerName, List(toString))
}

object ContentDisposition {
  val headerName = "Content-Disposition"

  private val FilenameStar = """(?sm)(\w+);\s*filename\*=\s*(.*)""".r
  private val Filename = """(?im)(\w+);\s*filename=\s*"?(.*[^\"])"?""".r
  private val DT = """(?i)(\w+)""".r

  private val Separators = "()<>@,;:\\\"%/[]?={}\t"
  private val CTLs = ((0 to 31) ++ Seq(127)).map(_.toChar).mkString

  def apply(s: String): Option[ContentDisposition] = {
    s match {
      case Filename(t, f) if isValidToken(f) => Some(ContentDisposition(DispositionType(t), Some(f), None))
      case FilenameStar(t, f) => {
        Some(ContentDisposition(DispositionType(t), None, Some(CharsetFilename.decoded(f))))
      }
      case DT(t) => Some(ContentDisposition(DispositionType(t), None, None))
      case ss => None
    }
  }

  private def isValidToken(s: String) = s.forall(c => Separators.indexOf(c) < 0 && CTLs.indexOf(c) < 0)

}

object Rfc3986 {

  def decode(input: String, charset: Charset) = URLDecoder.decode(input, charset.name())

  def encode(input: String, charset: Charset): String = encode(input.getBytes(charset))

  private def encode(source: Array[Byte]): String = {
    val bos = new ByteArrayOutputStream(source.length)

    source foreach (
      b => {
        val byte: Int = if (b < 0) b + 256 else b.toInt
        if (isAllowed(byte)) {
          bos.write(byte)
        }
        else {
          bos.write('%')

          val hex1 = Character.toUpperCase(Character.forDigit((byte >> 4) & 0xF, 16))
          val hex2 = Character.toUpperCase(Character.forDigit(byte & 0xF, 16))

          bos.write(hex1)
          bos.write(hex2)
        }
      }
      )
    new String(bos.toByteArray, "US-ASCII")
  }

  private def isAllowed(c: Int) = isPchar(c.toChar)

  /**
   * Indicates whether the given character is in the "ALPHA" set.
   *
   * <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
   */
  private def isAlpha(c: Char) = (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')

  /**
   * Indicates whether the given character is in the "DIGIT" set.
   *
   * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
   */
  private def isDigit(c: Char) = c >= '0' && c <= '9'

  /**
   * Indicates whether the given character is in the "gen-delims" set.
   *
   * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
   */
  private def isGenericDelimiter(c: Char) = ':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c

  /**
   * Indicates whether the given character is in the "sub-delims" set.
   *
   * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
   */
  private def isSubDelimiter(c: Char) = '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
    ',' == c || ';' == c || '=' == c

  /**
   * Indicates whether the given character is in the "reserved" set.
   *
   * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
   */
  private def isReserved(c: Char): Boolean = isGenericDelimiter(c) || isReserved(c)

  /**
   * Indicates whether the given character is in the "unreserved" set.
   *
   * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
   */
  private def isUnreserved(c: Char) = isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c


  /**
   * Indicates whether the given character is in the "pchar" set.
   *
   * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
   */
  private def isPchar(c: Char) = isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c
}