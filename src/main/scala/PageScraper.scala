
import Exceptions.FetchingException
import JsoupWrappers.{DocumentFetcher, JsoupFetcher}

import java.io.{File, FileWriter}
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.ListHasAsScala

class PageScraper(baseUrl: String, outputPath: String) {
  val domain = baseUrl.stripPrefix("http://").stripPrefix("https://").split('/')(0)
  val baseURI = new java.net.URI(baseUrl)

  def scrape(logger: Logger = ConsoleLogger, fetcher: DocumentFetcher = JsoupFetcher): Unit = {
    val visitedUrls = collection.mutable.Set.empty[String]

    def alreadyVisited(url: String): Boolean = {
      visitedUrls.contains(url) || visitedUrls.contains(url.stripSuffix("index.html"))
    }

    // recursive closure
    def _scrape(url: String): Unit = {
      logger.log(s"Processing $url")
      visitedUrls += url
      val document =
        try {
          fetcher.fetch(url)
        }
        catch {
          case e =>
            throw new FetchingException(s"Error when fetching the url: $url", e)

        }
      val outerHtml = document.outerHtml()
      saveHtmlContent(url, outerHtml)
      val links = document.select(s"a[href]").asScala.map(_.attr("abs:href")).distinct
      links.filterNot(alreadyVisited).filter(withinDomain).foreach { link =>
        _scrape(link)
      }
    }

    _scrape(baseUrl)

  }

  private def withinDomain(url: String): Boolean = {
    new java.net.URL(url).getHost == baseURI.getHost
  }

  private def saveHtmlContent(url: String, content: String): Unit = {
    // add index.html to paths that do not end in .html
    val path = Paths.get(getLocalPath(url))
    Files.createDirectories(path.getParent)
    val fw = new FileWriter(new File(path.toString))
    fw.write(content)
    fw.close()
  }

  private def getLocalPath(url: String): String = {
    val domainPath = Paths.get(outputPath).resolve(
      domain.replaceAll("[^a-zA-Z0-9.-/]", "_")
    )
    val urlPath = domainPath.resolve(
      baseURI.relativize(new java.net.URI(url)).getPath).toString

    if (urlPath.endsWith(".html")) urlPath else urlPath + "/index.html"

  }
}

object PageScraper extends App {
  // app config via environment variables
  val outputPath = defaultedEnvVar("SCRAPER_OUTPUT_PATH", "tmp")
  val baseUrl = defaultedEnvVar("SCRAPER_TARGET_URL", "https://books.toscrape.com/")

  def defaultedEnvVar(varName: String, default: String): String = {
    val res = System.getenv(varName)
    if (res.isEmpty) default else res
  }

  def apply(baseUrl: String = baseUrl, outputPath: String = outputPath): PageScraper =
    new PageScraper(baseUrl, outputPath)

}
