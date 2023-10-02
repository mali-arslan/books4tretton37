
import Exceptions.FetchingException
import JsoupWrappers.{DocumentFetcher, JsoupFetcher}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.mutable
import scala.io.Source
import scala.jdk.CollectionConverters.{IteratorHasAsScala, ListHasAsScala}
import scala.reflect.io.Directory


class PageScraperTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  override def beforeEach(): Unit = {
    new Directory(new File("tmp")).deleteRecursively()
    super.beforeEach()
  }

  "PageScraper" should "throw an error if the page URL is invalid" in {
    val invalidURL = "invalid"
    a[FetchingException] should be thrownBy
      PageScraper(invalidURL).scrape(MockLogger, JsoupFetcher)

  }

  it should "create correct file structure for a simple path" in {
    val target = "http://books.com"
    val domain = "books.com"
    val mockFetcher = new DocumentFetcher {
      val mockedResponse =
        Jsoup.parse(
          Source.fromResource("single_level_plain.html").getLines().mkString("\n"),
          target
        )

      override def fetch(url: String): Document = url match {
        case `target` =>
          mockedResponse
      }
    }
    PageScraper(target, "tmp").scrape(MockLogger, mockFetcher)
    assert(Files.exists(Paths.get("tmp/").resolve(domain)))
    Files.list(Paths.get("tmp/").resolve(domain)).count() shouldBe 1
  }

  it should "create correct file structure for a two level plain html" in {
    val target = "http://books.com"
    val domain = "books.com"
    val mockFetcher = new DocumentFetcher {
      override def fetch(url: String): Document = url match {
        case `target` =>
          Jsoup.parse(
            Source.fromResource("two_levels_plain.html").getLines().mkString("\n"),
            target
          )
        case s"$target/page2" => Jsoup.parse(
          Source.fromResource("single_level_plain.html").getLines().mkString("\n"),
          url
        )
      }
    }
    PageScraper(target, "tmp").scrape(MockLogger, mockFetcher)
    assert(Files.exists(Paths.get("tmp/").resolve(domain)))
    assert(Files.exists(Paths.get("tmp/").resolve(s"$domain/page2")))
  }
  it should "save the start page's html and visit each link" in {

    val target = "http://books.toscrape.com"
    val domain = "books.toscrape.com"
    val html = Source.fromResource("books_to_scrape_index.html").getLines().mkString
    val document = Jsoup.parse(html, target)
    // create a mock fetcher that returns a Jsoup document
    // from "books_to_scrape.html" the test resources
    // only for "http://books.toscrape.com".
    // any other target will return an empty shell
    // this way we can test the first layer of links and stop
    val mockFetcher = new DocumentFetcher {
      override def fetch(url: String): Document = url match {
        case `target` =>
          document
        case _ =>
          Document.createShell("http://books.toscrape.com/")
      }
    }
    PageScraper(target, "tmp").scrape(ConsoleLogger, mockFetcher)
    assert(Files.exists(Paths.get("tmp/").resolve(domain)))
    Files.walk(Paths.get("tmp/").resolve(domain))
      .iterator()
      .asScala
      .filter(Files.isRegularFile(_))
      .count(_ => true) shouldBe document.select("a[href]").asScala.map(_.attr("abs:href")).distinct.size
  }
}

object MockLogger extends Logger {
  val logs = mutable.ArrayBuffer.empty[String]

  override def log(s: String): Unit = {
    println(s)
    logs += s
  }
}
