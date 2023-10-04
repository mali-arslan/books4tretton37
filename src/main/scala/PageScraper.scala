
import Exceptions.FetchingException
import JsoupWrappers.{JsoupFetcher, PageFetcher}
import org.jsoup.nodes.Document

import java.net.URL
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.collection.parallel.CollectionConverters.{ImmutableIterableIsParallelizable, IterableIsParallelizable}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.jdk.CollectionConverters.{CollectionHasAsScala, ListHasAsScala}

/**
 *
 * @param baseUrl    Root url to scrape from
 * @param outputPath root directory to save to
 */
class PageScraper(baseUrl: String, outputPath: String) {
  val domain = stripProtocol(baseUrl).split('/')(0)
  val baseURI = new java.net.URI(baseUrl)

  def scrape(logger: Logger = ConsoleLogger, fetcher: PageFetcher[Document] = JsoupFetcher): Unit = {
    println(s"Scraping initialized for $baseUrl, into $outputPath")
    val visitedUrls = collection.concurrent.TrieMap.empty[String, Unit]
    // use TrieMap here as well?
    val failures = new java.util.concurrent.ConcurrentLinkedQueue[(String, Throwable)]()
    //collection.concurrent.TrieMap.empty[String, Future[Unit]]
    implicit val ec = ExecutionContext.global

    def alreadyVisited(url: String): Boolean = {
      visitedUrls.contains(url) || visitedUrls.contains(url.stripSuffix("index.html"))
    }

    // recursive closure

    def _scrapeWithErrorTracking(url: String): Unit = {
      try
        _scrape(url)
      catch {
        case e => failures.add(url -> e)
      }
    }

    def _scrape(url: String): Unit = {
      logger.log(s"Processing $url")
      visitedUrls.putIfAbsent(url, ())
      val document = fetcher.fetchDocument(url)
      val outerHtml = document.outerHtml()
      saveHtmlContent(url, outerHtml)

      // Extract links
      val links = document.select(s"a[href]").asScala
        .map(_.attr("abs:href"))
        .distinct
        .filterNot(alreadyVisited)
        .filter(withinDomain)
        .toList

      visitedUrls.addAll(links.map(_ -> ()))

      // Extract and download textbased resources

      val textExtensions = List(".css", ".js", ".json", ".xml", ".svg", ".rss", ".atom", ".csv", ".txt", ".log")
      val textResourceSelector = textExtensions.map(ext => s"[href$$=$ext], [src$$=$ext]").mkString(",")
      val textResources = document.select(textResourceSelector)
        .asScala
        .flatMap(el =>
          List(el.attr("abs:href"), el.attr("abs:src"))
        )
        .distinct
        .filter(r => r.nonEmpty && withinDomain(r) && !alreadyVisited(r))
      visitedUrls.addAll(textResources.map(_ -> ()))
      textResources.par.foreach { resourceUrl =>
        val resourceContent = fetcher.fetchDocument(resourceUrl, true).html()
        if (resourceContent.nonEmpty)
          saveTextResource(resourceUrl, resourceContent)
      }

      // Extract and download binary resources
      val binaryResources = document.select("[href], [src]").asScala.flatMap(el =>
        List(el.attr("abs:href"), el.attr("abs:src"))
      ).distinct
        .filter(r => r.nonEmpty && !r.endsWith(".html") && withinDomain(r) && !alreadyVisited(r)
        ).filterNot(textResources.contains)
      visitedUrls.addAll(binaryResources.map(_ -> ()))
      binaryResources.par.foreach { resourceUrl =>
        val resourceContent = fetcher.fetchBytes(resourceUrl)
        if (resourceContent.nonEmpty)
          saveBinaryResource(resourceUrl, resourceContent)
      }


      println(s"Scraping complete for $url. Continuing with its links")
      links.par.foreach(_scrapeWithErrorTracking)

    }

    def now = System.currentTimeMillis()

    val start = now
    _scrapeWithErrorTracking(baseUrl)
    logger.log(s"Number of links scraped: ${visitedUrls.size}")
    logger.log(s"Time spent: ${Duration(now - start, MILLISECONDS).toSeconds}")
    logger.log("List of scraping failures with reasons:")
    failures.asScala.foreach {
      case (u, t) => logger.log(s"$u - ${t.getMessage}")
    }
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
    val path = Paths.get(outputPath).resolve(stripProtocol(absoluteUrl)).toString
    if (path.endsWith(".html")) path else path + "/index.html"
  }

  private def stripProtocol(url: String) = {
    url.stripPrefix("http://").stripPrefix("https://")
  }

  private def getLocalPathResource(absoluteUrl: String): String = {
    Paths.get(outputPath).resolve(stripProtocol(absoluteUrl)).toString
  }
}

object PageScraper {
  // app config via environment variables
  val outputPath = defaultedEnvVar("SCRAPER_OUTPUT_PATH", "tmp")
  val baseUrl = defaultedEnvVar("SCRAPER_TARGET_URL", "https://books.toscrape.com/")

  def defaultedEnvVar(varName: String, default: String): String = {
    val res = System.getenv(varName)
    if (res == null) default else res
  }

  def apply(baseUrl: String = baseUrl, outputPath: String = outputPath): PageScraper =
    new PageScraper(baseUrl, outputPath)

  def apply(): PageScraper =
    new PageScraper(baseUrl, outputPath)

}
