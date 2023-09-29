trait Logger {
  def log(s: String): Unit
}

object ConsoleLogger extends Logger {
  override def log(s: String): Unit = println(s)
}