import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static java.lang.System.exit;
import java.io.FileReader;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Main {
    // Return correct WebDriver based on clients operating system
    public static WebDriver createDriver(boolean headless) {
        String OS = System.getProperty("os.name");
        if (OS.equals("Mac OS X")) {// MAC OS
            System.setProperty("webdriver.gecko.driver",
                    "drivers/geckodriver-v26-mac");
        } else if (OS.equals("Linux")) {// LINUX
            System.setProperty("webdriver.gecko.driver",
                    "drivers/geckodriver-v26-linux");
        } else if (OS.contains("Windows")) {// WINDOWS
            System.setProperty("webdriver.gecko.driver",
                    "drivers/geckodriver-v26-win.exe");
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

            Objects.requireNonNull(waitForElementId(driver, "202036", 10)).click();

            WebElement termContinueButton = waitForElementId(driver, "term-go", 10);
            if (termContinueButton == null) { return -1; }
            termContinueButton.click();

            return 1;
        } catch (Exception e) {
            System.out.println("Failed selecting term: " + e);
            return -1;
        }
    }

    // Builds the url that is needed to search for a specific class
    // Class term is hardcoded '202036'
    public static void classSearch(WebDriver driver, String subject, String startCourseNumber,
                                 String endCourseNumber) {
        System.out.println("Creating class search url");
        System.out.println("Searching for class");
        String classUrlSearch = "https://prd-xereg.temple.edu/StudentRegistrationSsb/ssb/searchResults/searchResults?txt_subject=" +
                subject +
                "&txt_course_number_range=" +
                startCourseNumber +
                "&txt_course_number_range_to=" +
                endCourseNumber +
                "&txt_term=202036&pageMaxSize=100000&sortDirection=asc";
        driver.get(classUrlSearch);
        System.out.println("Completed searching for class");
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
        StringBuilder schedule = new StringBuilder("");

        try {
            for (Object o : meetingArray) {
                JSONObject meetingObj = (JSONObject) o;
                JSONObject meeting = (JSONObject) meetingObj.get("meetingTime");

                // Check if class is online only
                if (meeting.get("meetingType") == "OLL") { return "Online"; }

                // Time class occurs
                String startTime = (String) meeting.get("beginTime");
                String endTime = (String) meeting.get("endTime");

                // What days of the week the class occurs
                Boolean mon = (Boolean) meeting.get("monday");
                Boolean tue = (Boolean) meeting.get("tuesday");
                Boolean wed = (Boolean) meeting.get("wednesday");
                Boolean thu = (Boolean) meeting.get("thursday");
                Boolean fri = (Boolean) meeting.get("friday");

                if (mon) {
                    appendScheduleString(schedule, "Mon", startTime, endTime);
                }
                if (tue) {
                    appendScheduleString(schedule, "Tue", startTime, endTime);
                }
                if (wed) {
                    appendScheduleString(schedule, "Wed", startTime, endTime);
                }
                if (thu) {
                    appendScheduleString(schedule, "Thu", startTime, endTime);
                }
                if (fri) {
                    appendScheduleString(schedule, "Fri", startTime, endTime);
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing schedule: " + e);
            return "";
        }
        return schedule.toString();
    }

    public static void parseClasses(WebDriver driver, String term, Connection conn) {
        waitForPageLoaded(driver);
        JSONParser jsonParser = new JSONParser();
        try {
            // Firefox ONLY - Show raw data instead of JSON format
            try { waitForElementId(driver, "rawdata-tab", 10).click(); }
            catch (Exception e) {System.out.println("Can not find rawdata-tab button!"); }

            // Extract raw data response
            String data = driver.findElement(By.cssSelector("pre")).getText();

            // FileReader data = new FileReader("C:\\Users\\anon\\Documents\\Projects\\Class-Scheduler\\Temple-Class-Scheduler-Scraper\\ExampleResponse.json");

            JSONObject obj = (JSONObject) jsonParser.parse(data);
            JSONArray classes = (JSONArray) obj.get("data");

            int crn = -1;
            for (int i = 0; i < classes.size(); i++) {
                try {
                    JSONObject aClass = (JSONObject) classes.get(i);

                    // Get faculty object
                    String instructor = "";
                    try {
                        JSONArray aFacultyArr = (JSONArray) (((JSONObject) classes.get(i)).get("faculty"));
                        JSONObject aFaculty = (JSONObject)aFacultyArr.get(0);
                        instructor = (String) aFaculty.get("displayName");
                    } catch(Exception ignore) {
                        System.out.println("Can't find instructor for crn: " + crn);
                    }

                    // Get schedule object
                    JSONArray aMeetingArr = (JSONArray) ((JSONObject) classes.get(i)).get("meetingsFaculty");
                    String schedule = parseSchedule(aMeetingArr);

                    String campus = null;
                    try { campus = (String) aClass.get("campusDescription"); } catch (Exception ignore) {}

                    crn = Integer.parseInt((String)aClass.get("courseReferenceNumber"));
                    String subject = (String) aClass.get("subject");
                    String subjectCourse = (String) aClass.get("subjectCourse");
                    Integer courseNumber =  Integer.parseInt(((String)aClass.get("courseNumber")));
                    Integer creditHours = (int) (long) aClass.get("creditHourLow");
                    String title = (String) aClass.get("courseTitle");

                    // Parse capacity info
                    Integer capacity = (int) (long) aClass.get("maximumEnrollment");
                    Integer currentCapacity = (int)(long) aClass.get("seatsAvailable");
                    boolean full = false;
                    if ((capacity - currentCapacity) >= capacity) { full = true; }

                    if (instructor.equals("")) { instructor = null; }
                    if (schedule.equals("")) { schedule = null; }

                    // Insert to sql
                    insertClassSQL(term, crn, subject, courseNumber, subjectCourse, creditHours, title, capacity,
                            currentCapacity, full, instructor, schedule, campus, conn);

                } catch (Exception e) {
                    System.out.println("Error parsing class crn: " + Integer.toString(crn) + "! Error: " + e);
                }

            }

        } catch (Exception e) {
            System.out.println("Error parsing class: " + e);
        }

    }

    public static void insertClassSQL(String term, Integer crn, String subject, Integer courseNumber, String subjectCourse,
                                      Integer creditHours, String title, Integer capacity, Integer currentCapacity,
                                      boolean capacityFull, String instructor, String schedule, String campus,
                                      Connection conn) {
        try {
            // Avoid duplicate inserts
            // https://stackoverflow.com/questions/61069118/java-sql-insert-into-table-only-new-entries
            String query = null;
            try {
                // Insert new class if it doesn't exist, and update capacity if class already exists in database
                query = "INSERT INTO Classes (" +
                        "crn, subject, courseNumber, subjectCourse, creditHours, title, " +
                        "capacity, currentCapacity, capacityFull, instructor, schedule, campus) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE capacity=?, currentCapacity=?, capacityFull=?," +
                        "instructor=?, schedule=?";
            } catch(Exception e) {
                conn.close();
            }

            PreparedStatement preparedStmt = conn.prepareStatement(query);
            // Insert New
            preparedStmt.setInt(1, crn);
            preparedStmt.setString(2, subject);
            preparedStmt.setInt(3, courseNumber);
            preparedStmt.setString(4, subjectCourse);
            preparedStmt.setInt(5, creditHours);
            preparedStmt.setString(6, title);
            preparedStmt.setInt(7, capacity);
            preparedStmt.setInt(8, currentCapacity);
            preparedStmt.setBoolean(9, capacityFull);
            preparedStmt.setString(10, instructor);
            preparedStmt.setString(11, schedule);
            preparedStmt.setString(12, campus);

            // Update
            preparedStmt.setInt(13, capacity);
            preparedStmt.setInt(14, currentCapacity);
            preparedStmt.setBoolean(15, capacityFull);
            preparedStmt.setString(16, instructor);
            preparedStmt.setString(17, schedule);

            preparedStmt.executeUpdate();
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

        int testing = 1;
        if (testing == 0) {
            parseClasses(null, term, conn);
            exit(-1);
        }

        WebDriver driver = createDriver(false);
        driver.get("https://prd-xereg.temple.edu/StudentRegistrationSsb/ssb/classSearch/classSearch");

        // Assign values based on arguments, or lack there of (default values for testing)
        if (args.length == 0) {
            // No arguments specified (testing)
            subject = "";
            // Default course numbers 0-4999 includes all undergrad classes (5000+ are grad classes)
            startCourseNumber = "1000";
            endCourseNumber = "2000";
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
            parseClasses(driver, term, conn);
            driver.close();
        } catch (Exception e) {
            System.out.println("Error executing program!");
            System.out.println(e);
            driver.close();
        }


        try {
            conn.close(); // Close SQL connection
        } catch (Exception e) { System.out.println("Error close SQL connection: " + e); }


    }

}
