
import Exceptions.FetchingException
import JsoupWrappers.{JsoupFetcher, PageFetcher}
import org.jsoup.nodes.Document

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 *
 * @param baseUrl Root url to scrape from
 * @param outputPath root directory to save to
 */
class PageScraper(baseUrl: String, outputPath: String) {
  val domain = stripProtocol(baseUrl).split('/')(0)
  val baseURI = new java.net.URI(baseUrl)

  def scrape(logger: Logger = ConsoleLogger, fetcher: PageFetcher[Document] = JsoupFetcher): Unit = {
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
          fetcher.fetchDocument(url)
        }
        catch {
          case e =>
            throw new FetchingException(s"Error when fetching the url: $url", e)

        }
      val outerHtml = document.outerHtml()
      saveHtmlContent(url, outerHtml)

      // Extract and download textbased resources

      val textExtensions = List(".css", ".js", ".json", ".xml", ".svg", ".rss", ".atom", ".csv", ".txt", ".log")
      val textResourceSelector = textExtensions.map(ext => s"[href$$=$ext], [src$$=$ext]").mkString(",")
      val textResources = document.select(textResourceSelector)
        .asScala
        .flatMap(el =>
          List(el.attr("abs:href"), el.attr("abs:src"))
        )
        .distinct
        .filter(r => r.nonEmpty && withinDomain(r))

      textResources.foreach { resourceUrl =>
        val resourceContent = fetcher.fetchDocument(url, true).outerHtml()
        if (resourceContent.nonEmpty)
          saveTextResource(resourceUrl, resourceContent)
      }

      // Extract and download binary resources
      val binaryResources = document.select("[href], [src]").asScala.flatMap(el =>
        List(el.attr("abs:href"), el.attr("abs:src"))
      ).distinct
        .filter(r =>
          r.nonEmpty && !r.endsWith(".html") &&
            withinDomain(r)
        ).filterNot(textResources.contains)

      binaryResources.foreach { resourceUrl =>
        val resourceContent = fetcher.fetchBytes(resourceUrl)
        if (resourceContent.nonEmpty)
          saveBinaryResource(resourceUrl, resourceContent)
      }

      val links = document.select(s"a[href]").asScala.map(_.attr("abs:href")).distinct
      links.filterNot(alreadyVisited).filter(withinDomain).foreach { link =>
        _scrape(link)
      }
    }

    _scrape(baseUrl)

  }

  def saveBinaryResource(url: String, content: Array[Byte]): Unit = {
    val path = Paths.get(getLocalPathResource(url))
    Files.createDirectories(path.getParent)
    Files.write(path, content, StandardOpenOption.CREATE)
  }

  private def withinDomain(url: String): Boolean = {
    url.startsWith("./") || url.startsWith("/") || new java.net.URL(url).getHost == baseURI.getHost
  }

  private def saveTextResource(url: String, content: String): Unit = {
    // add index.html to paths that do not end in .html
    val path = Paths.get(getLocalPathResource(url))
    Files.createDirectories(path.getParent)
    Files.write(path, content.getBytes, StandardOpenOption.CREATE)
  }

  private def saveHtmlContent(url: String, content: String): Unit = {
    // add index.html to paths that do not end in .html
    val path = Paths.get(getLocalPathHtml(url))
    Files.createDirectories(path.getParent)
    Files.write(path, content.getBytes, StandardOpenOption.CREATE)
  }

  private def getLocalPathHtml(absoluteUrl: String): String = {
    val path = Paths.get(outputPath).resolve(cleanPath(stripProtocol(absoluteUrl))).toString
    if (path.endsWith(".html")) path else path + "/index.html"
  }

  private def stripProtocol(url: String) = {
    url.stripPrefix("http://").stripPrefix("https://")
  }

  private def cleanPath(path: String) = {
    path.replaceAll("[^a-zA-Z0-9.-/]", "_")
  }

  private def getLocalPathResource(absoluteUrl: String): String = {
    Paths.get(outputPath).resolve(cleanPath(stripProtocol(absoluteUrl))).toString
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
