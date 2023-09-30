import java.io.{File, FileWriter}
import java.net.MalformedURLException
import java.nio.file.{Files, Paths}
import scala.util.{Failure, Try}


object PageScraper {
  def scrape(logger: Logger, urlString: String, fetcher: HttpFetcher, outputPath: String = ""): Try[Unit] = {
    Try {
      _scrape(logger, urlString, fetcher, outputPath)
    }.recoverWith {
      case e: MalformedURLException =>
        logger.log("Invalid URL")
        Failure(e)
      case e =>
        logger.log(e.getMessage)
        Failure(e)
    }
  }

  private def _scrape(logger: Logger, url: String, fetcher: HttpFetcher, outputPath: String): Unit = {
    //    new URL(url) // make sure the URL is correct
    val outerHtml = fetcher.fetch(url)

    saveHtmlContent(url, outerHtml, outputPath)
  }

  private def saveHtmlContent(url: String, content: String, basePath: String): Unit = {
    val path = Paths.get(basePath, url.replaceAll("[^a-zA-Z0-9.-]", "_"), "index.html")
    Files.createDirectories(path.getParent)
    val fw = new FileWriter(new File(path.toString))
    fw.write(content)
    fw.close()
  }

}
