import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.mutable
import scala.io.Source
import scala.reflect.io.Directory

class MockFetcher(content: Map[String, String]) extends HttpFetcher {
  override def fetch(url: String): String = content.getOrElse(url, "")
}

class PageScraperTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  override def beforeEach(): Unit = {
    new Directory(new File("tmp")).deleteRecursively()
    super.beforeEach()
  }

  "PageScraper" should "return an error message if the page URL is invalid" in {
    val invalidURL = "invalid"
    PageScraper.scrape(MockLogger, invalidURL, JsoupFetcher)
    MockLogger.logs should contain("Invalid URL")
  }

  it should "create correct file structure for a simple path" in {
    val url = "example.com"
    val fetcher = new MockFetcher(Map(
      url -> Source.fromResource("flat_single_level.html").getLines().mkString("\n")
    ))
    PageScraper.scrape(MockLogger, url, fetcher, "tmp")

    assert(Files.exists(Paths.get("tmp/").resolve(url)))

  }
}

object MockLogger extends Logger {
  val logs = mutable.ArrayBuffer.empty[String]

  override def log(s: String): Unit = logs += s
}
