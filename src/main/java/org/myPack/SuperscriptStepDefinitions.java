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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.Duration.*;

public class SuperscriptStepDefinitions {


    // --- Inner Class Definitions ---
    // Placed here for completeness, consider separate files for larger projects
    public static class SuperscriptInfo {
        String tabIdentifier;
        String superscriptText;
        String precedingText;
        boolean isHyperlink;
        String linkHref;
        PositionStatus positionStatus;
        String verificationMessage;

        public enum PositionStatus {ABOVE_BASELINE, SAME_LINE, CHECK_ERROR, NOT_APPLICABLE}

        @Override
        public String toString () {
            return "SuperscriptInfo{tab='" + tabIdentifier + "', text='" + superscriptText + "', preceding='" + precedingText + "', isLink=" + isHyperlink + ", href='" + linkHref + "', position=" + positionStatus + ", msg='" + verificationMessage + "'}";
        }
    }

    // Make this a top-level public class
    public static class VerificationSummary {
        int totalSuperscriptsFoundInPanel = 0;
        int numericSuperscriptsProcessed = 0;
        int positionCheckPassed = 0;
        int positionCheckFailed = 0;
        int positionCheckErrors = 0;
        int hyperlinkCount = 0;
        int noHyperlinkCount = 0;
        List<String> emptySuperscriptLogs = new ArrayList<> ( );
        List<String> nonNumericSuperscriptLogs = new ArrayList<> ( );
        int expectedNumericTotal = - 1;
        String finalResult = "SKIPPED";

