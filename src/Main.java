import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Scanner;

public class Main {
    // Return correct WebDriver based on clients operating system
    public static WebDriver createDriver(boolean headless) {
        String OS = System.getProperty("os.name");
        if ("Mac OS X".equals(OS)) {// MAC OS
            System.setProperty("webdriver.gecko.driver",
                    "drivers/geckodriver-v26-mac");
        } else if ("Linux".equals(OS)) {// LINUX
            System.setProperty("webdriver.gecko.driver",
                    "drivers/geckodriver-v26-linux");
        } else if ("Windows".equals(OS)) {// WINDOWS
            System.setProperty("webdriver.gecko.driver",
                    "drivers/geckodriver-v26-win");
        }
        return (createFirefox(headless));
    }

    // Create a Firefox WebDriver
    public static WebDriver createFirefox(boolean headless) {
        // Run in headless mode (no visual output)
        if (headless) {
            // Set Firefox Options
            FirefoxOptions options = new FirefoxOptions();
            options.setHeadless(true);
            return new FirefoxDriver(options);
        }
        // Don't run headless
        return new FirefoxDriver();
    }

    // https://www.testingexcellence.com/webdriver-wait-page-load-example-java/
    public static void waitForPageLoaded(WebDriver driver) {
        ExpectedCondition<Boolean> expectation = new
                ExpectedCondition<Boolean>() {
                    public Boolean apply(WebDriver driver) {
                        return ((JavascriptExecutor) driver).executeScript("return document.readyState").
                                toString().equals("complete");
                    }
                };
        try {
            Thread.sleep(1000);
            WebDriverWait wait = new WebDriverWait(driver, 30);
            wait.until(expectation);
        } catch (Throwable error) {
            System.out.println("Timeout waiting for Page Load Request to complete.");
        }
    }

