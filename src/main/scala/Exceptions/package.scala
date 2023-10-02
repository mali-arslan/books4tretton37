package object Exceptions {
  class FetchingException(message: String, val underlying: Throwable) extends Exception(message)

}