        @Override
        public String toString () {
            return "Summary{totalFound=" + totalSuperscriptsFoundInPanel + ", numericProcessed=" + numericSuperscriptsProcessed + ", posPassed=" + positionCheckPassed + ", posFailed=" + positionCheckFailed + ", posErrors=" + positionCheckErrors + ", links=" + hyperlinkCount + ", noLinks=" + noHyperlinkCount + ", emptyTags=" + emptySuperscriptLogs.size ( ) + ", nonNumericTags=" + nonNumericSuperscriptLogs.size ( ) + ", expected=" + (expectedNumericTotal == - 1 ? "N/A" : expectedNumericTotal) + ", result='" + finalResult + "'}";
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
    public static List<SuperscriptInfo> allSuperscriptDetails = new ArrayList<> ( );
    public static Map<String, VerificationSummary> tabSummaries = new LinkedHashMap<> ( );

    // --- Constants for Titles (Update with EXACT titles from your application) ---
    private static final String PAGE_TITLE_ED = "Everyday Checking";
    private static final String PAGE_TITLE_CABA = "Choose a bank account"; // Verify this title if used
    private static final String PAGE_TITLE_CAB = "Clear Access Banking";
    private static final String CHECKING_ACCOUNTS_LANDING = "Checking Accounts: Open Online Today | Wells Fargo";
    private static final String SAVINGS_ACCOUNTS_LANDING = "Open a Savings Account Online | Wells Fargo";

    private int expectedTotalNumericSuperscriptsForScenario = - 1; // Default to invalid
    private int actualTotalNumericSuperscriptsForScenario = 0;    // Accumulates across tabs/pages for THIS scenario
    private String overallValidationResultForScenario = "SKIPPED"; // Default state

    // --- Constructor ---
    public SuperscriptStepDefinitions () {
        // Called by Cucumber for each scenario
        System.out.println ( "SuperscriptStepDefinitions instance created." );
    }

    // --- @Given Steps ---
    @Given("I have a Chrome browser for Superscript extraction")
    public void i_have_a_chrome_browser_for_superscript_extraction () {
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\DataValidationRates\\src\\main\\resources\\chromedriver.exe" );
        driver = new ChromeDriver ( );
        driver.manage ( ).timeouts ( ).implicitlyWait ( ofSeconds ( 10 ) ); // Adjust implicit wait if needed
        driver.manage ( ).window ( ).maximize ( );

        expectedSuperscriptCounts = new HashMap<> ( ); // Initialize map for expectations

        // --- Initialize Workbook and Sheets ONCE per test run ---
        if ( timestamp == null ) {
            System.out.println ( "--- Initializing Workbook and Sheets (First Run) ---" );
            timestamp = new SimpleDateFormat ( "yyyyMMdd_HHmmss" ).format ( new Date ( ) );
            workbook = new XSSFWorkbook ( );
            // Detail Sheet
            detailSheet = workbook.createSheet ( "SuperscriptDetail" );
            detailRowCount = 0;
            Row headerRow = detailSheet.createRow ( detailRowCount++ );
            String[] headers = {"Tab/Page Identifier" , "Superscript Text" , "Preceding Text" , "Is Hyperlink" , "Link Href" , "Position Status" , "Position Result Msg"};
            for (int i = 0; i < headers.length; i++) headerRow.createCell ( i ).setCellValue ( headers[i] );
            // Summary Sheet
            summarySheet = workbook.createSheet ( "ValidationSummary" );
            summaryRowCount = 0;
            Row summaryHeader = summarySheet.createRow ( summaryRowCount++ );
            String[] summaryHeaders = {"Page/Tab Identifier" , "Total Found" , "Numeric Processed" , "Position Passed" , "Position Failed" , "Position Errors" , "Hyperlinks Found" , "No Hyperlinks" , "Empty Tags" , "Non-Numeric" , "Expected Numeric Total" , "Overall Result"};
            for (int i = 0; i < summaryHeaders.length; i++)
                summaryHeader.createCell ( i ).setCellValue ( summaryHeaders[i] );
            System.out.println ( "Workbook initialized. Timestamp: " + timestamp );
        } else {
            System.out.println ( "--- Reusing existing Workbook ---" );
        }
    }

    @Given("I navigate to the web page {string}")
    public void i_navigate_to_the_web_page ( String url ) {
        currentURL = url;
        System.out.println ( "Attempting to navigate to: " + currentURL );
        try {
            driver.get ( currentURL );
            page = new PageObjects ( driver ); // Assumes PageObjects constructor initializes PageFactory
            System.out.println ( "PageObjects instance created for URL: " + currentURL );
            // Wait slightly for title stabilization
            WebDriverWait titleWait = new WebDriverWait ( driver , ofSeconds ( 10 ) );
            titleWait.until ( d -> d.getTitle ( ) != null && ! d.getTitle ( ).isEmpty ( ) );
            pageTitleFromDriver = driver.getTitle ( );
            if ( pageTitleFromDriver == null ) pageTitleFromDriver = "[Title Unavailable]";
            System.out.println ( "Navigation complete. Page Title: '" + pageTitleFromDriver + "'" );
            // Reset scenario-specific state
            modalWasDisplayed = false;
            expectedTotalNumericSuperscriptsForScenario = - 1;
            actualTotalNumericSuperscriptsForScenario = 0;
            overallValidationResultForScenario = "SKIPPED";
        } catch (Exception e) {
            System.err.println ( "ERROR navigating to URL '" + currentURL + "': " + e.getMessage ( ) );
            e.printStackTrace ( );
            Assert.fail ( "Failed to navigate to URL: " + currentURL );
        }
    }

    // --- @When Step (Handles Modals Only) ---
    @When("I handle any initial modal dialogues")
    public void i_handle_any_initial_modal_dialogues () {
        System.out.println ( "--- Checking for Modals ---" );
        modalWasDisplayed = false;
        if ( page == null ) { // Defend against null page object
            System.err.println ( "Page object is null, cannot check modals." );
            return;
        }
        try {
            if ( page.isZipCodeModalDisplayed ( ofSeconds ( 2 ) ) ) { // Shorter wait for check
                System.out.println ( "Zip code modal (type 1) is present. Handling..." );
                page.enterZipAndSubmitInModal ( "12345" ); // Use appropriate Zip
                System.out.println ( "Zip code modal (type 1) handled." );
                modalWasDisplayed = true;
            } else {
                System.out.println ( "Zip code modal (type 1) was not present." );
            }

            if ( page.isc28LightboxDisplayed ( ofSeconds ( 2 ) ) ) { // Shorter wait for check
                System.out.println ( "c28 lightbox is present. Handling..." );
                page.enterZipAndSubmitInC28 ( "94070" ); // Use appropriate Zip
                System.out.println ( "c28 lightbox handled." );
                modalWasDisplayed = true;
            } else {
                System.out.println ( "c28 lightbox was not present." );
            }

            System.out.println ( "Modal handling check complete. Modal displayed flag: " + modalWasDisplayed );
        } catch (Exception e) {
            System.err.println ( "An error occurred during modal handling check: " + e.getMessage ( ) );
            // Optionally fail test
        }
    }

    // --- @Then Step: Control Flow for Processing ---
    @Then("I extract superscript information and take screenshot for {string} expecting {string}")
    public void i_extract_superscript_information_and_take_screenshot ( String url , String expectedCountStr ) throws IOException {
        System.out.println ( "--- ENTERING @Then 'I extract...' for URL: " + url + " expecting: " + expectedCountStr + " ---" );
        String actualPageTitleText = driver.getTitle ( ); // Retrieve the page title

        if ( driver == null || page == null ) Assert.fail ( "Driver or PageObject was NULL in @Then step." );
        System.out.println ( "  @Then: driver and page instances are valid." );
        // ... (Store expected count, reset actual count)
        this.expectedTotalNumericSuperscriptsForScenario = - 1;
        this.overallValidationResultForScenario = "SKIPPED";
        try {
            expectedTotalNumericSuperscriptsForScenario = Integer.parseInt ( expectedCountStr );
        } catch (NumberFormatException e) {
            overallValidationResultForScenario = "ERROR (Bad Expectation: '" + expectedCountStr + "')";
        }
        System.out.println ( "  Stored Expected Total Numeric: " + expectedTotalNumericSuperscriptsForScenario );
        this.actualTotalNumericSuperscriptsForScenario = 0; // Reset accumulator


        // ... (Safely get actualPageTitleText) ...
        actualPageTitleText = null;
        boolean canProceed = true; // Initialize canProceed with a default value or logic
        try {
            actualPageTitleText = driver.getTitle ( );
        } catch (Exception e) {
        }
        if ( ! canProceed ) {
            Assert.fail ( "Cannot proceed." );
            return;
        } // Ensure canProceed logic is solid
        System.out.println ( "Actual Page Title for conditional check: '" + actualPageTitleText + "'" );
        System.out.println ( "Modal was displayed flag: " + modalWasDisplayed );

        String pageIdentifierForSummary = getSafeFilenameBase ( ); // Base identifier

        try {
            String trimmedActualTitle = actualPageTitleText.trim ( );
            boolean isCheckingLanding = CHECKING_ACCOUNTS_LANDING.equalsIgnoreCase ( trimmedActualTitle );
            boolean isSavingsLanding = SAVINGS_ACCOUNTS_LANDING.equalsIgnoreCase ( trimmedActualTitle );
            boolean isSimplePage = PAGE_TITLE_ED.equalsIgnoreCase ( trimmedActualTitle ) || PAGE_TITLE_CABA.equalsIgnoreCase ( trimmedActualTitle ) || PAGE_TITLE_CAB.equalsIgnoreCase ( trimmedActualTitle );
            // ... (Pre-comparison logging) ...

            // --- Determine processing path ---
            String processingPathIdentifier = "";
            if ( modalWasDisplayed ) {
                processingPathIdentifier = "AfterModal_" + pageIdentifierForSummary;
                System.out.println ( "  Processing Path: After Modal Displayed" );
                processSuperscriptsForFullPage ( processingPathIdentifier , true );
            } else if ( isCheckingLanding || isSavingsLanding ) {
                processingPathIdentifier = "TABS_" + pageIdentifierForSummary;
                System.out.println ( "  Processing Path: Tabs Page (" + actualPageTitleText + ")" );
                processSuperscriptsByTab ( actualPageTitleText , processingPathIdentifier ); // Pass key
            } else if ( isSimplePage ) {
                processingPathIdentifier = "SimplePage_" + pageIdentifierForSummary;
                System.out.println ( "  Processing Path: Simple Page (" + actualPageTitleText + ")" );
                processSuperscriptsForFullPage ( processingPathIdentifier , true );
            } else {
                processingPathIdentifier = "Default_" + pageIdentifierForSummary;
                System.out.println ( "  Processing Path: Default (Treat as Simple Page)" );
                processSuperscriptsForFullPage ( processingPathIdentifier , true );
            }

            // --- FINAL VALIDATION FOR THIS SCENARIO ---
            System.out.println ( "\n--- Performing Final Validation for Scenario using key: " + processingPathIdentifier + " ---" );
            VerificationSummary finalSummary = tabSummaries.get ( processingPathIdentifier );
            if ( finalSummary == null ) {
                System.err.println ( "ERROR: No summary object found for identifier '" + processingPathIdentifier + "'. Cannot perform validation." );
                overallValidationResultForScenario = "ERROR (No Summary)";
                // Fail test if summary missing? Maybe expected if no superscripts found at all.
            } else {
                actualTotalNumericSuperscriptsForScenario = finalSummary.numericSuperscriptsProcessed;
                System.out.println ( "  Actual Total Numeric Processed for Scenario: " + actualTotalNumericSuperscriptsForScenario );
                System.out.println ( "  Expected Total Numeric for Scenario        : " + expectedTotalNumericSuperscriptsForScenario );

                if ( expectedTotalNumericSuperscriptsForScenario != - 1 ) {
                    if ( actualTotalNumericSuperscriptsForScenario == expectedTotalNumericSuperscriptsForScenario ) {
                        overallValidationResultForScenario = "PASS";
                    } else {
                        overallValidationResultForScenario = "FAIL";
                    }
                    System.out.println ( "  Overall Scenario Result: " + overallValidationResultForScenario );
                    // Store result in the summary object itself
                    finalSummary.expectedNumericTotal = expectedTotalNumericSuperscriptsForScenario;
                    finalSummary.finalResult = overallValidationResultForScenario;
                } else { /* Handle skipped validation */ }
            }

        } catch (IOException ioEx) { /* Handle/Throw */
            throw ioEx;
        } catch (Exception ex) { /* Handle/Assert.fail */ }

        System.out.println ( "--- EXITING @Then 'I extract...' step ---" );
    }
    // --- Helper: Process Tab by Tab ---

    /**
     * Helper method to process superscripts for an entire page.
     * Used for simple pages, default cases, or after a modal has been handled.
     *
     * @param pageSummaryKey Base string for filenames (e.g., "SimplePage_MyPageTitle").
     * @param expandHeaders      Boolean flag indicating whether to expand collapsible headers first.
     * @throws IOException If screenshot saving fails.
     */
    private void processSuperscriptsForFullPage ( String pageSummaryKey , boolean expandHeaders ) throws IOException {
        System.out.println ( "Processing Full Page: " + pageSummaryKey );
        // Construct filename with path separator
        String screenshotFilename = "screenshots" + File.separator + pageSummaryKey + "_" + timestamp + ".png";

        // 1. Expand Headers if requested and possible
        if ( expandHeaders && page != null ) { /* Expand headers */ }
        BufferedImage combinedImage = takeFullPageScreenshotWithAShot ( driver );
        if ( combinedImage == null ) { /* Handle error */
            return;
        }
        By superscriptLocator = By.xpath ( "//sup[@class='c20ref']" );
        List<WebElement> superscripts = findElementsWithWait(driver, superscriptLocator, 15L); // Longer wait
        System.out.println ( "Found " + superscripts.size ( ) + " elements globally for: " + pageSummaryKey );
        VerificationSummary pageSummary = tabSummaries.computeIfAbsent ( pageSummaryKey , k -> new VerificationSummary ( ) );
        if ( ! superscripts.isEmpty ( ) ) {
            markAndCollectNumericSuperscripts ( driver , combinedImage , null , superscripts , null , pageSummaryKey , pageSummary );
        } else {
            pageSummary.totalSuperscriptsFoundInPanel = 0;
        }
        saveMarkedScreenshot ( combinedImage , screenshotFilename );



        // 2. Take Full Page Screenshot (using AShot wrapper)
        System.out.println("Taking full page screenshot for: " + pageSummaryKey);
        combinedImage = takeFullPageScreenshotWithAShot ( driver );
        if (combinedImage == null) {
            System.err.println("Screenshot failed for " + pageSummaryKey + ". Aborting processing for this page.");
            // Optionally create an empty summary entry to indicate failure?
            pageSummary = tabSummaries.computeIfAbsent ( pageSummaryKey , k -> new VerificationSummary ( ) );
            pageSummary.positionCheckErrors++; // Mark an error
            return; // Exit if screenshot failed
        }

        // 3. Find All Superscripts on the Page (with wait)
        System.out.println("Finding all superscripts on page (" + pageSummaryKey + ") using //sup[@class='c20ref']");
        superscriptLocator = By.xpath ( "//sup[@class='c20ref']" );
        superscripts = findElementsWithWait ( driver , superscriptLocator , timeout );
        if (superscripts == null) superscripts = new ArrayList<>();
        System.out.println("Found " + superscripts.size() + " elements globally with locator: " + superscriptLocator);

        // 4. Get/Create Summary Object // Corrected comment
        pageSummary = tabSummaries.computeIfAbsent ( pageSummaryKey , k -> new VerificationSummary ( ) );

        // 5. Call the Marking/Collection Method
        // Pass null for activePanel, pass the globally found list
        if (!superscripts.isEmpty()) {
            System.out.println("Calling markAndCollectNumericSuperscripts for full page: " + pageSummaryKey);
            markAndCollectNumericSuperscripts(driver, combinedImage, null, superscripts, null, pageSummaryKey, pageSummary);
        } else {
            System.out.println("No superscripts found on the page to mark for: " + pageSummaryKey);
            pageSummary.totalSuperscriptsFoundInPanel = 0; // Explicitly set count to 0
        }

        // 6. Save the Marked Screenshot
        System.out.println("Saving marked screenshot for " + pageSummaryKey);
        saveMarkedScreenshot(combinedImage, screenshotFilename); // Save image AFTER marking is done
    }

    // --- Ensure other required methods like markAndCollectNumericSuperscripts,
    // --- findElementsWithWait, takeFullPageScreenshotWithAShot, saveMarkedScreenshot, etc.,
    // --- and the inner classes SuperscriptInfo, VerificationSummary are present in this file. ---

    private static List<WebElement> findElementsWithWait( WebDriver driver, By locator, long timeout) { // Corrected method signature
        if (driver == null || locator == null) {
            System.err.println("Error: Invalid arguments passed to findElementsWithWait (driver or locator is null).");
            return null; // Return null to indicate a critical error
        }

        try {
            WebDriverWait wait = new WebDriverWait(driver, ofSeconds(timeout));
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

    private void processSuperscriptsByTab( String pageIdentifier , String processingPathIdentifier ) throws IOException {
        System.out.println("Processing Page Tab by Tab: " + pageIdentifier);
        WebDriverWait wait = new WebDriverWait(driver, ofSeconds(20)); // Longer wait for tabs
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
                markAndCollectNumericSuperscripts(driver, tabScreenshot, activePanel, null,  superscriptInPanelLocator, tabIdentifier,  currentTabSummary);

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
    // --- markAndCollectNumericSuperscripts Method ---
    // --- Place this method INSIDE SuperscriptStepDefinitions.java ---



    /**
     * Processes a list of potential superscripts (either found globally or within a specific panel),
     * filters for numeric ones, performs validation checks, draws markers on the image,
     * updates the summary object, adds details to the static list, and writes to the detail Excel sheet.
     *
     * @param driver                   The WebDriver instance.
     * @param combinedImage            The BufferedImage to draw marks on (MUST NOT be null).
     * @param activePanel              The WebElement of the active tab panel (null if processing a full page list).
     * @param superscriptsList         A pre-fetched list of superscripts (null if processing within activePanel).
     * @param specificSuperscriptLocator The By locator to find superscripts WITHIN activePanel (null if superscriptsList is provided).
     * @param identifier               A string identifying the current context (e.g., Page Title or Tab ID).
     * @param summary                  The VerificationSummary object for this identifier to update counts.
     */
    // --- Place this method INSIDE SuperscriptStepDefinitions.java ---



    /**
     * Processes a list of potential superscripts (either found globally or within a specific panel),
     * filters for numeric ones, performs validation checks (position, hyperlink), draws markers on the image,
     * updates the summary object, adds details to the static list, and writes to the detail Excel sheet row by row.
     *
     * @param driver                   The WebDriver instance.
     * @param combinedImage            The BufferedImage to draw marks on (MUST NOT be null).
     * @param activePanel              The WebElement of the active tab panel (null if processing a full page list).
     * @param superscriptsList         A pre-fetched list of superscripts (null if processing within activePanel).
     * @param specificSuperscriptLocator The By locator to find superscripts WITHIN activePanel (null if superscriptsList is provided).
     * @param identifier               A string identifying the current context (e.g., Page Title or Tab ID).
     * @param summary                  The VerificationSummary object for this identifier to update counts.
     */
    private void markAndCollectNumericSuperscripts(
            WebDriver driver, BufferedImage combinedImage,
            WebElement activePanel, List<WebElement> superscriptsList,
            By specificSuperscriptLocator,
            String identifier, // Page or Tab identifier for DETAIL row
            VerificationSummary summary)   // Summary object to UPDATE
    {
        // --- Initial Null Checks ---
        if (combinedImage == null) {
            System.err.println("ERROR: Cannot process/mark superscripts for [" + identifier + "] because the input image (combinedImage) is null.");
            int potentialErrors = 0;
            if (summary != null) { // Check if summary exists before updating
                if (activePanel != null && specificSuperscriptLocator != null) { try { potentialErrors = activePanel.findElements(specificSuperscriptLocator).size(); } catch (Exception ignored) {} }
                else if (superscriptsList != null) { potentialErrors = superscriptsList.size(); }
                summary.positionCheckErrors += potentialErrors;
            }
            return;
        }
        if (summary == null || workbook == null || detailSheet == null) {
            System.err.println("Cannot process superscripts: Null object detected (Summary="+(summary==null)+", Workbook="+(workbook==null)+", DetailSheet="+(detailSheet==null)+") for identifier: " + identifier);
            return; // Cannot proceed without these
        }
        // --- End Null Checks ---

        Graphics2D graphics = null; // Initialize graphics to null
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> superscriptsToProcess = new ArrayList<>();

        try {
            // --- Determine list of superscripts to process ---
            if (activePanel != null && specificSuperscriptLocator != null) {
                // Processing within a specific panel
                System.out.println("Waiting for VISIBLE superscripts within panel [" + identifier + "] using locator: " + specificSuperscriptLocator);
                try {
                    WebDriverWait panelWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    // Wait for at least one matching element to be VISIBLE within the panel
                    panelWait.until(d -> {
                        try {
                            // Check for staleness of panel itself before finding children
                            if (!activePanel.isDisplayed()) return false; // Implicitly checks staleness somewhat
                            return activePanel.findElements(specificSuperscriptLocator).stream().anyMatch(WebElement::isDisplayed);
                        } catch (StaleElementReferenceException e) {
                            System.err.println("Panel became stale during visibility wait for: " + identifier);
                            return false; // Let wait continue or timeout
                        }
                    });
                    Thread.sleep(250); // Pause after visibility confirmed
                    // Find all elements within the panel using the specific locator
                    superscriptsToProcess = activePanel.findElements(specificSuperscriptLocator);
                } catch (TimeoutException te) {
                    System.out.println("No visible superscripts found in panel for: " + identifier + " using " + specificSuperscriptLocator);
                    // superscriptsToProcess remains empty, which is handled below
                } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                catch (StaleElementReferenceException se) { System.err.println("Panel became stale while finding superscripts for: " + identifier); }
                catch (Exception e) { System.err.println("Error finding superscripts in panel for "+identifier+": "+e.getMessage()); }

            } else if (superscriptsList != null) {
                // Processing a pre-fetched list (full page)
                System.out.println("Processing provided list of " + superscriptsList.size() + " superscripts for: " + identifier);
                superscriptsToProcess = superscriptsList;
            } else {
                System.err.println("Error: Invalid parameters. Need either activePanel+locator OR superscriptsList for identifier: " + identifier);
                return; // Cannot proceed
            }

            // --- Process Found Superscripts ---
            if (superscriptsToProcess.isEmpty()) {
                System.out.println("No superscripts found or identified to process/mark for: " + identifier);
                // Update summary total found if it wasn't already (e.g., if panel was processed but empty)
                if (activePanel != null) summary.totalSuperscriptsFoundInPanel = 0;
                return; // Exit if list is empty
            }

            // Create graphics context now that we know we have elements and a valid image
            System.out.println("Creating Graphics context for marking " + identifier + "...");
            try {
                graphics = combinedImage.createGraphics();
                graphics.setStroke(new BasicStroke(2)); // Set default stroke
                System.out.println("Graphics context created.");
            } catch (Throwable graphEx) { // Catch Throwable for unexpected graphics errors like HeadlessException
                System.err.println("!!! FAILED TO CREATE GRAPHICS CONTEXT for " + identifier + ": " + graphEx.getMessage());
                graphEx.printStackTrace();
                graphics = null; // Ensure it's null on failure
                summary.positionCheckErrors += superscriptsToProcess.size(); // Count all as errors if graphics fail
            }

            // Update total count in summary (use exact size of list we are processing)
            // If processing full page list, set total. If processing tabs, accumulate via caller.
            if (activePanel == null) summary.totalSuperscriptsFoundInPanel = superscriptsToProcess.size();
            // Accumulation for tabs happens in the calling method (processSuperscriptsByTab) before this now

            System.out.println("Processing " + superscriptsToProcess.size() + " potential superscripts for context: " + identifier);

            Pattern numericPattern = Pattern.compile("^\\d+$");
            int processedNumericCountThisCall = 0; // Track numeric count for this specific call

            // --- Process Each Superscript ---
            for (int j = 0; j < superscriptsToProcess.size(); j++) {
                WebElement sup = superscriptsToProcess.get(j);
                String elementIdForLog = getElementIdentifierForLog(sup) + " [Index " + j + "]";
                System.out.println("\n  --- Checking Element: " + elementIdForLog + " ---");

                // Create info object WITH DEFAULTS for error cases
                SuperscriptInfo info = new SuperscriptInfo();
                info.tabIdentifier = identifier; // Set identifier immediately
                info.superscriptText = "[ERROR]"; // Default error value
                info.precedingText = "[ERROR]";
                info.isHyperlink = false;
                info.linkHref = "[ERROR]";
                info.positionStatus = SuperscriptInfo.PositionStatus.CHECK_ERROR; // Default error status
                info.verificationMessage = "Error during processing"; // Default error message

                boolean isEmpty = true;
                boolean isNumeric = false;
                boolean proceedToExcelWrite = false; // Flag to control writing full vs partial row

                try {
                    // --- 1. Get Text ---
                    String supTextRaw = null;
                    System.out.println("    1. Attempting getText()...");
                    try {
                        supTextRaw = sup.getText();
                        if (supTextRaw == null) supTextRaw = "";
                        info.superscriptText = supTextRaw.trim(); // Populate info object
                        isEmpty = info.superscriptText.isEmpty();
                        System.out.println("       Raw Text: '" + supTextRaw + "' | Trimmed: '" + info.superscriptText + "' | isEmpty: " + isEmpty);
                    } catch (StaleElementReferenceException seText) {
                        System.err.println("    1. StaleElement getting text for " + elementIdForLog + ". Skipping element.");
                        summary.positionCheckErrors++; // Count as error
                        writePartialDetailRowOnError(info); // Write minimal info
                        continue; // Skip rest for this element
                    } catch (Exception eText) {
                        System.err.println("    1. ERROR Getting Text for " + elementIdForLog + ": " + eText.getMessage());
                        summary.positionCheckErrors++; // Count as error
                        writePartialDetailRowOnError(info); // Write minimal info
                        continue; // Skip rest for this element
                    }

                    // --- 2. Handle Empty Tags ---
                    if (isEmpty) {
                        System.out.println("    2. Result: Tag is EMPTY.");
                        info.precedingText = getPrecedingText(sup, driver); // Try to get context
                        summary.emptySuperscriptLogs.add(identifier + ": Empty near '" + info.precedingText + "' " + elementIdForLog);
                        writePartialDetailRowOnError(info); // Write partial row for empty tag
                        continue; // Skip to the next superscript
                    }

                    // --- 3. Check Numeric ---
                    Matcher matcher = numericPattern.matcher(info.superscriptText); // Check trimmed text
                    isNumeric = matcher.matches();
                    System.out.println("    3. Is Numeric ('" + info.superscriptText + "')? " + isNumeric);

                    // --- 4. Process ONLY if Numeric ---
                    if (isNumeric) {
                        processedNumericCountThisCall++;
                        summary.numericSuperscriptsProcessed++; // Update summary object count

                        // --- 4a. Get Preceding Text ---
                        System.out.println("    4a. Attempting getPrecedingText()...");
                        try {
                            info.precedingText = getPrecedingText(sup, driver); // Use JS helper
                        } catch (Exception ePrec) {
                            info.precedingText = "[ERROR]"; summary.positionCheckErrors++;
                            System.err.println("        ERROR Getting Preceding Text: " + ePrec.getMessage());
                        }
                        System.out.println("        Preceding Text: '" + info.precedingText + "'");


                        // --- 4b. Position Check ---
                        System.out.println("    4b. Attempting checkSuperscriptPositionCoordinates()...");
                        try {
                            info.positionStatus = checkSuperscriptPositionCoordinates(sup, driver); // Use Coord version
                            info.verificationMessage = getPositionVerificationMessage(info.positionStatus, info.superscriptText);
                            if (info.positionStatus == SuperscriptInfo.PositionStatus.ABOVE_BASELINE) summary.positionCheckPassed++;
                            else if (info.positionStatus == SuperscriptInfo.PositionStatus.SAME_LINE) summary.positionCheckFailed++;
                            else summary.positionCheckErrors++; // Increment error count if check failed
                        } catch (Exception ePosition) {
                            info.positionStatus = SuperscriptInfo.PositionStatus.CHECK_ERROR;
                            info.verificationMessage = "[ERROR Checking Position]";
                            summary.positionCheckErrors++;
                            System.err.println("        ERROR Checking Position: " + ePosition.getMessage());
                        }
                        System.out.println("        Position Status: " + info.positionStatus);


                        // --- 4c. Hyperlink Check ---
                        System.out.println("    4c. Attempting Hyperlink Check...");
                        info.isHyperlink = false; info.linkHref = "NO HYPERLINK"; // Reset defaults
                        try {
                            // Check parent OR ancestor 'a' tag
                            WebElement linkElement = sup.findElement(By.xpath("./parent::a | ./ancestor::a[1]"));
                            String href = linkElement.getAttribute("href");
                            info.isHyperlink = (href != null && !href.trim().isEmpty() && !href.toLowerCase().startsWith("javascript:"));
                            info.linkHref = href != null ? href.trim() : "N/A (Empty href)";
                            if (info.isHyperlink) summary.hyperlinkCount++;
                            else summary.noHyperlinkCount++;
                        } catch (NoSuchElementException eLink) {
                            summary.noHyperlinkCount++; // Expected if no link
                        } catch (StaleElementReferenceException staleLinkEx) {
                            System.err.println("        WARN: Stale checking link for " + elementIdForLog);
                            summary.noHyperlinkCount++;
                            info.linkHref = "[Error: Stale Link Check]";
                        } catch (Exception eLinkOther) {
                            System.err.println("        ERROR Checking link for " + elementIdForLog + ": " + eLinkOther.getMessage());
                            summary.noHyperlinkCount++; // Count as no link on error
                            info.linkHref = "[Error: Link Check Exception]";
                        }
                        System.out.println("        Is Hyperlink: " + info.isHyperlink + (info.isHyperlink ? " ("+info.linkHref+")" : ""));


                        // Add fully populated detail object to the static list *after* all checks
                        allSuperscriptDetails.add(info);

                        // --- 4d. Draw Oval ---
                        System.out.println("    4d. Attempting to Draw Oval...");
                        if (graphics != null) { // Only draw if graphics context is valid
                            Map<String, Object> rect = null;
                            try {
                                System.out.println("        Getting coordinates via JS...");
                                rect = (Map<String, Object>) js.executeScript(
                                        "var elem = arguments[0]; if (!elem) return null; var rect = elem.getBoundingClientRect(); if(rect.width===0 || rect.height===0) return null; return { top: rect.top + window.pageYOffset, left: rect.left + window.pageXOffset, width: rect.width, height: rect.height };", sup);

                                if (rect != null) {
                                    System.out.println("        Coordinates obtained: " + rect);
                                    int x=0, y=0, width=0, height=0, drawX=0, drawY=0, paddedWidth=10, paddedHeight=10; // Initialize
                                    try {
                                        x = ((Number) rect.get("left")).intValue();
                                        y = ((Number) rect.get("top")).intValue();
                                        width = ((Number) rect.get("width")).intValue();
                                        height = ((Number) rect.get("height")).intValue();
                                        paddedWidth = Math.max(width + 6, 10);
                                        paddedHeight = Math.max(height + 6, 10);
                                        drawX = x + (width / 2) - (paddedWidth / 2); // Center oval
                                        drawY = y + (height / 2) - (paddedHeight / 2); // Center oval

                                        // Set color based on hyperlink OR position status
                                        if (info.positionStatus == SuperscriptInfo.PositionStatus.SAME_LINE) {
                                            graphics.setColor(Color.MAGENTA); // Prioritize FAIL color
                                        } else {
                                            graphics.setColor(info.isHyperlink ? Color.GREEN : Color.RED); // Green for link, Red for no link (if position PASS/OK)
                                        }
                                        System.out.println("        Drawing oval at (" + drawX + "," + drawY + ") Size (" + paddedWidth + "x" + paddedHeight + ") Color: " + graphics.getColor());
                                        graphics.drawOval(drawX, drawY, paddedWidth, paddedHeight); // Draw the oval

                                    } catch (Exception coordEx) {
                                        System.err.println("        Error calculating/drawing coords for sup '" + info.superscriptText + "': " + coordEx.getMessage());
                                        summary.positionCheckErrors++; // Count drawing error
                                    }
                                } else {
                                    System.err.println("        Failed to get coordinates for drawing " + elementIdForLog);
                                    summary.positionCheckErrors++; // Count coordinate error
                                }
                            } catch (Exception drawEx) {
                                System.err.println("        Error during drawing process for " + elementIdForLog + ": " + drawEx.getMessage());
                                summary.positionCheckErrors++; // Count drawing error
                            }
                        } else {
                            System.out.println("        Skipping drawing oval (graphics context is null).");
                            // Count as position error if drawing was skipped due to graphics failure? Yes.
                            summary.positionCheckErrors++;
                        }

                        // *** Set flag to write full row for numeric ***
                        proceedToExcelWrite = true;
                        System.out.println("    Result: PROCESSED as Numeric.");

                    } else {
                        // --- Handle Non-Numeric ---
                        System.out.println("    Result: Value is NON-NUMERIC.");
                        summary.nonNumericSuperscriptLogs.add(identifier + ": " + info.superscriptText + " " + elementIdForLog);
                        // Set flag to write partial non-numeric row if desired
                        info.verificationMessage = "Non-numeric value"; // Set message for partial row
                        proceedToExcelWrite = true; // Write partial row for non-numeric
                    }

                    // --- 4e. Write Detail Row (Conditionally) ---
                    if (proceedToExcelWrite) {
                        System.out.println("    4e. Writing detail row to Excel...");
                        writeDetailRowToSheet(info); // Write fully or partially populated info
                    }
                    // else { // No need for else, covered by non-numeric case above if needed
                    //      System.out.println("    4e. Skipping Excel write for this element.");
                    // }

                } catch (StaleElementReferenceException seOuter) {
                    System.err.println("  Outer Stale element reference for " + elementIdForLog + " for " + identifier);
                    summary.positionCheckErrors++;
                    info.superscriptText = "[STALE]"; // Indicate staleness
                    writePartialDetailRowOnError(info); // Write partial data if possible
                } catch (Exception supEx) {
                    System.err.println("  Outer Error processing superscript " + elementIdForLog + " for " + identifier + ": " + supEx.getMessage());
                    summary.positionCheckErrors++;
                    supEx.printStackTrace();
                    info.superscriptText = "[ERROR]";
                    writePartialDetailRowOnError(info); // Write partial data if possible
                }
                System.out.println("  --- Finished Checking Element: " + elementIdForLog + " ---");
            } // end for loop over superscriptsToProcess

            System.out.println("Finished processing loop. Numeric processed in this call for " + identifier + ": " + processedNumericCountThisCall);

        } catch (Exception generalEx) {
            System.err.println("General error during markAndCollectNumericSuperscripts for " + identifier + ": " + generalEx.getMessage());
            if (summary != null) summary.positionCheckErrors++; // Increment general error count if possible
            generalEx.printStackTrace();
        } finally {
            // Dispose graphics context ONLY if it was successfully created
            if (graphics != null) {
                System.out.println("Disposing graphics context for: " + identifier);
                graphics.dispose();
            }
        }
    } // End of markAndCollectNumericSuperscripts// End of markAndCollectNumericSuperscripts // End of markAndCollectNumericSuperscripts


    // --- NEW HELPER: Write Detail Row Safely ---
    private void writeDetailRowToSheet(SuperscriptInfo info) {
        if (workbook == null || detailSheet == null) { /* Error */ return; }
        if (info == null) { /* Error */ return; }
        try {
            Row dataRow = detailSheet.createRow(detailRowCount++);
            int cellNum = 0;
            // Use defaults set in info object if checks failed
            dataRow.createCell(cellNum++).setCellValue(info.tabIdentifier);
            dataRow.createCell(cellNum++).setCellValue(info.superscriptText);
            dataRow.createCell(cellNum++).setCellValue(info.precedingText);
            dataRow.createCell(cellNum++).setCellValue(info.isHyperlink ? "YES" : "NO");
            dataRow.createCell(cellNum++).setCellValue(info.linkHref);
            dataRow.createCell(cellNum++).setCellValue(info.positionStatus.name()); // Write enum name
            dataRow.createCell(cellNum++).setCellValue(info.verificationMessage);
            // System.out.println("      Detail row written successfully.");
        } catch (Exception e) { /* Log error */ }
    }

    // --- NEW HELPER: Write Partial Row on Error/Skip ---
    // Writes only identifier and basic info when full processing fails
    private void writePartialDetailRowOnError(SuperscriptInfo info) {
        if (workbook == null || detailSheet == null || info == null) return; // Basic checks
        try {
            Row dataRow = detailSheet.createRow(detailRowCount++);
            int cellNum = 0;
            dataRow.createCell(cellNum++).setCellValue(info.tabIdentifier != null ? info.tabIdentifier : "N/A");
            dataRow.createCell(cellNum++).setCellValue(info.superscriptText != null ? info.superscriptText : "[ERROR/EMPTY]");
            dataRow.createCell(cellNum++).setCellValue(info.precedingText != null ? info.precedingText : "[N/A]");
            // Fill remaining columns with error/skipped indicators
            dataRow.createCell(cellNum++).setCellValue("N/A");
            dataRow.createCell(cellNum++).setCellValue("N/A");
            dataRow.createCell(cellNum++).setCellValue("SKIPPED/ERROR");
            dataRow.createCell(cellNum++).setCellValue("Processing skipped or failed early");
            System.out.println("      Partial error/skipped row written to Excel.");
        } catch (Exception e) {
            System.err.println("ERROR writing PARTIAL detail row to Excel: " + e.getMessage());
        }
    }  // End of markAndCollectNumericSuperscripts

    private static String getElementIdentifierForLog ( WebElement sup ) {
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
    private static SuperscriptInfo.PositionStatus checkSuperscriptPositionCoordinates(WebElement supElement, WebDriver driver) {
        if (supElement == null) return SuperscriptInfo.PositionStatus.CHECK_ERROR;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            // Get Rect for Superscript relative to document top/left
            Map<String, Object> supRectMap = (Map<String, Object>) js.executeScript(
                    "var elem = arguments[0]; if (!elem) return null; var rect = elem.getBoundingClientRect(); if(rect.width===0 || rect.height===0) return null; return { top: rect.top + window.pageYOffset, left: rect.left + window.pageXOffset, width: rect.width, height: rect.height };", supElement);

            if (supRectMap == null) {
                System.err.println("Position check: Could not get valid rect for superscript " + getElementIdentifierForLog(supElement));
                return SuperscriptInfo.PositionStatus.CHECK_ERROR;
            }
            double supTop = ((Number) supRectMap.get("top")).doubleValue();

            // Get Rect for Parent relative to document top/left
            WebElement parentElement = supElement.findElement(By.xpath("./..")); // Find parent
            Map<String, Object> parentRectMap = (Map<String, Object>) js.executeScript(
                    "var elem = arguments[0]; if (!elem) return null; var rect = elem.getBoundingClientRect(); if(rect.width===0 || rect.height===0) return null; return { top: rect.top + window.pageYOffset };", parentElement); // Only need top

            if (parentRectMap == null) {
                System.err.println("Position check: Could not get valid rect for parent of " + getElementIdentifierForLog(supElement));
                return SuperscriptInfo.PositionStatus.CHECK_ERROR;
            }
            double parentTop = ((Number) parentRectMap.get("top")).doubleValue();

            // --- Comparison Logic ---
            // If superscript top is significantly less than parent top, it's above.
            // Allow a small tolerance (e.g., 2 pixels) for rendering variations.
            // A difference of 0 or 1, or supTop being GREATER, indicates same line or lower.
            double difference = parentTop - supTop;
            System.out.println("  Debug Pos Check: " + getElementIdentifierForLog(supElement) + " | supTop=" + supTop + " | parentTop=" + parentTop + " | diff=" + difference);

            if (difference > 1.5) { // If supTop is at least 1.5px higher than parentTop
                return SuperscriptInfo.PositionStatus.ABOVE_BASELINE;
            } else {
                // Includes cases where supTop == parentTop, supTop > parentTop, or very slightly higher
                return SuperscriptInfo.PositionStatus.SAME_LINE;
            }

        } catch (StaleElementReferenceException se) {
            System.err.println("StaleElementReferenceException during coordinate position check for " + getElementIdentifierForLog(supElement));
            return SuperscriptInfo.PositionStatus.NOT_APPLICABLE;
        } catch (Exception e) {
            System.err.println("Error during coordinate position check for " + getElementIdentifierForLog(supElement) + ": " + e.getMessage().split("\n")[0]);
            // e.printStackTrace(); // Uncomment for detailed JS errors
            return SuperscriptInfo.PositionStatus.CHECK_ERROR;
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
    //private String getPositionVerificationMessage(SuperscriptInfo.PositionStatus status, String supText) {
        /**
         * Generates a human-readable PASS/FAIL/INFO/ERROR message based on the
         * calculated position status of a superscript.
         *
         * @param status    The PositionStatus enum value determined by the position check.
         * @param supText   The actual text content of the superscript (used for logging).
         * @return A formatted String describing the verification outcome.
         */
        private static String getPositionVerificationMessage(SuperscriptInfo.PositionStatus status, String supText) {
            // Handle null status defensively, though it ideally shouldn't happen if checkPosition is called
            if (status == null) {
                return "ERROR: Position status was null for superscript '" + supText + "'.";
            }

            String safeSupText = (supText != null && !supText       .isEmpty()) ? "'" + supText + "'" : "[Empty Tag]";

            switch (status) {
                case ABOVE_BASELINE:
                    // This is the desired "PASS" state for standard superscripts
                    return "PASS: Superscript " + safeSupText + " is displayed above the baseline.";

                case SAME_LINE:
                    // This is the "FAIL" state according to the requirement
                    // (i.e., it's *not* above the baseline)
                    return "FAIL: Superscript " + safeSupText + " is displayed on the SAME LINE as preceding text baseline.";

                case CHECK_ERROR:
                    // An error occurred within the checkPosition JavaScript or processing
                    return "ERROR: Could not reliably determine position for superscript " + safeSupText + " due to check error.";

                case NOT_APPLICABLE:
                    // The check couldn't be performed, often due to a stale element
                    return "N/A: Position check skipped/not applicable for " + safeSupText + " (e.g., due to stale element).";

                default:
                    // Should not happen if all enum values are handled, but include as fallback
                    return "ERROR: Unknown position status (" + status + ") for superscript " + safeSupText + ".";
            }
        }

        // --- Inner Class Definitions (ensure these exist) ---
        // You need the SuperscriptInfo class with the PositionStatus enum defined
        // either here as static inner classes or as separate top-level classes.

    /* Example (if using inner classes):
    static class SuperscriptInfo {
        // ... other fields ...
        PositionStatus positionStatus;
        String verificationMessage;

        enum PositionStatus {
            ABOVE_BASELINE,
            SAME_LINE,
            CHECK_ERROR,
            NOT_APPLICABLE
        }
        // ... toString() etc. ...
    }
    */


    // Creates safe filename part
    private String getSafeFilenameBase() {
        String safeTitle = pageTitleFromDriver.replaceAll("[^a-zA-Z0-9.-]", "_");
        return safeTitle.length() > 40 ? safeTitle.substring(0, 40) : safeTitle;
    }
    // --- Helper method to sum up counts for the current scenario ---
    // Refined logic to better identify relevant summaries
    private int calculateTotalNumericProcessedForScenario(String pageIdBase, String pageTitle) {
        int total = 0;
        System.out.println("Calculating total numeric processed for page/tab summaries related to: " + pageIdBase);
        // Iterate through summaries added/updated during this run
        for (Map.Entry<String, VerificationSummary> entry : tabSummaries.entrySet()) {
            String key = entry.getKey();
            // Logic to identify summaries relevant to THIS specific scenario run.
            // Assumes identifiers created in processSuperscriptsByTab and processSuperscriptsForFullPage
            // contain the page title or the base identifier.
            boolean isRelevant = false;
            if (key.equals("AfterModal_" + pageIdBase) ||
                    key.equals("SimplePage_" + pageIdBase) ||
                    key.equals("Default_" + pageIdBase)) {
                isRelevant = true; // Matches full page processing identifier
            } else if (key.contains(" (" + pageTitle + ")")) { // Matches tab identifier format
                isRelevant = true;
            }

            if (isRelevant) {
                System.out.println("  Including count from summary key: " + key + " (Count: " + entry.getValue().numericSuperscriptsProcessed + ")");
                total += entry.getValue().numericSuperscriptsProcessed;
            }
        }
        // Fallback if the above logic fails: If only ONE summary exists starting with the baseId, use it.
        if (total == 0) {
            List<Map.Entry<String, VerificationSummary>> relevantEntries = tabSummaries.entrySet().stream()
                    .filter(e -> e.getKey().contains(pageIdBase) && !e.getKey().startsWith("OVERALL:"))
                    .toList();
            if (relevantEntries.size() == 1) {
                System.out.println("  Fallback: Using single relevant summary entry: " + relevantEntries.get(0).getKey());
                total = relevantEntries.get(0).getValue().numericSuperscriptsProcessed;
            } else {
                System.out.println("  Fallback failed or found multiple entries. Total remains 0.");
            }
        }
        return total;
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
            FileOutputStream fileOut = new FileOutputStream(excelFile);
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
