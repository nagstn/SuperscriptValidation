package org.myPack; // Corrected package declaration

// --- Essential Imports ---
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.junit.Assert; // Uncomment if you add JUnit assertions

import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


// AShot Imports (Recommended)
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;

// Java IO and Graphics Imports
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SuperscriptStepDefinitions {


    // Make this a top-level public class
    public static class SuperscriptInfo { // Corrected class declaration
        String tabIdentifier; // Or page identifier
        String superscriptText;
        String precedingText;
        boolean isHyperlink;
        String linkHref;
        PositionStatus positionStatus; // Use the enum defined here
        String verificationMessage;

        // Enum for position status - defined WITHIN the top-level class
        public enum PositionStatus {
            ABOVE_BASELINE, // Pass condition
            SAME_LINE,      // Fail condition
            CHECK_ERROR,    // Error during check
            NOT_APPLICABLE // e.g., if element was stale before check
        }

        // Add constructor or setters if needed
        // Add a meaningful toString() if needed for debugging
        @Override
        public String toString() {
            return "SuperscriptInfo{" + /* ... fields ... */ "}";
        }
    }
    // Make this a top-level public class
    public static class VerificationSummary { // Corrected class declaration
        int totalSuperscriptsFoundInPanel = 0;
        int numericSuperscriptsProcessed = 0;
        int positionCheckPassed = 0;
        int positionCheckFailed = 0;
        int positionCheckErrors = 0;
        int hyperlinkCount = 0;
        int noHyperlinkCount = 0;
        List<String> emptySuperscriptLogs = new ArrayList<>();
        List<String> nonNumericSuperscriptLogs = new ArrayList<>();

        // Add constructor or setters if needed
        // Add a meaningful toString() if needed for debugging
        @Override
        public String toString() {
            return "Summary{" + /* ... fields ... */ "}";
        }
    }
    // --- Instance variables (Reset for each scenario by Cucumber) ---
    private static WebDriver driver;
    private static PageObjects page;
    private String pageTitleFromDriver;
    private String currentURL; // Set from feature file
    private Map<String, Integer> expectedSuperscriptCounts; // For validation if needed later
    private boolean modalWasDisplayed = false;
    private long timeout = 10;

    // --- Static variables (Shared across scenarios in a single run, accessed by Hooks) ---
    public static Workbook workbook;
    public static String timestamp;
    // Detail Sheet for individual superscript data
    private static Sheet detailSheet;
    private static int detailRowCount = 0; // Tracks next available row in detailSheet
    // Summary Sheet for per-tab/page counts
    private static String getSafeFilenameBase;
    private static Sheet summarySheet;
    private static int summaryRowCount = 1; // Tracks next available data row in summarySheet (row 0 is header)

    // Collections storing data across scenarios for final Excel export
    public static List<SuperscriptInfo> allSuperscriptDetails = new ArrayList<>();
    public static Map<String, VerificationSummary> tabSummaries = new LinkedHashMap<>();

    // --- Constants for Titles (Update with EXACT titles from your application) ---
    private static final String PAGE_TITLE_ED = "Everyday Checking";
    private static final String PAGE_TITLE_CABA = "Choose a bank account"; // Verify this title if used
    private static final String PAGE_TITLE_CAB = "Clear Access Banking";
    private static final String CHECKING_ACCOUNTS_LANDING = "Checking Accounts: Open Online Today | Wells Fargo";
    private static final String SAVINGS_ACCOUNTS_LANDING = "Open a Savings Account Online | Wells Fargo";

    // --- Constructor ---
    public SuperscriptStepDefinitions() {
        // Called by Cucumber for each scenario
        System.out.println("SuperscriptStepDefinitions instance created.");
    }

    // --- @Given Steps ---
    @Given("I have a Chrome browser for Superscript extraction")
    public void i_have_a_chrome_browser_for_superscript_extraction() {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\nagar\\IdeaProjects\\DataValidationRates\\src\\main\\resources\\chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); // Adjust implicit wait if needed
        driver.manage().window().maximize();

        expectedSuperscriptCounts = new HashMap<>(); // Initialize map for expectations

        // --- Initialize Workbook and Sheets ONCE per test run ---
        if (timestamp == null) {
            System.out.println("--- Initializing Workbook and Sheets (First Run) ---");
            timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            workbook = new XSSFWorkbook(); // Create the static workbook object

            // Detail Sheet Setup
            detailSheet = workbook.createSheet("SuperscriptDetail");
            detailRowCount = 0; // Reset row count for header
            Row headerRow = detailSheet.createRow(detailRowCount++); // Add header row 0, increment to 1
            String[] headers = {"Tab/Page Identifier", "Superscript Text", "Preceding Text", "Is Hyperlink", "Link Href", "Position Status", "Position Result Msg"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i); cell.setCellValue(headers[i]);
            }

            // Summary Sheet Setup
            summarySheet = workbook.createSheet("ValidationSummary");
            summaryRowCount = 0; // Reset row count for header
            Row summaryHeader = summarySheet.createRow(summaryRowCount++); // Add header row 0, increment to 1
            String[] summaryHeaders = {"Tab/Page Identifier", "Total Found", "Numeric Processed", "Position Passed", "Position Failed", "Position Errors", "Hyperlinks Found", "No Hyperlinks", "Empty Tags", "Non-Numeric"};
            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell cell = summaryHeader.createCell(i); cell.setCellValue(summaryHeaders[i]);
            }
            System.out.println("Workbook and sheets initialized. Timestamp: " + timestamp);
        } else {
            System.out.println("--- Reusing existing Workbook ---");
        }
    }

    @Given("I navigate to the web page {string}")
    public void i_navigate_to_the_web_page(String url) {
        currentURL = url; // Capture URL from feature file Example
        System.out.println("Attempting to navigate to: " + currentURL);
        try {
            driver.get(currentURL);
            page = new PageObjects(driver); // Assumes PageObjects class is defined elsewhere
            System.out.println("PageObjects instance created for URL: " + currentURL);
            pageTitleFromDriver = driver.getTitle();
            if (pageTitleFromDriver == null) pageTitleFromDriver = "Title Unavailable";
            System.out.println("Navigation complete. Page Title: " + pageTitleFromDriver);
            //  modalWasDisplayed = false; // Reset flag for new scenario/page
        } catch (Exception e) {
            System.err.println("ERROR navigating to URL '" + currentURL + "': " + e.getMessage());
            e.printStackTrace();
            Assert.fail("Failed to navigate to URL: " + currentURL); // Fail test if navigation fails
        }
    }

    // --- @When Step (Handles Modals Only) ---
    @When("I handle any initial modal dialogues")
    public void i_handle_any_initial_modal_dialogues() {
        System.out.println("--- Checking for Modals ---");
        modalWasDisplayed = false;
        if (page == null) { // Defend against null page object
            System.err.println("Page object is null, cannot check modals.");
            return;
        }
        try {
            if (page.isZipCodeModalDisplayed(Duration.ofSeconds(2))) { // Shorter wait for check
                System.out.println("Zip code modal (type 1) is present. Handling...");
                page.enterZipAndSubmitInModal("12345"); // Use appropriate Zip
                System.out.println("Zip code modal (type 1) handled.");
                modalWasDisplayed = true;
            } else { System.out.println("Zip code modal (type 1) was not present."); }

            if (page.isc28LightboxDisplayed(Duration.ofSeconds(2))) { // Shorter wait for check
                System.out.println("c28 lightbox is present. Handling...");
                page.enterZipAndSubmitInC28("94070"); // Use appropriate Zip
                System.out.println("c28 lightbox handled.");
                modalWasDisplayed = true;
            } else { System.out.println("c28 lightbox was not present."); }

            System.out.println("Modal handling check complete. Modal displayed flag: " + modalWasDisplayed);
        } catch (Exception e) {
            System.err.println("An error occurred during modal handling check: " + e.getMessage());
            // Optionally fail test
        }
    }

    // --- @Then Step: Control Flow for Processing ---
    @Then("I extract superscript information and take screenshot")
    public void i_extract_superscript_information_and_take_screenshot() throws IOException {
        // *** ADDED: Log entry into the step ***
        System.out.println("--- ENTERING @Then 'I extract superscript...' step ---");
        String actualPageTitleText = null;
        boolean canProceed = true; // Flag to control execution

        // --- Safely Get Page Title ---
        if (page == null) {
            System.err.println("CRITICAL ERROR: PageObjects instance is null in @Then step. Cannot get title.");
            actualPageTitleText = "[ERROR: PageObject Null]";
            canProceed = false; // Cannot proceed without page object in this design
            Assert.fail("PageObjects instance was null in @Then step."); // Fail fast
        } else {
            try {
                System.out.println("Attempting to get actual page title via PageObjects...");
                actualPageTitleText = page.getActualPageTitleText();
                if (actualPageTitleText == null) {
                    System.out.println("PageObjects returned null title. Falling back to driver.getTitle().");
                    actualPageTitleText = driver.getTitle();
                }
                if (actualPageTitleText == null) {
                    actualPageTitleText = "[ERROR: Title Unavailable]";
                    System.err.println("ERROR: Could not get page title from PageObjects or driver.getTitle().");
                    canProceed = false; // Cannot proceed without a title for comparison
                }
            } catch (Exception e) {
                System.err.println("EXCEPTION occurred getting page title: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace(); // Print stack trace for this error
                actualPageTitleText = "[ERROR: Exception Getting Title]";
                canProceed = false; // Cannot proceed if title fetching fails critically
                Assert.fail("Exception occurred while getting page title in @Then step."); // Fail fast
            }
        }

        System.out.println("Actual Page Title for conditional check: '" + actualPageTitleText + "'");
        System.out.println("Modal was displayed flag: " + modalWasDisplayed);

        // --- Only proceed if title could be obtained ---
        if (canProceed) {
            System.out.println("Proceeding with conditional processing logic...");
            try {
                String trimmedActualTitle = actualPageTitleText.trim(); // Trim for comparison

                // *** Log the comparison results BEFORE the if/else block ***
                boolean isCheckingLanding = CHECKING_ACCOUNTS_LANDING.equalsIgnoreCase(trimmedActualTitle);
                boolean isSavingsLanding = SAVINGS_ACCOUNTS_LANDING.equalsIgnoreCase(trimmedActualTitle);
                boolean isSimplePage = PAGE_TITLE_ED.equalsIgnoreCase(trimmedActualTitle) || PAGE_TITLE_CABA.equalsIgnoreCase(trimmedActualTitle) || PAGE_TITLE_CAB.equalsIgnoreCase(trimmedActualTitle);
                System.out.println("  isCheckingLanding? " + isCheckingLanding);
                System.out.println ( "  isSavingsLanding? " + isSavingsLanding );
                System.out.println ( "  isSimplePage? " + isSimplePage );
                System.out.println ( "  modalWasDisplayed? " + modalWasDisplayed );
                // *** End Log ***

                // Determine processing path
                if ( modalWasDisplayed ) {
                    System.out.println("  MATCH: modalWasDisplayed = true");
                    System.out.println("  Processing Path: After Modal Displayed");
                    processSuperscriptsForFullPage("AfterModal_" + getSafeFilenameBase(), true, this.timeout);
                } else if ( isCheckingLanding || isSavingsLanding ) {
                    System.out.println("  MATCH: isCheckingLanding or isSavingsLanding = true");
                    System.out.println("  Processing Path: Tabs Page (" + actualPageTitleText + ")");
                    processSuperscriptsByTab(actualPageTitleText);
                } else if ( isSimplePage ) {
                    System.out.println("  MATCH: isSimplePage = true");
                    System.out.println("  Processing Path: Simple Page (" + actualPageTitleText + ")");
                    processSuperscriptsForFullPage("SimplePage_" + getSafeFilenameBase(), true, timeout);
                } else {
                    System.out.println("  MATCH: No specific condition met.");
                    System.out.println("  Processing Path: Default (Treat as Simple Page)");
                    processSuperscriptsForFullPage("Default_" + getSafeFilenameBase(), true, timeout);
                }
            } catch (IOException ioEx) { // Corrected exception type
                System.err.println("IOException during processing/screenshotting: " + ioEx.getMessage());
                ioEx.printStackTrace();
                throw ioEx; // Re-throw critical IOExceptions
            } catch (Exception ex) {
                System.err.println("Unexpected exception during conditional processing logic: " + ex.getMessage());
                ex.printStackTrace();
                Assert.fail("Unexpected exception during conditional processing: " + ex.getMessage());
            }
        } else {
            System.err.println("Skipping conditional processing because canProceed flag is false.");
        }

        System.out.println("--- EXITING @Then 'I extract superscript...' step ---");
    }

    // --- Helper: Process Tab by Tab ---
    /**
     * Helper method to process superscripts for an entire page.
     * Used for simple pages, default cases, or after a modal has been handled.
     *
     * @param fileIdentifierBase Base string for filenames (e.g., "SimplePage_MyPageTitle").
     * @param expandHeaders      Boolean flag indicating whether to expand collapsible headers first.
     * @param timeout
     * @throws IOException If screenshot saving fails.
     */
    private void processSuperscriptsForFullPage(String fileIdentifierBase, boolean expandHeaders, long timeout) throws IOException {
        System.out.println("Processing Full Page: " + fileIdentifierBase);
        // Construct filename with path separator
        String screenshotFilename = "screenshots" + File.separator + fileIdentifierBase + "_" + timestamp + ".png";

        // 1. Expand Headers if requested and possible
        if (expandHeaders) {
            if (page != null) {
                System.out.println("Expanding all collapsible headers for " + fileIdentifierBase + " if present...");
                try {
                    page.expandAllCollapsibleHeaders(); // Use PageObjects method
                    // Wait after expansion for content to potentially load/settle
                    Thread.sleep(1500); // Increased wait after expansion
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    System.err.println("Thread interrupted while waiting after header expansion.");
                } catch (Exception e) {
                    System.err.println("Error occurred during header expansion for " + fileIdentifierBase + ": " + e.getMessage());
                    // Continue processing even if expansion fails? Or Assert.fail?
                }
            } else {
                System.err.println("Cannot expand headers for " + fileIdentifierBase + ", PageObject is null.");
            }
        }

        // 2. Take Full Page Screenshot (using AShot wrapper)
        System.out.println("Taking full page screenshot for: " + fileIdentifierBase);
        BufferedImage combinedImage = takeFullPageScreenshotWithAShot(driver); // Recommended: Use AShot
        if (combinedImage == null) {
            System.err.println("Screenshot failed for " + fileIdentifierBase + ". Aborting processing for this page.");
            // Optionally create an empty summary entry to indicate failure?
            VerificationSummary pageSummary = tabSummaries.computeIfAbsent(fileIdentifierBase, k -> new VerificationSummary());
            pageSummary.positionCheckErrors++; // Mark an error
            return; // Exit if screenshot failed
        }

        // 3. Find All Superscripts on the Page (with wait)
        System.out.println("Finding all superscripts on page (" + fileIdentifierBase + ") using //sup[@class='c20ref']");
        By superscriptLocator = By.xpath("//sup[@class='c20ref']"); // Global locator
        List<WebElement> superscripts = findElementsWithWait(driver, superscriptLocator, timeout); // Use wait helper, increased timeout
        if (superscripts == null) superscripts = new ArrayList<>();
        System.out.println("Found " + superscripts.size() + " elements globally with locator: " + superscriptLocator);

        // 4. Get/Create Summary Object // Corrected comment
        VerificationSummary pageSummary = tabSummaries.computeIfAbsent(fileIdentifierBase, k -> new VerificationSummary ( ) );

        // 5. Call the Marking/Collection Method
        // Pass null for activePanel, pass the globally found list
        if (!superscripts.isEmpty()) {
            System.out.println("Calling markAndCollectNumericSuperscripts for full page: " + fileIdentifierBase);
            markAndCollectNumericSuperscripts(driver, combinedImage, null, superscripts, fileIdentifierBase, pageSummary);
        } else {
            System.out.println("No superscripts found on the page to mark for: " + fileIdentifierBase);
            pageSummary.totalSuperscriptsFoundInPanel = 0; // Explicitly set count to 0
        }

        // 6. Save the Marked Screenshot
        System.out.println("Saving marked screenshot for " + fileIdentifierBase);
        saveMarkedScreenshot(combinedImage, screenshotFilename); // Save image AFTER marking is done
    }

    // --- Ensure other required methods like markAndCollectNumericSuperscripts,
    // --- findElementsWithWait, takeFullPageScreenshotWithAShot, saveMarkedScreenshot, etc.,
    // --- and the inner classes SuperscriptInfo, VerificationSummary are present in this file. ---

    private static List<WebElement> findElementsWithWait(WebDriver driver, By locator, double timeout) { // Corrected method signature
        if (driver == null || locator == null) {
            System.err.println("Error: Invalid arguments passed to findElementsWithWait (driver or locator is null).");
            return null; // Return null to indicate a critical error
        }

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds((long) timeout));
            // Wait for at least one element to be present
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            // Now find all elements (even if only one was present)
            return driver.findElements(locator);
        } catch (TimeoutException e) {
            System.out.println("Timeout (" + timeout + "s) waiting for presence of elements: " + locator + ". Returning empty list.");
            return new ArrayList<>(); // Return empty list on timeout
        } catch (NoSuchElementException e) {
            System.out.println("No such element found: " + locator + ". Returning empty list.");
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error finding elements with wait (" + locator + "): " + e.getMessage());
            return new ArrayList<>(); // Return empty list on other errors
        }
    }

    private void processSuperscriptsByTab(String pageIdentifier) throws IOException {
        System.out.println("Processing Page Tab by Tab: " + pageIdentifier);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // Longer wait for tabs
        By tabContainerLocator = By.xpath("//div[@role='tablist' and contains(@class, 'table-tab-list')]");
        By tabLocator = By.xpath(".//button[@role='tab']");
        String activePanelIndicatorAttribute = "aria-selected";
        String activePanelIndicatorValue = "true"; // Corrected value
        By superscriptInPanelLocator = By.xpath(".//sup[@class='c20ref']");

        WebElement tabContainer;
        int tabCount;
        try {
            tabContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(tabContainerLocator));
            tabCount = tabContainer.findElements(tabLocator).size();
            System.out.println("Found " + tabCount + " tabs to process.");
            if (tabCount == 0) {
                System.out.println("No tabs found to process. Exiting.");
                return;
            }
        } catch (Exception e) { /* Log error */ return; }

        for (int i = 0; i < tabCount; i++) {
            WebElement currentTab = null;
            String tabIdentifier = "[Unknown Tab " + i + "]";
            VerificationSummary currentTabSummary = null;

            try {
                // Re-find tab list and element
                List<WebElement> currentTabsList = driver.findElements(tabLocator);
                if (i >= currentTabsList.size()) { System.err.println("Tab index out of bounds: " + i); continue; }
                currentTab = currentTabsList.get(i);
                tabIdentifier = getTabIdentifier(currentTab, i) + " (" + pageIdentifier + ")"; // More specific ID
                currentTabSummary = tabSummaries.computeIfAbsent(tabIdentifier, k -> new VerificationSummary());
                System.out.println("\nProcessing Tab (" + (i + 1) + "/" + tabCount + "): " + tabIdentifier);

                // Click Tab if needed
                boolean needsClick = !activePanelIndicatorValue.equalsIgnoreCase(currentTab.getAttribute(activePanelIndicatorAttribute));
                if (needsClick) {
                    System.out.println("Clicking tab...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", currentTab);
                    Thread.sleep(300); // Pause after scroll
                    try {
                        wait.until(ExpectedConditions.elementToBeClickable(currentTab)).click();
                    } catch (ElementClickInterceptedException e) {
                        System.out.println("ElementClickInterceptedException, trying JS click...");
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", currentTab);
                    }
                    wait.until(ExpectedConditions.attributeToBe(currentTab, activePanelIndicatorAttribute, activePanelIndicatorValue)); // Wait for attribute change
                    if (!activePanelIndicatorValue.equalsIgnoreCase(currentTab.getAttribute(activePanelIndicatorAttribute))) {
                        System.err.println("Tab click failed, attribute not updated.");
                    }
                    System.out.println("Tab click successful, waiting for panel content...");
                    Thread.sleep(1200); // Longer wait after click for content
                } else {
                    System.out.println("Tab already selected."); Thread.sleep(500); // Longer pause even if selected
                }

                // Find Active Panel
                WebElement activePanel = findActiveTabPanel(driver, wait, currentTab); // Corrected method call
                if (activePanel == null) {
                    System.err.println("Could not find active panel for tab: " + tabIdentifier);
                    if (currentTabSummary != null) currentTabSummary.positionCheckErrors++; // Increment error count
                    continue;
                }

                // Take screenshot AFTER panel is active
                System.out.println("Taking screenshot for tab: " + tabIdentifier);
                BufferedImage tabScreenshot = takeFullPageScreenshotWithAShot(driver);
                if (tabScreenshot == null) {
                    System.err.println("Screenshot failed for tab: " + tabIdentifier);
                    continue; }

                // Process superscripts within this panel using the screenshot for this tab
                markAndCollectNumericSuperscripts(driver, tabScreenshot, activePanel, null,  tabIdentifier, currentTabSummary);

                // Save screenshot for this tab
                String safeTabId = tabIdentifier.replaceAll("[^a-zA-Z0-9.-]", "_").replaceAll("_on_.*$", ""); // Clean up identifier for filename
                String tabScreenshotFilename = "screenshots" + File.separator + getSafeFilenameBase() + "_TAB_" + safeTabId + "_" + timestamp + ".png";
                System.out.println("Saving marked screenshot for tab: " + tabIdentifier);
                saveMarkedScreenshot(tabScreenshot, tabScreenshotFilename);

            } catch (Exception e) {
                System.err.println("Error processing tab cycle for " + tabIdentifier + ": " + e.getMessage());
                if (currentTabSummary != null) currentTabSummary.positionCheckErrors++; // Increment error count
                e.printStackTrace();
            }
        } // end for loop
    }


    // --- markAndCollectNumericSuperscripts Method ---
    // Processes superscripts either from a list OR within a panel
    // Populates static collections and writes detail rows
    // --- Inside SuperscriptStepDefinitions.java (or wherever this method resides) ---

    /**
     * Processes a list of potential superscripts (either found globally or within a specific panel),
     * filters for numeric ones, performs validation checks, draws markers on the image,
     * updates the summary object, adds details to the static list, and writes to the detail Excel sheet.
     *
     * @param driver           The WebDriver instance.
     * @param combinedImage    The BufferedImage to draw marks on (MUST NOT be null).
     * @param activePanel      The WebElement of the active tab panel (null if processing a full page list).
     * @param superscriptsList A pre-fetched list of superscripts (null if processing within activePanel).
     * @param identifier       A string identifying the current context (e.g., Page Title or Tab ID).
     * @param summary          The VerificationSummary object for this identifier to update counts.
     */
    private void markAndCollectNumericSuperscripts(
            WebDriver driver, BufferedImage combinedImage,
            WebElement activePanel, List<WebElement> superscriptsList,
            String identifier, VerificationSummary summary)
    {
        // --- Initial Null Checks ---
        if (combinedImage == null) {
            System.err.println("ERROR: Cannot process/mark superscripts for [" + identifier + "] because the input image (combinedImage) is null.");
            // Estimate potential errors based on initial find attempt if possible, otherwise mark as general error
            summary.positionCheckErrors += 1; // Indicate at least one error occurred
            return;
        }
        if (summary == null || workbook == null || detailSheet == null) {
            System.err.println("Cannot process superscripts: Null object detected (Summary, Workbook, or Detail Sheet) for identifier: " + identifier);
            return;
        }
        // --- End Null Checks ---

        Graphics2D graphics = null; // Initialize graphics to null
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> superscriptsToProcess = new ArrayList<>();
        By superscriptLocatorInPanel = By.xpath(".//sup[@class='c20ref']"); // Locator relative to panel

        try {
            // --- Determine list of superscripts to process ---
            if (activePanel != null) {
                // Processing within a specific panel
                System.out.println("Waiting for VISIBLE superscripts within panel for: " + identifier);
                try {
                    WebDriverWait panelWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    // Wait for at least one element to be visible within the panel
                    panelWait.until(d -> { // Use lambda for concise condition
                        try {
                            return activePanel.findElements(superscriptLocatorInPanel).stream().anyMatch(WebElement::isDisplayed); // Corrected method reference
                        } catch (StaleElementReferenceException e) {
                            return false;
                        } // Handle panel getting stale during check
                    });


                    Thread.sleep(250); // Pause after visibility confirmed
                    superscriptsToProcess = activePanel.findElements(superscriptLocatorInPanel); // Find all now
                } catch (TimeoutException te) {
                    System.out.println("No visible superscripts found in panel for: " + identifier);
                } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                catch (StaleElementReferenceException se) { System.err.println("Panel became stale while finding superscripts for: " + identifier); }
                catch (Exception e) { System.err.println("Error finding superscripts in panel for "+identifier+": "+e.getMessage()); }

            } else if (superscriptsList != null) {
                // Processing a pre-fetched list (full page)
                System.out.println("Processing provided list of " + superscriptsList.size() + " superscripts for: " + identifier);
                superscriptsToProcess = superscriptsList;
            } else {
                System.err.println("Error: Neither activePanel nor superscriptsList provided for identifier: " + identifier);
                return; // Cannot proceed
            }

            // --- Process Found Superscripts ---
            if (superscriptsToProcess.isEmpty()) {
                System.out.println("No superscripts found to process/mark for: " + identifier);
                return; // Exit if list is empty
            }

            // Create graphics context now that we know we have elements
            System.out.println("Creating Graphics context for marking " + identifier + "...");
            graphics = combinedImage.createGraphics();
            graphics.setStroke(new BasicStroke(2)); // Set default stroke

            summary.totalSuperscriptsFoundInPanel += superscriptsToProcess.size(); // Increment total found count
            System.out.println("Processing " + superscriptsToProcess.size() + " potential superscripts for: " + identifier);

            Pattern numericPattern = Pattern.compile("^\\d+$");
            int processedNumericCountThisCall = 0; // Track numeric count for this call

            for (WebElement sup : superscriptsToProcess) {
                String supText = "[Error]"; boolean isEmpty = false; boolean isNumeric = false;
                String elementIdForLog = getElementIdentifierForLog(sup); // Get ID/Text for logging

                try {
                    // It's often safer to get text immediately in case of staleness
                    supText = sup.getText().trim();
                    isEmpty = supText.isEmpty();
                    if (!isEmpty) {
                        isNumeric = numericPattern.matcher(supText).matches();
                    }

                    // --- Handle Empty Tags ---
                    if (isEmpty) {
                        summary.emptySuperscriptLogs.add(identifier + ": Empty near '" + getPrecedingText(sup, driver) + "'");
                        continue; // Skip to the next superscript
                    } else summary.emptySuperscriptLogs.add(identifier + ": NOT Empty near '" + getPrecedingText(sup, driver) + "'");

                    // --- Process Only Numeric Superscripts ---
                    if (isNumeric) {
                        processedNumericCountThisCall++;
                        summary.numericSuperscriptsProcessed++; // Update summary object
                        SuperscriptInfo info = new SuperscriptInfo(); // Assumes SuperscriptInfo class exists
                        info.tabIdentifier = identifier; // Use the passed identifier (page or tab)
                        info.superscriptText = supText;
                        info.precedingText = getPrecedingText(sup, driver); // Use JS helper

                        // --- Position Check ---
                        SuperscriptInfo.PositionStatus status = checkSuperscriptPosition(sup, driver); // Use getComputedStyle version
                        info.positionStatus = status;
                        info.verificationMessage = getPositionVerificationMessage(status, supText);
                        // Update summary counts based on position status
                        if (status == SuperscriptInfo.PositionStatus.ABOVE_BASELINE) {
                            summary.positionCheckPassed++;
                        } else if (status == SuperscriptInfo.PositionStatus.SAME_LINE) {
                            summary.positionCheckFailed++;
                        } else summary.positionCheckErrors++;

                        // --- Hyperlink Check ---
                        boolean isHyperlink = false;
                        String linkHref = "NO HYPERLINK"; // Default
                        try {
                            // Check for an ancestor 'a' tag
                            WebElement parentLink = sup.findElement(By.xpath(".//ancestor::a[1]"));
                            String href = parentLink.getAttribute("href");
                            // Check if href is meaningful (not null, empty, or javascript:)
                            isHyperlink = (href != null && !href.trim().isEmpty() && !href.toLowerCase().startsWith("javascript:"));
                            linkHref = href != null ? href.trim() : "N/A (Empty href)";
                            if (isHyperlink) summary.hyperlinkCount++;
                            else summary.noHyperlinkCount++;
                        } catch (NoSuchElementException eLink) {
                            // No ancestor link found
                            summary.noHyperlinkCount++;
                        } catch (StaleElementReferenceException staleLinkEx) {
                            // Element became stale while checking link
                            System.err.println("Stale element reference checking link for " + elementIdForLog);
                            summary.noHyperlinkCount++; // Count as no link if error occurs
                            info.linkHref = "[Error: Stale Checking Link]";
                        }
                        info.isHyperlink = isHyperlink;
                        // Only assign linkHref if it wasn't the error default
                        if (!"[Error: Stale Checking Link]".equals(linkHref)) {
                            info.linkHref = linkHref;
                        }


                        // *** Add detail object to the static list for final Excel export ***
                        allSuperscriptDetails.add(info);

                        // --- Draw Oval on Image ---
                        Map<String, Object> rect = null;
                        try { // Get coordinates via JS
                            rect = (Map<String, Object>) js.executeScript( // Cast to Map
                                    "var elem = arguments[0]; if (!elem) return null; var rect = elem.getBoundingClientRect(); return { top: rect.top + window.pageYOffset, left: rect.left + window.pageXOffset, width: rect.width, height: rect.height };", sup);
                        } catch (Exception coordJsEx) {
                            System.err.println("Error getting coordinates via JS for " + elementIdForLog + ": " + coordJsEx.getMessage());
                            summary.positionCheckErrors++; // Count coordinate error
                        }

                        if (rect != null) {
                            int x=0, y=0, width=0, height=0, drawX=0, drawY=0, paddedWidth=10, paddedHeight=10; // Initialize
                            try { // Extract coordinates
                                x = ((Number) rect.get("left")).intValue(); // Corrected casting
                                y = ((Number) rect.get("top")).intValue(); // Corrected casting
                                width = ((Number) rect.get("width")).intValue(); // Corrected casting
                                height = ((Number) rect.get("height")).intValue(); // Corrected casting
                                // Ensure minimum size for drawing
                                paddedWidth = Math.max(width + 6, 10);
                                paddedHeight = Math.max(height + 6, 10);
                                // Center the oval slightly better
                                drawX = x + (width / 2) - (paddedWidth / 2);
                                drawY = y + (height / 2) - (paddedHeight / 2);

                                // Set color based on hyperlink status
                                graphics.setColor(isHyperlink ? Color.GREEN : Color.RED);
                                graphics.drawOval(drawX, drawY, paddedWidth, paddedHeight); // Draw the oval

                            } catch (Exception coordEx) {
                                System.err.println("Error calculating/drawing coords for sup '" + supText + "': " + coordEx.getMessage());
                                summary.positionCheckErrors++; // Count drawing error
                            }
                        } else {
                            System.err.println("Could not get coordinates to draw oval for " + elementIdForLog);
                            summary.positionCheckErrors++; // Count coordinate error
                        }

                        // --- Write Detail Row to Excel Sheet ---
                        Row dataRow = detailSheet.createRow(detailRowCount++); // Use static sheet/counter
                        int cellNum = 0;
                        dataRow.createCell(cellNum).setCellValue(info.tabIdentifier != null ? info.tabIdentifier : "N/A"); cellNum++; // Corrected cell creation
                        dataRow.createCell(cellNum).setCellValue(info.superscriptText != null ? info.superscriptText : "N/A"); cellNum++; // Corrected cell creation
                        dataRow.createCell(cellNum).setCellValue(info.precedingText != null ? info.precedingText : "[Error]"); cellNum++; // Corrected cell creation
                        dataRow.createCell(cellNum).setCellValue(info.isHyperlink ? "YES" : "NO"); cellNum++; // Corrected cell creation
                        dataRow.createCell(cellNum).setCellValue(info.linkHref != null ? info.linkHref : "N/A"); cellNum++; // Corrected cell creation
                        dataRow.createCell(cellNum).setCellValue(info.positionStatus != null ? info.positionStatus.name() : "ERROR"); cellNum++; // Corrected cell creation
                        dataRow.createCell(cellNum).setCellValue(info.verificationMessage != null ? info.verificationMessage : "Error getting message"); cellNum++; // Corrected cell creation

                    } else {
                        // Log non-numeric ones if needed
                        summary.nonNumericSuperscriptLogs.add(identifier + ": " + supText);
                    }
                } catch (StaleElementReferenceException se) {
                    System.err.println("Stale element reference processing superscript " + elementIdForLog + " ('" + supText + "') for " + identifier);
                    summary.positionCheckErrors++; // Count as error
                } catch (Exception supEx) {
                    System.err.println("Error processing superscript " + elementIdForLog + " ('" + supText + "') for " + identifier + ": " + supEx.getMessage());
                    summary.positionCheckErrors++; // Count as error
                    supEx.printStackTrace(); // See details for unexpected errors
                }
            } // end for loop over superscriptsToProcess

            System.out.println("Finished processing. Numeric superscripts processed in this call for " + identifier + ": " + processedNumericCountThisCall);

        } catch (Exception generalEx) {
            System.err.println("General error during markAndCollectNumericSuperscripts for " + identifier + ": " + generalEx.getMessage());
            summary.positionCheckErrors++; // Increment general error count if possible
            generalEx.printStackTrace();
        } finally {
            // Dispose graphics context ONLY if it was successfully created
            if (graphics != null) {
                System.out.println("Disposing graphics context for: " + identifier);
                graphics.dispose();
            }
        }
    } // End of markAndCollectNumericSuperscripts

    private String getElementIdentifierForLog(WebElement sup) {
        return sup.getText();
    }

    // --- @Then write results (No-Op) ---
    @Then("I write results to an Excel file")
    public void i_write_results_to_an_excel_file() {
        writeExtractedDataToExcel(getSafeFilenameBase()); // Corrected method call
    }
    // --- @Then close browser (No file writing) ---
    @Then("I close the browser for Superscript extraction")
    public void i_close_the_browser_for_superscript_extraction() { /* Quit Driver */
        if (driver != null) {
        driver.quit();
    }}

    // --- Helper Methods ---

    // USE ASHOT VERSION
    private BufferedImage takeFullPageScreenshotWithAShot(WebDriver driver) {
        System.out.println("Taking full page screenshot using AShot...");
        Screenshot fpScreenshot = null;
        try {
            ShootingStrategy shootingStrategy = ShootingStrategies.viewportPasting(1500); // Adjust timeout
            fpScreenshot = new AShot().shootingStrategy(shootingStrategy).takeScreenshot(driver);
            System.out.println("AShot screenshot captured successfully.");
            return fpScreenshot.getImage();
        } catch (Exception e) {
            System.err.println("AShot screenshot failed: " + e.getMessage() + ". Falling back to viewport."); // Corrected error message
            e.printStackTrace();
            try {
                File sf = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                return ImageIO.read(sf);
            } catch (Exception fallbackEx) {
                System.err.println("Fallback screenshot also failed: " + fallbackEx.getMessage());
                return null;
            }
        }
    }
    private BufferedImage takeFullPageScreenshot(WebDriver driver) throws IOException {
        return takeFullPageScreenshotWithAShot(driver); // Default to AShot
    } // Corrected method signature


    private static void saveMarkedScreenshot ( BufferedImage combinedImage , String filename ) throws IOException {
        if (combinedImage == null) { /* Log error */ return; }
        File outputFile = new File(filename);
        try {
            outputFile.getParentFile().mkdirs();
            boolean success = ImageIO.write(combinedImage, "png", outputFile);
            if (success) { System.out.println("----> Marked screenshot saved to: " + outputFile.getAbsolutePath()); }
            else { System.err.println("ERROR: ImageIO.write failed for: " + outputFile.getAbsolutePath());}
        } catch (IOException e) { /* Log/throw */ throw e; }
        catch (Exception e) { /* Log/print */ }
    }

    // Get computed style version
    private static SuperscriptInfo.PositionStatus checkSuperscriptPosition(WebElement supElement, WebDriver driver) {
        if (supElement == null) return SuperscriptInfo.PositionStatus.CHECK_ERROR;
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver; // Corrected variable name
            Object result = js.executeScript(
                    "var elem = arguments[0]; if (!elem) return 'CHECK_ERROR: Element is null';" +
                            "try { var style = window.getComputedStyle(elem); return style.getPropertyValue('vertical-align'); }" +
                            "catch (e) { return 'CHECK_ERROR: JS Error - ' + e.message; }", supElement);

            if (result instanceof String) {
                String verticalAlign = (String) result;
                if ("super".equalsIgnoreCase(verticalAlign)) {
                    return SuperscriptInfo.PositionStatus.ABOVE_BASELINE;
                } else if ("baseline".equalsIgnoreCase(verticalAlign) || "middle".equalsIgnoreCase(verticalAlign) || "sub".equalsIgnoreCase(verticalAlign) || "text-bottom".equalsIgnoreCase(verticalAlign) || "text-top".equalsIgnoreCase(verticalAlign) || "top".equalsIgnoreCase(verticalAlign) || "bottom".equalsIgnoreCase(verticalAlign)) {
                    return SuperscriptInfo.PositionStatus.SAME_LINE;
                } else {
                    return SuperscriptInfo.PositionStatus.CHECK_ERROR; // Unrecognized value
                }
            } else {
                return SuperscriptInfo.PositionStatus.CHECK_ERROR; // Unexpected type
            }
        } catch (Exception e) {
            return SuperscriptInfo.PositionStatus.CHECK_ERROR; // Error during check
        }
    }

    // JS version
    private String getPrecedingText(WebElement element, WebDriver driver) {
        if (element == null) {
            System.err.println("getPrecedingText: element is null.");
            return "[Error: Element Null]";
        }
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String precedingText = (String) js.executeScript(
                    "var elem = arguments[0];" +
                            "var prev = elem.previousSibling;" +
                            "while (prev && prev.nodeType !== Node.TEXT_NODE) {" +
                            "  prev = prev.previousSibling;" +
                            "}" +
                            "return prev ? prev.textContent.trim() : '';", element);
            return precedingText;
        } catch (Exception e) {
            System.err.println("getPrecedingText: Error getting preceding text: " + e.getMessage());
            return "[Error: JS Exception]";
        }
    }

    // Get ID, label, or text version
    private String getTabIdentifier(WebElement tabElement, int index) {
        try {
            String id = tabElement.getAttribute("id");
            String label = tabElement.getAttribute("aria-label");
            String text = tabElement.getText().trim();
            return (id != null && !id.isEmpty() ? id : (label != null && !label.isEmpty() ? label : (text.isEmpty() ? "Tab " + index : text)));
        } catch (Exception e) {
            System.err.println("Error getting tab identifier: " + e.getMessage());
            return "[Error: Tab " + index + "]";
        }
    }

    // aria-controls version
    private WebElement findActiveTabPanel(WebDriver driver, WebDriverWait wait, WebElement clickedTab) {
        try {
            String panelId = clickedTab.getAttribute("aria-controls");
            if (panelId == null || panelId.isEmpty()) {
                System.err.println("findActiveTabPanel: aria-controls attribute is empty or null.");
                return null;
            }
            By panelLocator = By.id(panelId);
            return wait.until(ExpectedConditions.visibilityOfElementLocated(panelLocator));
        } catch (Exception e) {
            System.err.println("findActiveTabPanel: Error finding active panel: " + e.getMessage());
            return null;
        }
    }

    // Creates message based on enum
    private String getPositionVerificationMessage(SuperscriptInfo.PositionStatus status, String supText) {
        // ... (Implementation from previous correct answer) ... // Corrected comment
        return "Verification message pending implementation"; // Placeholder
    }

    // Creates safe filename part
    private String getSafeFilenameBase() {
        String safeTitle = pageTitleFromDriver.replaceAll("[^a-zA-Z0-9.-]", "_");
        return safeTitle.length() > 40 ? safeTitle.substring(0, 40) : safeTitle;
    }

    private void writeExtractedDataToExcel(String filenameBase) {
        System.out.println("--- ENTERING writeExtractedDataToExcel ---");
        if (workbook == null) {
            System.err.println("ERROR: Workbook is null. Cannot write to Excel.");
            return;
        }
        if (detailSheet == null || summarySheet == null) {
            System.err.println("ERROR: Detail or Summary sheet is null. Cannot write to Excel.");
            return;
        }

        try {
            // --- Write Summary Data ---
            System.out.println("Writing summary data to Excel...");
            for (Map.Entry<String, VerificationSummary> entry : tabSummaries.entrySet()) {
                String tabId = entry.getKey();
                VerificationSummary summary = entry.getValue();

                Row summaryDataRow = summarySheet.createRow(summaryRowCount++);
                int cellNum = 0;
                summaryDataRow.createCell(cellNum).setCellValue(tabId); cellNum++;
                summaryDataRow.createCell(cellNum).setCellValue(summary.totalSuperscriptsFoundInPanel); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.numericSuperscriptsProcessed); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.positionCheckPassed); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.positionCheckFailed); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.positionCheckErrors); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.hyperlinkCount); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.noHyperlinkCount); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.emptySuperscriptLogs.size()); cellNum++; // Corrected cell creation
                summaryDataRow.createCell(cellNum).setCellValue(summary.nonNumericSuperscriptLogs.size()); cellNum++; // Corrected cell creation
            }

            // --- Write Detail Data (already done in markAndCollect) ---
            System.out.println("Detail data already written to Excel.");

            // --- Save the Workbook ---
            System.out.println("Saving Excel workbook...");
            String excelFilename = "results" + File.separator + filenameBase + "_" + timestamp + ".xlsx";
            File excelFile = new File(excelFilename);
            excelFile.getParentFile().mkdirs(); // Ensure directory exists
            java.io.FileOutputStream fileOut = new java.io.FileOutputStream(excelFile);
            workbook.write(fileOut);
            fileOut.close();
            System.out.println("Excel file saved to: " + excelFile.getAbsolutePath());

            // --- Log Empty/Non-Numeric ---
            System.out.println("\n--- Empty Superscript Logs ---");
            tabSummaries.values().forEach(s -> s.emptySuperscriptLogs.forEach(System.out::println));
            System.out.println("\n--- Non-Numeric Superscript Logs ---");
            tabSummaries.values().forEach(s -> s.nonNumericSuperscriptLogs.forEach(System.out::println));

        } catch (IOException ioEx) {
            System.err.println("IOException writing to Excel: " + ioEx.getMessage());
            ioEx.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Unexpected exception writing to Excel: " + ex.getMessage());
            ex.printStackTrace();
        }
        System.out.println("--- EXITING writeExtractedDataToExcel ---");
    }
} // End of class // Corrected class closing brace





