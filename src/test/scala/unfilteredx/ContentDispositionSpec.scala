package unfilteredx

import org.specs2.mutable.Specification
import java.nio.charset.Charset

class ContentDispositionSpec extends Specification {
  "Content Disposition" should {
    "generate simple inline correctly" in {
      val expected  = "inline"
      ContentDisposition(DispositionType.INLINE).toString() must be equalTo(expected)
    }

    "Generate attachment correctly from example in rfc" in {
      val expected = """attachment; filename="genome.jpeg""""
      val actual = ContentDisposition(
        DispositionType.ATTACHMENT,
        Some("genome.jpeg")
      )

      actual.toString() must be equalTo expected
    }

    "Parse inline header value correctly from example in rfc" in {
      val input = """INLINE; FILENAME= "an example.html""""
      val expected = ContentDisposition(
        DispositionType.INLINE,
        Some("an example.html")
      )

      ContentDisposition(input) must be equalTo Some(expected)
    }
    "Parse attachment value with underscore in filename" in {
      val input = """attachment; FILENAME= "an_example.html""""
      val expected = ContentDisposition(
        DispositionType.ATTACHMENT,
        Some("an_example.html")
      )

      ContentDisposition(input) must be equalTo Some(expected)
    }
    "Fail to parse attachment value with % in filename" in {
      val input = """attachment; FILENAME= "an%example.html""""
      ContentDisposition(input) must be equalTo None
    }
    "Parse attachment header value correctly from example in rfc" in {
      val input = """attachment;
                          filename*= UTF-8''%e2%82%ac%20rates"""
      val expected = ContentDisposition(
        DispositionType.ATTACHMENT,
        None,
        Some(CharsetFilename("€ rates", Some(Charset.forName("UTF-8"))))
      )

      ContentDisposition(input) must be equalTo Some(expected)
    }
  }
}