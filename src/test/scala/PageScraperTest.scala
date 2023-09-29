import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URL
import scala.collection.mutable
object MockLogger extends Logger {
  val logs = mutable.ArrayBuffer.empty[String]
  override def log(s: String): Unit = logs += s
}
class PageScraperTest extends AnyFlatSpec with Matchers {
  "PageScraper" should "return an error message if the page URL is invalid" in {
    val ps = new PageScraper
    val invalidURL = "invalid"
    ps.scrape(MockLogger, invalidURL)
    MockLogger.logs should contain ("Invalid URL")
  }
}
