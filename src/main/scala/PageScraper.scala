import java.net.{MalformedURLException, URL}
import scala.util.{Failure, Try}

class PageScraper {
  def scrape(logger: Logger, urlString: String): Try[Unit] = {
    Try {
      _scrape(logger, urlString)
    }.recoverWith {
      case e: MalformedURLException =>
        logger.log("Invalid URL")
        Failure(e)
    }
  }

  def _scrape(logger: Logger, urlString: String): Unit = {
    val url = new URL(urlString)
  }

}
