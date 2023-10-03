package JsoupWrappers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

trait PageFetcher[D] {
  def fetchDocument(url: String, ignoreContentType: Boolean = false): D

  def fetchBytes(url: String): Array[Byte]
}

object JsoupFetcher extends PageFetcher[Document] {
  override def fetchDocument(url: String, ignoreContentType: Boolean): Document =
    Jsoup.connect(url).ignoreContentType(ignoreContentType).get()

  override def fetchBytes(url: String): Array[Byte] =
    Jsoup.connect(url).ignoreContentType(true).execute().bodyAsBytes()
}