    // Wait for an element based on it's id, then return the element once it is found
    // Return null if element is not found or wait timed out
    public static WebElement waitForElementId(WebDriver driver, String id, int timeoutTime) {
        try{
            WebDriverWait wait = new WebDriverWait(driver, timeoutTime);
            return(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id))));
        } catch (Exception e) {
            System.out.println("ID: " + id);
            System.out.println("Timed out searching for element. Element not present.");
            return null;
        }
    }

    public static List<WebElement> waitForElementClass(WebDriver driver, String className, int timeoutTime) {
        try{
            WebDriverWait wait = new WebDriverWait(driver, timeoutTime);
            return(wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.className(className))));
        } catch (Exception e) {
            System.out.println("Class: " + className);
            System.out.println("Timed out searching for element. Element not present.");
            return null;
        }
    }

    public static void executeJavascript(WebDriver driver, String executeString) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(executeString);
    }

    // Select the term based on user input
    public static int selectTerm(WebDriver driver, String term) {
        try {
            // Wait for term dropdown
            WebElement termInput = waitForElementId(driver, "select2-chosen-1", 30);
            if (termInput == null) { return -1; }
            termInput.click();

            // Enter term into search box
            WebElement termSearchInput = driver.findElement(By.id("s2id_autogen1_search"));
            termSearchInput.sendKeys(term);

            WebElement foundTermButton = driver.findElements(By.xpath("//*[contains(text(), '" + term + "')]")).get(0);
            foundTermButton.click();

            WebElement termContinueButton = waitForElementId(driver, "term-go", 10);
            if (termContinueButton == null) { return -1; }
            termContinueButton.click();

            return 1;
        } catch (Exception e) {
            System.out.println("Failed selecting term");
            return -1;
        }
    }

    public static void findClass(WebDriver driver, String subject, String startCourseNumber,
                                 String endCourseNumber) {
        // String[] classInfo = className.split(" ");
        // System.out.println("Class Info: " + classInfo[0] + " + " + classInfo[1]);
        waitForPageLoaded(driver);

        try {
            // BUG FIX: Keep both waits
            // executeJavascript(driver, "document.getElementById('select2-drop-mask').style.display = 'none';");
            waitForElementId(driver, "search-go", 60); // Wait for search button
            waitForElementId(driver, "s2id_txt_subject", 60);

            try {
                executeJavascript(driver, "document.getElementById('splash').style.display = 'none';");
            } catch (Exception e){
                System.out.println("No splash screen found");
            }

            WebElement classInput = waitForElementId(driver, "s2id_txt_subject", 60);
            classInput.click(); // Click inside the 'Subject' textbox

            // Wait for subject dropdown to populate
            waitForElementClass(driver, "select2-result-label", 30);

            // BUG FIX: Disable the mask preventing key stroke input
            executeJavascript(driver,
                    "document.getElementById('select2-drop-mask').style.display = 'none';");
            waitForElementId(driver, "s2id_autogen1", 20);
            driver.findElement(By.id("s2id_autogen1")).sendKeys(subject); // Enter subject abrev

            waitForElementId(driver, subject, 10); // Wait for the subject dropdown to populate
            driver.findElement(By.id(subject)).click();

            // Enter start and end course number into 'Course Number Range' textboxes
            driver.findElement(By.id("txt_course_number_range")).sendKeys(startCourseNumber);
            driver.findElement(By.id("txt_course_number_range_to")).sendKeys(endCourseNumber);

            driver.findElement(By.id("search-go")).click();
        } catch (Exception e) {
            System.out.println("Error finding class: " + e);
        }
    }

    // Parse the meeting time section of each class
    // Returns a string in the format DDDHHHHHHHH, (first 3 letters of the day, start hour, end hour)
    // time is in 24hr format. More info in README.md
    public static String parseSchedule(WebDriver driver, WebElement schedule) {
        List<WebElement> meetings = schedule.findElements(By.className("meeting"));
        int counter = 1;
        StringBuilder parsedMeeting = new StringBuilder();
        for (WebElement meeting : meetings) {
            String daysUnparsed = meeting.findElement(
                    By.className("ui-pillbox-summary")).getAttribute("innerHTML");
            String time = meeting.findElement(
                    By.cssSelector("div:nth-child(" + counter + ") > span:nth-child(2)")).getText();
            counter++;

            // Parse start and end time
            String[] timeSplit = time.split(" - ");
            String startTime;
            String endTime;
            try {
                // Parse the format and compress it
                SimpleDateFormat displayFormat = new SimpleDateFormat("HHmm");
                SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a");
                startTime = displayFormat.format(parseFormat.parse(timeSplit[0]));
                endTime = displayFormat.format(parseFormat.parse(timeSplit[1]));

                // Parse days of the week
                // Clean and split the daysUnparsed string
                String[] days = daysUnparsed.replace(" ", "").split(",");
                for (String day : days) {
                    day = day.substring(0, 3); // Get first 3 letters of each day the class is held
                    parsedMeeting.append(day).append(startTime).append(endTime).append(",");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return(parsedMeeting.toString());
    }

    public static void parseClasses(WebDriver driver) {
        waitForPageLoaded(driver);
        waitForElementId(driver, "table1", 10);

        waitForElementClass(driver, "odd", 30);
        List<WebElement> classList = driver.findElements(By.className("odd"));
        classList.addAll(driver.findElements(By.className("even")));

        for (WebElement classElem : classList) {
            String campus = classElem.findElement(By.cssSelector("td:nth-child(3)")).getText();

            // Only add classes on Main campus
            if (campus.equals("Main")) {
                Integer crn = Integer.parseInt(classElem.findElement(By.cssSelector("td:nth-child(1)")).getText());
                String subject = classElem.findElement(By.cssSelector("td:nth-child(2)")).getText().split(",")[0];
                Integer creditHours = Integer.parseInt(
                        classElem.findElement(By.cssSelector("td:nth-child(4)")).getText());
                String title = classElem.findElement(By.cssSelector("td:nth-child(5)")).getText();
                String capacity = classElem.findElement(By.cssSelector("td:nth-child(7)")).getText();
                String instructor = classElem.findElement(By.cssSelector("td:nth-child(8) > a:nth-child(1)")).getText();
                WebElement schedule = classElem.findElements(By.cssSelector
                        ("[data-property='meetingTime']")).get(0);
                String scheduleParsed = parseSchedule(driver, schedule);
                // System.out.println(scheduleParsed);
                insertClassSQL(crn, subject, creditHours, title, capacity, instructor, scheduleParsed);
            }
        }
    }

    public static void insertClassSQL(Integer crn, String subject, Integer creditHours, String title,
                                 String capacity, String instructor, String schedule) {

        try {
            String url = "jdbc:mysql://" + System.getenv("AWS_URL") + "/class_scheduler";
            Connection conn = DriverManager.getConnection(
                    url, System.getenv("AWS_USER"), System.getenv("AWS_PASS"));
            String query = null;
            try {
                query = "INSERT IGNORE INTO Classes (crn, subject, creditHours, title, capacity, instructor, schedule) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            } catch(Exception e) {
                conn.close();
            }

            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setInt(1, crn);
            preparedStmt.setString(2, subject);
            preparedStmt.setInt(3, creditHours);
            preparedStmt.setString(4, title);
            preparedStmt.setString(5, capacity);
            preparedStmt.setString(6, instructor);
            preparedStmt.setString(7, schedule);
            preparedStmt.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Reset browse classes page to go back to page that allows new search
    public static void newSearch(WebDriver driver) {
        String searchAgainButtonId = "search-again-button";
        String clearSearchButtonId = "search-clear";
        try {
            // On class results screen click 'search again' button
            WebElement searchAgainButton = waitForElementId(driver, searchAgainButtonId, 10);
            if (searchAgainButton != null) {
                searchAgainButton.click();
            } else {
                System.out.println("Error waiting for 'search again' button to reset new search");
            }

            // On search page click clear to start new search
            WebElement clearButton = waitForElementId(driver, clearSearchButtonId, 30);
            if (clearButton != null) {
                clearButton.click();
            } else {
                System.out.println("Error clearing search after starting new search");
            }

        } catch (Exception e) {
            System.out.println("Error resting search!");
            System.out.println(e);
        }

    }

    // args = [Term, Subject, StartCourseNumber, EndCourseNumber]
    // If no EndCourseNumber specified then only search for specific course number
    public static void main (String[] args) {
        String baseurl = "https://www.temple.edu/apply/common/cdcheck.asp";

        // DEFAULT VALUES
        String term;
        String subject;
        String startCourseNumber;
        String endCourseNumber;

        term = "2020 Fall";
        subject = "CIS";
        startCourseNumber = "0";
        endCourseNumber = "4999";

        /*
        // Assign values based on arguments, or lack there of (default values for testing)
        if (args.length == 0) {
            // No arguments specified (testing)
            term = "2020 Fall";
            subject = "CIS";
            startCourseNumber = "0";
            endCourseNumber = "4999";
        } else {
            // Use arguments specified
            term = args[0];
            subject = args[1];
            startCourseNumber = args[2];
            endCourseNumber = args[3];
        }
         */

        WebDriver driver = createDriver(false);
        driver.get(baseurl);

        try {
            selectTerm(driver, term);
            findClass(driver, subject, startCourseNumber, endCourseNumber);
            parseClasses(driver);
            newSearch(driver);
            driver.close();
        } catch (Exception e) {
            System.out.println("Error executing program!");
            System.out.println(e);
            driver.close();
        }

    }

}
