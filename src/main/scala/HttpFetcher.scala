import org.jsoup.Jsoup

import java.net.URL

trait HttpFetcher {
  def fetch(string: String): String
}

object JsoupFetcher extends HttpFetcher {
  override def fetch(url: String): String = Jsoup.connect(url).get().outerHtml()
}
