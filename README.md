# PageScraper for tretton37

This is a program that will recursively traverse each page in
http://books.toscrape.com and save the contents, including images, scripts
and other resources in a path that is passed as an argument to it. Once it completes you can browse the page from your local file system. 

## Installation

- Clone this repository

### Alternative 1 (run jar file)

There is a pre-packaged jar file in the repo, run it with Java:

```
java -jar PageScraper-assembly-0.1.0-SNAPSHOT.jar <ouput path for scraping>
```

### Alternative 2

- Install scala: https://www.scala-lang.org/download/
- To run tests, in the root of the repo:
  ```bash
  sbt test
  ```
- In the root of the repo:
    ```bash
    sbt "run <valid output path>"
    ```

## Design and some choices

- I chose the Jsoup library to fetch and parse the html files, as it looked like a common tool for this purpose.
In order to be able to mock this external library, I created an abstraction using a trait and inject the dependency in
`Main.scala`. 

- I have tried to keep using the test first method to start simple and then extend the functionalities. In order to test with close-to-real data before the real data, I copied the source html for the start page and used mocking to only fetch the start page, otherwise return an empty shell. 

- There is one ignored test in `PageScraperTest.scala` that scrapes the real page and asserts that the number of pages for books equals to 1000. To enable it just replace `ignore` with `it` for that test in the file. 

- For **parallelisation**, I used parallel collections, inherent to Scala, and branched out to new threads for each page's links.

- **Errors** are gathered in a synchronized list to be logged at the end.

- To avoid **repeated visits** I kept a synchronized map of URLs shared by all the threads.

## Further thoughts
- Obviously this is just limited to the hardcoded http://books.toscrape.com page. The design I submit parameterizes the target under the hoods.
However, there is no further check on the target website, other than checking that we remain in the same domain. For a more extensive scraper that is an obvious problem.  

- This program also trusts the target website completely. A real scraper should not.

- I did not have time to work on the links that are embedded in the resources, such as icons (e.g. stars under each book thumbnail).

- Error handling is also quite crude. Would have liked to improve it.

- If I had spent more time I would work on refactoring `PageScraper.scala` to remove code duplication when saving different types of content. 

Hope this was not a complete waste of your time :)

Peace,
Mehmet
