Temple Class Scheduler Backend Scraper

Sebastian Tota

To-do:
* Create database connection in main() to avoid reconnecting on every INSERT
* Scrape JSON data instead of waiting for entire website to load
    * Jackson JSON parser library
* Add campus option selection
* Convert from Selenium to Jsoup for better performance
* Remove hardcoded term
    * Parse term selection numbers