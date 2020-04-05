import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileReader;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;

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

            /*
            WebElement foundTermButton = driver.findElements(By.xpath("//*[contains(text(), '" + term + "')]")).get(0);
            foundTermButton.click();
             */

            waitForElementId(driver, "202036", 10).click();

            WebElement termContinueButton = waitForElementId(driver, "term-go", 10);
            if (termContinueButton == null) { return -1; }
            termContinueButton.click();

            return 1;
        } catch (Exception e) {
            System.out.println("Failed selecting term");
            return -1;
        }
    }

    public static void classSearch(WebDriver driver, String subject, String startCourseNumber,
                                 String endCourseNumber) {
        StringBuilder classUrlSearch = new StringBuilder();
        classUrlSearch.append("https://prd-xereg.temple.edu/StudentRegistrationSsb/ssb/searchResults/searchResults?txt_subject=");
        classUrlSearch.append(subject);
        classUrlSearch.append("&txt_course_number_range=");
        classUrlSearch.append(startCourseNumber);
        classUrlSearch.append("&txt_course_number_range_to=");
        classUrlSearch.append(endCourseNumber);
        classUrlSearch.append("&txt_term=202036&pageMaxSize=100&sortDirection=asc");

        driver.get(classUrlSearch.toString());
    }

    public static void appendScheduleString(StringBuilder schedule, String day, String startTime, String endTime) {
        schedule.append(day);
        schedule.append(startTime);
        schedule.append(endTime);
        schedule.append(",");
    }

    // Parse the meeting time section of each class
    // Returns a string in the format DDDHHHHHHHH, (first 3 letters of the day, start hour, end hour)
    // time is in 24hr format. More info in README.md
    public static String parseSchedule(JSONArray meetingArray) {
        StringBuilder schedule = new StringBuilder();

        Iterator i = meetingArray.iterator();
        while(i.hasNext()) {
            JSONObject meetingObj = (JSONObject) i.next();
            JSONObject meeting = (JSONObject) meetingObj.get("meetingTime");

            // Time class occurs
            String startTime = (String) meeting.get("beginTime");
            String endTime = (String) meeting.get("endTime");



            // What days of the week the class occurs
            Boolean mon = (Boolean) meeting.get("monday");
            Boolean tue = (Boolean) meeting.get("tuesday");
            Boolean wed = (Boolean) meeting.get("wednesday");
            Boolean thu = (Boolean) meeting.get("thursday");
            Boolean fri = (Boolean) meeting.get("friday");

            if (mon) { appendScheduleString(schedule, "Mon", startTime, endTime); }
            if (tue) { appendScheduleString(schedule, "Tue", startTime, endTime); }
            if (wed) { appendScheduleString(schedule, "Wed", startTime, endTime); }
            if (thu) { appendScheduleString(schedule, "Thu", startTime, endTime); }
            if (fri) { appendScheduleString(schedule, "Fri", startTime, endTime); }
        }
        return schedule.toString();
    }

    public static void parseClasses(WebDriver driver, Connection conn) {
        // waitForPageLoaded(driver);
        JSONParser jsonParser = new JSONParser();
        try {
            // Firefox ONLY - Show raw data instead of JSON format
            // waitForElementId(driver, "rawdata-tab", 10).click();

            // Extract raw data response
            // String data = driver.findElement(By.cssSelector("pre")).getText();
            FileReader data = new FileReader("/Users/sebastiantota/Documents/Projects/Class Scheduler/Temple-Class-Scheduler-Scraper/testing.json");

            JSONObject obj = (JSONObject) jsonParser.parse(data);
            JSONArray classes = (JSONArray) obj.get("data");

            for (int i = 0; i < classes.size(); i++) {
                JSONObject aClass = (JSONObject) classes.get(i);

                // Get faculty object
                JSONArray aFacultyArr = (JSONArray) (((JSONObject) classes.get(i)).get("faculty"));
                JSONObject aFaculty = (JSONObject)aFacultyArr.get(0);

                // Get schedule object
                JSONArray aMeetingArr = (JSONArray) ((JSONObject) classes.get(i)).get("meetingsFaculty");

                Integer crn = Integer.parseInt((String)aClass.get("courseReferenceNumber"));
                String subject = (String) aClass.get("subject");
                Integer creditHours = (int) (long) aClass.get("creditHourLow");
                String title = (String) aClass.get("courseTitle");
                Integer capacity = (int) (long) aClass.get("seatsAvailable");
                String instructor = (String) aFaculty.get("displayName");
                String schedule = parseSchedule(aMeetingArr);

                System.out.println(crn);
                System.out.println(subject);
                System.out.println(creditHours);
                System.out.println(title);
                System.out.println(capacity);
                System.out.println(instructor);
                System.out.println(schedule);
                System.out.println("");

            }

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public static void insertClassSQL(Integer crn, String subject, Integer creditHours, String title,
                                 String capacity, String instructor, String schedule, Connection conn) {
        try {
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

    // args = [Subject, StartCourseNumber, EndCourseNumber]
    // If no EndCourseNumber specified then only search for specific course number
    public static void main (String[] args) {
        Scanner scr = new Scanner(System.in);
        WebDriver driver = null;
        Connection conn = null;
        parseClasses(driver, conn);
        scr.nextLine();

        /*
        String url = "jdbc:mysql://" + System.getenv("AWS_URL") + "/class_scheduler";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    url, System.getenv("AWS_USER"), System.getenv("AWS_PASS"));
        } catch (Exception e) {
            System.out.println("Error connecting to database: " + e);
        }

        String baseurl = "https://www.temple.edu/apply/common/cdcheck.asp";

        // DEFAULT VALUES
        String term = "2020 Fall";
        String subject;
        String startCourseNumber;
        String endCourseNumber;

        WebDriver driver = createDriver(true);
        driver.get("https://prd-xereg.temple.edu/StudentRegistrationSsb/ssb/classSearch/classSearch");


        // Assign values based on arguments, or lack there of (default values for testing)
        if (args.length == 0) {
            // No arguments specified (testing)
            subject = "CIS";
            startCourseNumber = "2166";
            endCourseNumber = "2166";
        } else {
            // Use arguments specified
            subject = args[0];
            startCourseNumber = args[1];
            endCourseNumber = args[2];
        }

        driver.get(baseurl);

        try {
            selectTerm(driver, term);
            classSearch(driver, subject, startCourseNumber, endCourseNumber);
            parseClasses(driver, conn);
            driver.close();
        } catch (Exception e) {
            System.out.println("Error executing program!");
            System.out.println(e);
            driver.close();
        }

         */


    }

}
