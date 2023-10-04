object Main extends App {
  if (args.length != 1)
    println("Usage: sbt run <path to scrape to>")
  else {
    PageScraper(outputPath = args(0)).scrape()
  }
}
