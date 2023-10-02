package JsoupWrappers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

trait DocumentFetcher {
  def fetch(url: String): Document
}

object JsoupFetcher extends DocumentFetcher {
  override def fetch(url: String): Document = Jsoup.connect(url).get()
}
