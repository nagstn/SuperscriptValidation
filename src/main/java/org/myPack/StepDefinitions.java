package org.myPack;

// --- Essential Imports ---
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// AShot Imports (Assuming needed for takeFullPageScreenshotWithAShot implementation)
// import ru.yandex.qatools.ashot.AShot;
// import ru.yandex.qatools.ashot.Screenshot;
// import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

// Java IO and Graphics Imports
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color; // Added for drawing
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections; // Added for emptyList
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

// Renamed class to follow Java conventions and reflect its purpose
class SuperscriptProcessor {

    // --- Inner Class Definitions ---
    // (Defined only once)
    public static class SuperscriptInfo {
        String tabIdentifier = "N/A";
        String superscriptText = "N/A";
        String precedingText = "N/A";
        boolean isHyperlink = false;
        String linkHref = "N/A";
        PositionStatus positionStatus = PositionStatus.CHECK_ERROR;
        String verificationMessage = "Not Processed";

        // Enum for clarity
        public enum PositionStatus {ABOVE_BASELINE, SAME_LINE, CHECK_ERROR, NOT_APPLICABLE}

        @Override
        public String toString() {
            return "SuperscriptInfo{" +
                    "tabIdentifier='" + tabIdentifier + '\'' +
                    ", superscriptText='" + superscriptText + '\'' +
                    ", precedingText='" + precedingText + '\'' +
                    ", isHyperlink=" + isHyperlink +
                    ", linkHref='" + linkHref + '\'' +
                    ", positionStatus=" + positionStatus +
                    ", verificationMessage='" + verificationMessage + '\'' +
                    '}';
        }
    }

    public static class VerificationSummary {
        int totalSuperscriptsFoundInPanel = 0;
        int numericSuperscriptsProcessed = 0;
        int positionCheckPassed = 0;
        int positionCheckFailed = 0;
        int positionCheckErrors = 0;
        int hyperlinkCount = 0;
        int noHyperlinkCount = 0;
        List<String> emptySuperscriptLogs = new ArrayList<>();
        List<String> nonNumericSuperscriptLogs = new ArrayList<>();

        @Override
        public String toString() {
            return "VerificationSummary{" +
                    "totalFound=" + totalSuperscriptsFoundInPanel +
                    ", numericProcessed=" + numericSuperscriptsProcessed +
                    ", posPassed=" + positionCheckPassed +
                    ", posFailed=" + positionCheckFailed +
                    ", posErrors=" + positionCheckErrors +
                    ", hyperlinks=" + hyperlinkCount +
                    ", noHyperlinks=" + noHyperlinkCount +
                    ", emptyTags=" + emptySuperscriptLogs.size() +
                    ", nonNumericTags=" + nonNumericSuperscriptLogs.size() +
                    '}';
        }
    }
    // --- End Inner Classes ---

    // WebDriver should be managed within the scope that uses it (e.g., main or test methods)
    // private static WebDriver driver; // Removed static driver

    // Removed static collections for results, pass them as parameters instead
    // public static List<SuperscriptInfo> allSuperscriptDetails = new ArrayList<>();
    // public static Map<String, VerificationSummary> tabSummaries = new LinkedHashMap<>();
    // Removed unused static workbook
    // public static Workbook workbookOut;

    public static void main(String[] args) {
        WebDriver driver = null; // Declare driver locally within main
        // Collections local to this execution run
        List<SuperscriptInfo> currentRunDetails = new ArrayList<>();
        Map<String, VerificationSummary> currentRunSummaries = new LinkedHashMap<>();
        // Generate timestamp for this specific run
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String reportFilename = "Superscript_Report_" + timestamp + ".xlsx";
        String screenshotBaseDir = "screenshots"; // Define screenshot directory

        // Ensure screenshot directory exists
        File screenshotDir = new File(screenshotBaseDir);
        if (!screenshotDir.exists()) {
            if (screenshotDir.mkdirs()) {
                System.out.println("Created screenshot directory: " + screenshotDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create screenshot directory: " + screenshotDir.getAbsolutePath());
                // Decide if execution should stop
            }
        }


        try {
            // Consider making the driver path configurable (e.g., properties file or argument)
            System.setProperty("webdriver.chrome.driver", "C:\\Users\\nagar\\IdeaProjects\\DataValidationRates\\src\\main\\resources\\chromedriver.exe");
            driver = new ChromeDriver();
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5)); // Use implicit wait cautiously
            driver.manage().window().maximize();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // Default explicit wait

            // --- CONFIGURATION ---
            // Use local file for testing or a web URL
            //String url = "file:///C:/Users/nagar/OneDrive/Desktop/Demo_superscript_react_test.html";
             String url = "https://www.wellsfargo.com/savings-cds/platinum/";

            // Locators (Consider moving to a dedicated Page Object class if complexity grows)
            By tabContainerLocator = By.xpath("//div[@role='tablist' and contains(@class, 'table-tab-list')]");
            By tabLocator = By.xpath(".//button[@role='tab']"); // Relative to container
            String activePanelIndicatorAttribute = "aria-selected";
            String activePanelIndicatorValue = "true";
            By superscriptLocator = By.tagName("sup"); // Global locator
            By headerSummaryLocator = By.xpath("//summary[@aria-expanded='false']"); // For expanding sections

            // --- END CONFIGURATION ---

            System.out.println("Navigating to: " + url);
            driver.get(url);

            // --- Page Processing Logic ---
            boolean processTabs = false;
            WebElement tabContainer = null;
            try {
                // Use a shorter wait specifically for checking the presence of the tab container
                tabContainer = wait.withTimeout(Duration.ofSeconds(3)).until(ExpectedConditions.visibilityOfElementLocated(tabContainerLocator));
                // Check if tabs actually exist within the container
                if (tabContainer != null && !tabContainer.findElements(tabLocator).isEmpty()) {
                    processTabs = true;
                    System.out.println("Tab container found with tabs. Processing by tabs.");
                } else {
                    System.out.println("Tab container found but no tabs inside, or container not visible. Processing as full page.");
                }
            } catch (TimeoutException e) {
                System.out.println("Tab container locator timed out or not visible. Processing as full page.");
            } catch (NoSuchElementException e) {
                System.out.println("Tab container locator not found. Processing as full page.");
            } catch (Exception e) {
                // Catch other potential exceptions during the check
                System.err.println("Error checking for tab container: " + e.getMessage() + ". Processing as full page.");
            }

            String pageTitleForLog = driver.getTitle();
            if (pageTitleForLog == null || pageTitleForLog.trim().isEmpty()) {
                pageTitleForLog = url.substring(url.lastIndexOf('/') + 1); // Use filename if title is empty
                if (pageTitleForLog.isEmpty()) pageTitleForLog = "UntitledPage";
            }
            // Create a safe base name for files related to this page
            String fileBase = pageTitleForLog.replaceAll("[^a-zA-Z0-9.-]", "_").replaceAll("_+", "_");
            fileBase = fileBase.length() > 50 ? fileBase.substring(0, 50) : fileBase; // Limit length

            // Expand any collapsed sections first (affects both tabbed and full-page processing)
            expandCollapsibleSections(driver, headerSummaryLocator);

            if (processTabs && tabContainer != null) { // Ensure tabContainer is not null here
                processSuperscriptsByTab(driver, wait, tabContainer, tabLocator,
                        activePanelIndicatorAttribute, activePanelIndicatorValue, superscriptLocator,
                        pageTitleForLog, fileBase, timestamp, screenshotBaseDir,
                        currentRunDetails, currentRunSummaries);
            } else {
                processSuperscriptsForFullPage(driver, wait, superscriptLocator,
                        "FullPage_" + fileBase, timestamp, screenshotBaseDir,
                        currentRunDetails, currentRunSummaries);
            }

            System.out.println("\n--- FINAL SUMMARY ---");
            if (currentRunSummaries.isEmpty()) {
                System.out.println("No summaries generated.");
            } else {
                currentRunSummaries.forEach((key, value) -> System.out.println("Identifier: " + key + " -> " + value));
            }

        } catch (NoSuchWindowException e) {
            System.err.println("A critical error occurred: Browser window closed unexpectedly.");
        } catch (WebDriverException e) {
            System.err.println("A WebDriver error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) { // Catch IO exceptions from file operations
            System.err.println("An IO error occurred (e.g., writing screenshot/Excel): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch-all for other unexpected errors
            System.err.println("An unexpected error occurred during processing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // --- EXCEL WRITING LOGIC ---
            System.out.println("\n--- Preparing to write Excel file ---");
            if (currentRunDetails.isEmpty() && currentRunSummaries.isEmpty()) {
                System.out.println("No data collected to write to Excel.");
            } else {
                // Write collected data to the Excel file
                writeResultsToExcel(currentRunDetails, currentRunSummaries, reportFilename);
            }

            // --- Quit Driver ---
            if (driver != null) {
                System.out.println("\nQuitting WebDriver.");
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Error quitting WebDriver: " + e.getMessage());
                }
            }
            System.out.println("Execution finished.");
        } // End finally block
    } // End main method

    // --- Helper: Expand Collapsible Sections ---
    private static void expandCollapsibleSections(WebDriver driver, By summaryLocator) {
        try {
            List<WebElement> headers = driver.findElements(summaryLocator);
            if (!headers.isEmpty()) {
                System.out.println("Attempting to expand " + headers.size() + " collapsible sections...");
                JavascriptExecutor js = (JavascriptExecutor) driver;
                int expandedCount = 0;
                for (WebElement header : headers) {
                    try {
                        // Scroll into view slightly above the element for better visibility
                        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", header);
                        Thread.sleep(100); // Small pause after scroll
                        if (header.isDisplayed() && header.isEnabled()) {
                            // Use JS click as it's often more reliable for elements like <summary>
                            js.executeScript("arguments[0].click();", header);
                            expandedCount++;
                            Thread.sleep(100); // Pause after click to allow content to expand
                        } else {
                            System.out.println("  Skipping non-interactable header: " + getElementIdentifierForLog(header));
                        }
                    } catch (StaleElementReferenceException se) {
                        System.err.println("  Stale element reference while trying to expand a header. Skipping.");
                    } catch (ElementClickInterceptedException ec) {
                        System.err.println("  Element click intercepted while trying to expand header: " + getElementIdentifierForLog(header) + ". Skipping.");
                    } catch (Exception e) {
                        System.err.println("  Error expanding header " + getElementIdentifierForLog(header) + ": " + e.getMessage());
                    }
                }
                System.out.println("Expanded " + expandedCount + " sections.");
                if (expandedCount > 0) Thread.sleep(500); // Wait a bit longer after expanding all
            } else {
                System.out.println("No collapsible sections found to expand.");
            }
        } catch (Exception e) {
            System.err.println("Error finding or processing collapsible section headers: " + e.getMessage());
        }
    }


    // --- Helper: Process Full Page ---
    private static void processSuperscriptsForFullPage(
            WebDriver driver, WebDriverWait wait, By superscriptLocator,
            String pageSummaryKey, String timestamp, String screenshotBaseDir,
            List<SuperscriptInfo> detailList, Map<String, VerificationSummary> summaryMap) throws IOException {
        System.out.println("\n--- Processing Full Page: " + pageSummaryKey + " ---");
        String screenshotFilename = screenshotBaseDir + File.separator + pageSummaryKey + "_" + timestamp + ".png";

        // Take screenshot *before* finding elements, in case finding modifies the page state slightly
        BufferedImage combinedImage = takeFullPageScreenshotWithAShot(driver); // Assuming AShot implementation
        if (combinedImage == null) {
            System.err.println("ERROR: Failed to capture screenshot for " + pageSummaryKey + ". Cannot mark elements.");
            // Decide if we should still try to find elements or return
            // return; // Option: Stop if screenshot fails
        }

        // Find all superscript elements on the page using the global locator
        List<WebElement> superscripts = findElementsWithWait(driver, wait, superscriptLocator, Duration.ofSeconds(10));
        System.out.println("Found " + superscripts.size() + " potential superscript elements globally for: " + pageSummaryKey);

        VerificationSummary pageSummary = summaryMap.computeIfAbsent(pageSummaryKey, k -> new VerificationSummary());

        // Mark the screenshot and collect details
        markAndCollectNumericSuperscripts(driver, combinedImage, null, superscripts, null, pageSummaryKey, pageSummary, detailList);

        // Save the potentially marked screenshot
        if (combinedImage != null) {
            saveMarkedScreenshot(combinedImage, screenshotFilename);
        }
        System.out.println("--- Finished Processing Full Page: " + pageSummaryKey + " ---");
    }

    // --- Helper: Process Tab by Tab ---
    private static void processSuperscriptsByTab(
            WebDriver driver, WebDriverWait wait, WebElement tabContainer, By tabLocator,
            String activeAttr, String activeValue, By superscriptInPanelLocator, // Use specific locator for panels
            String pageTitleForLog, String pageKeyBase, String timestamp, String screenshotBaseDir,
            List<SuperscriptInfo> detailList, Map<String, VerificationSummary> summaryMap) throws IOException {

        System.out.println("\n--- Processing Page Tab by Tab: " + pageTitleForLog + " ---");
        List<WebElement> tabs = tabContainer.findElements(tabLocator);
        int tabCount = tabs.size();
        if (tabCount == 0) {
            System.out.println("No tabs found within the container to process.");
            return;
        }
        System.out.println("Found " + tabCount + " tabs to process.");

        // Create ONE summary entry for the whole tabbed page interaction
        String pageSummaryKey = "TABS_" + pageKeyBase;
        VerificationSummary pageLevelSummary = summaryMap.computeIfAbsent(pageSummaryKey, k -> new VerificationSummary());

        for (int i = 0; i < tabCount; i++) {
            WebElement currentTab = null;
            String tabDetailIdentifier = "[Unknown Tab " + i + "]";
            System.out.println("\n--- Processing Tab Index: " + i + " ---");

            try {
                // Re-find tabs in each iteration to avoid staleness
                tabs = tabContainer.findElements(tabLocator);
                if (i >= tabs.size()) {
                    System.err.println("Tab index " + i + " out of bounds after re-finding tabs. Skipping.");
                    continue;
                }
                currentTab = tabs.get(i);
                tabDetailIdentifier = getTabIdentifier(currentTab, i) + " (" + pageTitleForLog + ")"; // Get a meaningful name

                System.out.println("Processing Tab Name: " + tabDetailIdentifier);

                // --- Click Tab if Not Active ---
                String currentActiveState = currentTab.getAttribute(activeAttr);
                boolean needsClick = !activeValue.equalsIgnoreCase(currentActiveState);

                if (needsClick) {
                    System.out.println("  Tab is not active. Attempting to click.");
                    try {
                        // Scroll tab into view before clicking
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", currentTab);
                        Thread.sleep(100); // Pause after scroll
                        wait.until(ExpectedConditions.elementToBeClickable(currentTab)).click();
                        System.out.println("  Clicked tab: " + tabDetailIdentifier);
                        // Wait for the panel associated with the clicked tab to become visible/active
                        // This requires a reliable way to link the tab to its panel
                        Thread.sleep(500); // Simple pause - replace with explicit wait for panel if possible
                    } catch (TimeoutException te) {
                        System.err.println("  Timeout waiting for tab " + tabDetailIdentifier + " to be clickable. Skipping tab.");
                        pageLevelSummary.positionCheckErrors++; // Count as an error for the summary
                        continue; // Skip to next tab
                    } catch (ElementClickInterceptedException ec) {
                        System.err.println("  Click intercepted for tab " + tabDetailIdentifier + ". Another element is obscuring it. Skipping tab.");
                        pageLevelSummary.positionCheckErrors++;
                        continue;
                    } catch (Exception e) {
                        System.err.println("  Error clicking tab " + tabDetailIdentifier + ": " + e.getMessage());
                        pageLevelSummary.positionCheckErrors++;
                        continue; // Skip to next tab
                    }
                } else {
                    System.out.println("  Tab is already active.");
                    Thread.sleep(100); // Small pause even if active
                }

                // --- Find Active Panel ---
                // This logic depends heavily on how tabs/panels are linked (e.g., aria-controls, sibling divs)
                // Using a placeholder strategy - find the panel associated with the *currentTab*
                WebElement activePanel = findActiveTabPanel(driver, wait, currentTab); // Needs implementation
                if (activePanel == null) {
                    System.err.println("Could not reliably determine the active panel for tab: " + tabDetailIdentifier + ". Skipping analysis for this tab.");
                    pageLevelSummary.positionCheckErrors++;
                    continue; // Skip to next tab
                }
                System.out.println("  Active panel identified for tab: " + tabDetailIdentifier);

                // --- Screenshot and Analysis ---
                BufferedImage tabScreenshot = takeFullPageScreenshotWithAShot(driver); // Screenshot after panel is active
                if (tabScreenshot == null) {
                    System.err.println("ERROR: Failed to capture screenshot for tab " + tabDetailIdentifier + ". Cannot mark elements.");
                    // Continue processing data even if screenshot fails?
                }

                // Mark screenshot and collect data *within the active panel*
                markAndCollectNumericSuperscripts(driver, tabScreenshot, activePanel, null, superscriptInPanelLocator, tabDetailIdentifier, pageLevelSummary, detailList);

                // Save tab screenshot
                if (tabScreenshot != null) {
                    String safeTabId = tabDetailIdentifier.replaceAll("[^a-zA-Z0-9.-]", "_").replaceAll("_+", "_").replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
                    safeTabId = safeTabId.length() > 30 ? safeTabId.substring(0, 30) : safeTabId; // Sanitize and shorten
                    String tabScreenshotFilename = screenshotBaseDir + File.separator + pageKeyBase + "_TAB_" + safeTabId + "_" + timestamp + ".png";
                    saveMarkedScreenshot(tabScreenshot, tabScreenshotFilename);
                }

            } catch (StaleElementReferenceException se) {
                System.err.println("Stale element reference encountered while processing tab index " + i + ". Might need to re-find elements more frequently. Skipping tab.");
                pageLevelSummary.positionCheckErrors++; // Increment error count in the main summary
            } catch (Exception e) {
                System.err.println("Unexpected error processing tab [" + tabDetailIdentifier + "]: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for unexpected errors
                pageLevelSummary.positionCheckErrors++; // Increment error count
            }
            System.out.println("--- Finished Tab Index: " + i + " ---");
        } // End for loop iterating through tabs
        System.out.println("\nFinished processing all tabs for page: " + pageTitleForLog);
    }


    // --- Core Logic: Mark Screenshot and Collect Superscript Details ---
    private static void markAndCollectNumericSuperscripts(
            WebDriver driver, BufferedImage combinedImage, // Can be null if screenshot failed
            WebElement activePanel, // Null if processing full page
            List<WebElement> superscriptsList, // Null if processing panel
            By specificSuperscriptLocator, // Null if processing full page (using superscriptsList)
            String identifier, // Page or Tab identifier for DETAIL row
            VerificationSummary summary, // Summary object to UPDATE
            List<SuperscriptInfo> detailList // List to add results to
    ) {
        // --- Initial Null Checks ---
        if (summary == null) {
            System.err.println("CRITICAL ERROR: Summary object is null for identifier [" + identifier + "]. Cannot proceed with analysis.");
            return; // Cannot proceed without a summary object
        }
        if (detailList == null) {
            System.err.println("CRITICAL ERROR: Detail list object is null for identifier [" + identifier + "]. Cannot store results.");
            summary.positionCheckErrors++; // Log as an error in the summary
            return;
        }
        if (combinedImage == null) {
            System.err.println("WARNING: Screenshot (combinedImage) is null for [" + identifier + "]. Cannot draw markings.");
            // Continue processing data, but cannot mark image
        }

        Graphics2D graphics = null;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> superscriptsToProcess = new ArrayList<>();
        WebDriverWait elementWait = new WebDriverWait(driver, Duration.ofSeconds(3)); // Shorter wait for elements within panel/page

        try {
            // --- Determine list of superscripts to process ---
            if (activePanel != null && specificSuperscriptLocator != null) {
                // Processing within a specific panel
                System.out.println("Finding superscripts within panel [" + identifier + "] using locator: " + specificSuperscriptLocator);
                try {
                    // --- FIX START ---
                    // Wait for at least one element to be present *and visible* within the panel
                    // Use visibilityOfNestedElementsLocatedBy which accepts (WebElement parent, By childLocator)
                    elementWait.until(ExpectedConditions.visibilityOfNestedElementsLocatedBy( activePanel , specificSuperscriptLocator));
                    // --- FIX END ---
                    superscriptsToProcess = activePanel.findElements(specificSuperscriptLocator);
                    System.out.println("  Found " + superscriptsToProcess.size() + " potential superscripts in panel.");
                } catch (TimeoutException e) {
                    System.out.println("  No visible superscripts found (or timed out waiting) within the panel [" + identifier + "] using locator: " + specificSuperscriptLocator);
                    // No elements found is not necessarily an error
                } catch (NoSuchElementException e) {
                    System.out.println("  No superscripts found within the panel [" + identifier + "] using locator: " + specificSuperscriptLocator);
                }
            } else if (superscriptsList != null) {
                // Processing a pre-fetched list (likely for full page)
                System.out.println("Using pre-fetched list of " + superscriptsList.size() + " potential superscripts for [" + identifier + "]");
                superscriptsToProcess = superscriptsList;
            } else {
                System.err.println("ERROR: Invalid state in markAndCollectNumericSuperscripts for [" + identifier + "]. Either activePanel/locator or superscriptsList must be provided.");
                summary.positionCheckErrors++;
                return;
            }

            if (superscriptsToProcess.isEmpty()) {
                System.out.println("No superscripts to process for context: " + identifier);
                // Update summary total found (even if 0) - only if this is the first time for this identifier
                if (summary.totalSuperscriptsFoundInPanel == 0) { // Avoid double counting if called multiple times
                    // This check might be too simple if the method IS called multiple times intentionally
                    // Consider if totalSuperscriptsFoundInPanel should be reset or handled differently per call
                }
                return; // Nothing more to do
            }

            // --- Create Graphics Context (only if image exists) ---
            if (combinedImage != null) {
                try {
                    graphics = combinedImage.createGraphics();
                    graphics.setStroke(new BasicStroke(2)); // Line thickness
                    // Color will be set per element based on status
                } catch (Throwable graphEx) {
                    System.err.println("ERROR creating Graphics2D context for [" + identifier + "]: " + graphEx.getMessage());
                    graphics = null; // Ensure graphics is null if creation failed
                    summary.positionCheckErrors++; // Count as an error impacting marking
                }
            }

            // --- Update Summary Total Found ---
            // Be careful if this method can be called multiple times for the same summary object
            // This assumes it's called once per identifier context (page or tab)
            summary.totalSuperscriptsFoundInPanel = superscriptsToProcess.size();
            System.out.println("Processing " + superscriptsToProcess.size() + " potential superscripts for context: " + identifier);

            final Pattern numericPattern = Pattern.compile("^\\d+$"); // Matches strings containing only digits
            int processedNumericCountThisCall = 0;

            // --- Process Each Superscript ---
            for (int j = 0; j < superscriptsToProcess.size(); j++) {
                WebElement sup = null;
                SuperscriptInfo info = new SuperscriptInfo();
                info.tabIdentifier = identifier; // Assign the context identifier

                try {
                    sup = superscriptsToProcess.get(j); // Get element for this iteration
                    String elementIdForLog = getElementIdentifierForLog(sup) + " [Index " + j + "]"; // For logging

                    // --- Check Visibility --- // <<< CODE INSERTED HERE >>>
                    if (!sup.isDisplayed()) {
                        System.out.println("  Reporting non-visible superscript: " + elementIdForLog);

                        // Populate info for the report even though it's hidden
                        info.positionStatus = SuperscriptInfo.PositionStatus.NOT_APPLICABLE; // Position isn't applicable if not visible
                        info.verificationMessage = "Superscript Tag{sub) found but not visible/displayed.";

                        // Attempt to get text, preceding text, and link info, handling potential errors
                        try {
                            info.superscriptText = Objects.toString(sup.getText(), "").trim();
                            if (info.superscriptText.isEmpty()) {
                                info.superscriptText = "[EMPTY_TEXT]"; // Mark if text is empty while hidden
                            }
                        } catch (Exception e) {
                            info.superscriptText = "[HIDDEN_ERROR_GET_TEXT]";
                            System.err.println("  Error getting text for hidden " + elementIdForLog + ": " + e.getMessage().split("\n")[0]);
                        }

                        try {
                            info.precedingText = getPrecedingText(sup, js);
                        } catch (Exception e) {
                            info.precedingText = "[HIDDEN_ERROR_GET_PRECEDING]";
                            System.err.println("  Error getting preceding text for hidden " + elementIdForLog + ": " + e.getMessage().split("\n")[0]);
                        }

                        info.isHyperlink = false; // Default
                        info.linkHref = "N/A (Hidden)";
                        try {
                            WebElement parentAnchor = null;
                            try {
                                // Check if the hidden <sup> itself is, or is inside, an <a> tag
                                parentAnchor = sup.findElement(By.xpath(".//ancestor-or-self::a"));
                            } catch (NoSuchElementException nse) {
                                // No parent anchor found
                            }
                            if (parentAnchor != null) {
                                info.isHyperlink = true;
                                info.linkHref = parentAnchor.getAttribute("href");
                                if (info.linkHref == null || info.linkHref.trim().isEmpty()) {
                                    info.linkHref = "[HIDDEN_EMPTY_HREF]";
                                }
                            }
                        } catch (Exception e) {
                            info.linkHref = "[HIDDEN_ERROR_CHECK_LINK]";
                            System.err.println("  Error checking hyperlink for hidden " + elementIdForLog + ": " + e.getMessage().split("\n")[0]);
                        }

                        // Add the details of the hidden superscript to the main list
                        detailList.add(info);

                        // Optionally, log it in the summary as well (e.g., using nonNumeric or a new list)
                        summary.nonNumericSuperscriptLogs.add(identifier + " - HIDDEN: " + elementIdForLog + " Text: " + info.superscriptText);

                        continue; // Skip the rest of the numeric/position checks for this hidden element
                    }
                    // <<< END OF INSERTED CODE >>>

                    // --- Populate Info Fields Safely (for VISIBLE elements) ---
                    try {
                        info.superscriptText = Objects.toString(sup.getText(), "").trim();
                    } catch (StaleElementReferenceException se) {
                        System.err.println("  Stale element reference getting text for " + elementIdForLog + ". Skipping.");
                        summary.positionCheckErrors++; // Count as error
                        continue; // Skip this element
                    } catch (Exception e) {
                        info.superscriptText = "[ERROR_GET_TEXT]";
                        System.err.println("  Error getting text for " + elementIdForLog + ": " + e.getMessage());
                    }

                    boolean isEmpty = info.superscriptText.isEmpty() || info.superscriptText.startsWith("[ERROR");
                    if (isEmpty) {
                        System.out.println("  Skipping empty or error-text superscript: " + elementIdForLog + " (Text: '" + info.superscriptText + "')");
                        summary.emptySuperscriptLogs.add(identifier + " - " + elementIdForLog);
                        continue; // Skip further processing for this element
                    }

                    // --- Check if Numeric ---
                    boolean isNumeric = numericPattern.matcher(info.superscriptText).matches();

                    // --- Get Preceding Text (Always attempt) ---
                    try {
                        info.precedingText = getPrecedingText(sup, js); // Pass JS executor
                    } catch (Exception e) {
                        info.precedingText = "[ERROR_GET_PRECEDING]";
                        System.err.println("  Error getting preceding text for " + elementIdForLog + ": " + e.getMessage());
                    }

                    if (isNumeric) {
                        processedNumericCountThisCall++;
                        summary.numericSuperscriptsProcessed++; // Increment summary count

                        // --- Check Position ---
                        try {
                            info.positionStatus = checkSuperscriptPositionCoordinates(sup, driver, js); // Pass driver and js
                            info.verificationMessage = getPositionVerificationMessage(info.positionStatus, info.superscriptText);

                            // Update summary counts based on position check
                            switch (info.positionStatus) {
                                case ABOVE_BASELINE:
                                    summary.positionCheckPassed++;
                                    break;
                                case SAME_LINE:
                                    summary.positionCheckFailed++;
                                    break;
                                case CHECK_ERROR:
                                case NOT_APPLICABLE: // Treat N/A also as an error/issue for summary
                                    summary.positionCheckErrors++;
                                    break;
                            }
                        } catch (Exception e) {
                            System.err.println("  Error during position check for " + elementIdForLog + ": " + e.getMessage());
                            info.positionStatus = SuperscriptInfo.PositionStatus.CHECK_ERROR;
                            info.verificationMessage = "Position check failed with exception.";
                            summary.positionCheckErrors++;
                        }

                        // --- Check Hyperlink ---
                        info.isHyperlink = false; // Default
                        info.linkHref = "NO HYPERLINK";
                        try {
                            // Check if the <sup> itself is, or is inside, an <a> tag
                            WebElement parentAnchor = null;
                            try {
                                parentAnchor = sup.findElement(By.xpath(".//ancestor-or-self::a"));
                            } catch (NoSuchElementException nse) {
                                // No parent anchor found, it's not a hyperlink
                            }

                            if (parentAnchor != null) {
                                info.isHyperlink = true;
                                info.linkHref = parentAnchor.getAttribute("href");
                                if (info.linkHref == null || info.linkHref.trim().isEmpty()) {
                                    info.linkHref = "[EMPTY_HREF]";
                                }
                                summary.hyperlinkCount++;
                            } else {
                                summary.noHyperlinkCount++;
                            }
                        } catch (StaleElementReferenceException seLink) {
                            System.err.println("  Stale element reference checking hyperlink for " + elementIdForLog);
                            info.linkHref = "[ERROR_CHECK_LINK_STALE]";
                            summary.positionCheckErrors++; // Count link check errors here too? Or add separate counter?
                        } catch (Exception e) {
                            System.err.println("  Error checking hyperlink for " + elementIdForLog + ": " + e.getMessage());
                            info.linkHref = "[ERROR_CHECK_LINK]";
                            summary.positionCheckErrors++; // Or separate counter
                        }

                        // --- Add Result to Detail List ---
                        detailList.add(info);

                        // --- Draw Oval on Screenshot (if graphics context is available) ---
                        if (graphics != null) {
                            try {
                                Rectangle rect = sup.getRect();
                                // Adjust coordinates relative to the *full page* screenshot
                                // AShot's default strategy usually gives coordinates relative to viewport top-left.
                                // If using ShootingStrategies.viewportPasting, coordinates should be relative to document top-left.
                                // Assuming viewportPasting or similar for full page:
                                int x = rect.getX();
                                int y = rect.getY();
                                int width = rect.getWidth();
                                int height = rect.getHeight();

                                // Choose color based on position status
                                Color drawColor;
                                switch (info.positionStatus) {
                                    case ABOVE_BASELINE:
                                        drawColor = Color.GREEN; // Pass
                                        break;
                                    case SAME_LINE:
                                        drawColor = Color.RED;   // Fail
                                        break;
                                    case CHECK_ERROR:
                                    case NOT_APPLICABLE:
                                    default:
                                        drawColor = Color.ORANGE; // Error/Warning
                                        break;
                                }
                                graphics.setColor(drawColor);

                                // Draw oval slightly larger than the element
                                graphics.drawOval(x - 2, y - 2, width + 4, height + 4);

                            } catch (StaleElementReferenceException seDraw) {
                                System.err.println("  Stale element reference getting coordinates for drawing oval for " + elementIdForLog);
                            } catch (WebDriverException wdeDraw) {
                                System.err.println("  WebDriverException getting coordinates/drawing oval for " + elementIdForLog + ": " + wdeDraw.getMessage().split("\n")[0]);
                            } catch (Exception drawEx) {
                                System.err.println("  Error drawing oval for " + elementIdForLog + ": " + drawEx.getMessage());
                            }
                        } // end drawing logic

                    } else {
                        // Log non-numeric superscript text
                        System.out.println("  Skipping non-numeric superscript: " + elementIdForLog + " (Text: '" + info.superscriptText + "')");
                        summary.nonNumericSuperscriptLogs.add(identifier + " - " + elementIdForLog + " Text: " + info.superscriptText);
                        // Optionally add non-numeric ones to the detail list too, with a specific status?
                        // info.positionStatus = SuperscriptInfo.PositionStatus.NOT_APPLICABLE;
                        // info.verificationMessage = "Non-numeric content";
                        // detailList.add(info); // If you want them in the report
                    }

                } catch (StaleElementReferenceException seOuter) {
                    System.err.println("Stale element reference for superscript index " + j + " in context [" + identifier + "]. Skipping this element.");
                    summary.positionCheckErrors++; // Count as error
                    // Attempt to recover or just continue to the next element
                    continue;
                } catch (Exception supEx) {
                    System.err.println("Unexpected error processing superscript index " + j + " in context [" + identifier + "]: " + supEx.getMessage());
                    supEx.printStackTrace(); // Log stack trace for unexpected errors
                    summary.positionCheckErrors++;
                    // Ensure info object is added with error status if needed, or just log
                    if (info != null) { // If info was initialized
                        info.verificationMessage = "Processing failed with exception: " + supEx.getMessage().split("\n")[0];
                        info.positionStatus = SuperscriptInfo.PositionStatus.CHECK_ERROR;
                        // Decide if partially filled info should be added to detailList
                        // detailList.add(info);
                    }
                }
                // System.out.println("  --- Finished Checking Element: " + elementIdForLog + " ---"); // Can be verbose
            } // end inner loop processing superscripts

            System.out.println("Finished processing loop. Numeric processed in this call for " + identifier + ": " + processedNumericCountThisCall);

        } catch (Exception generalEx) {
            System.err.println("A general error occurred within markAndCollectNumericSuperscripts for [" + identifier + "]: " + generalEx.getMessage());
            generalEx.printStackTrace();
            if (summary != null) summary.positionCheckErrors++; // Increment error count if possible
        } finally {
            if (graphics != null) {
                graphics.dispose(); // Release graphics resources
            }
        }
    } // End of markAndCollectNumericSuperscripts


    /**
     * Checks the vertical position of a superscript element relative to its immediate parent
     * by comparing their top coordinates relative to the document.
     * Uses JavaScript for potentially more reliable coordinate retrieval.
     *
     * @param supElement The WebElement representing the <sup> tag. Must not be null.
     * @param driver     The WebDriver instance. Must not be null.
     * @param js         The JavascriptExecutor instance. Must not be null.
     * @return PositionStatus enum value indicating the result.
     */
    private static SuperscriptInfo.PositionStatus checkSuperscriptPositionCoordinates(WebElement supElement, WebDriver driver, JavascriptExecutor js) {
        // Basic null checks
        if (supElement == null || driver == null || js == null) {
            System.err.println("Position check error: Null parameter provided (supElement, driver, or js).");
            return SuperscriptInfo.PositionStatus.CHECK_ERROR;
        }

        String elementIdForLog = getElementIdentifierForLog(supElement); // Use helper for logging

        try {
            // --- Get Rect for Superscript using JS ---
            // System.out.println("    Attempting to get coordinates for superscript: " + elementIdForLog); // Verbose
            Object supRectObj = js.executeScript(
                    "var elem = arguments[0];" +
                            "if (!elem) return { error: 'Superscript element null in JS' };" +
                            "var rect = elem.getBoundingClientRect();" +
                            "if (rect.width === 0 || rect.height === 0 || !elem.checkVisibility()) return { error: 'Superscript has zero dimensions or is not visible' };" + // Added visibility check
                            "return { " +
                            "  top: rect.top + window.pageYOffset, " + // Coordinate relative to document top
                            "  height: rect.height " +
                            "};", supElement);

            // Validate JS result for superscript
            if (!(supRectObj instanceof Map)) {
                System.err.println("Position check error (" + elementIdForLog + "): JS did not return a Map for superscript rect. Result: " + supRectObj);
                return SuperscriptInfo.PositionStatus.CHECK_ERROR;
            }
            Map<String, Object> supRectMap = (Map<String, Object>) supRectObj;
            if (supRectMap.containsKey("error")) {
                System.err.println("Position check error (" + elementIdForLog + "): JS returned error for superscript: " + supRectMap.get("error"));
                // If not visible or zero dimensions, it's not applicable for position check relative to parent
                if (supRectMap.get("error").toString().contains("zero dimensions") || supRectMap.get("error").toString().contains("not visible")) {
                    return SuperscriptInfo.PositionStatus.NOT_APPLICABLE;
                }
                return SuperscriptInfo.PositionStatus.CHECK_ERROR;
            }

            // Safely extract numeric values
            double supTop = ((Number) supRectMap.get("top")).doubleValue();
            // double supHeight = ((Number) supRectMap.get("height")).doubleValue(); // Not currently used

            // --- Get Rect for Parent Element using JS ---
            // System.out.println("    Attempting to find parent and get its coordinates for: " + elementIdForLog); // Verbose
            Object parentRectObj = js.executeScript(
                    "var elem = arguments[0].parentElement;" + // Get parent directly in JS
                            "if (!elem) return { error: 'Parent element null in JS' };" +
                            "var rect = elem.getBoundingClientRect();" +
                            // Parent dimensions check is less critical, could be a warning
                            // "if(rect.width === 0 || rect.height === 0) return { warn: 'Parent has zero dimensions' };" +
                            "return { " +
                            "  top: rect.top + window.pageYOffset " + // Only need top coordinate relative to document
                            "};", supElement); // Pass the original sup element

            // Validate JS result for parent
            if (!(parentRectObj instanceof Map)) {
                System.err.println("Position check error (" + elementIdForLog + "): JS did not return a Map for parent rect. Result: " + parentRectObj);
                return SuperscriptInfo.PositionStatus.CHECK_ERROR;
            }
            Map<String, Object> parentRectMap = (Map<String, Object>) parentRectObj;
            if (parentRectMap.containsKey("error")) {
                System.err.println("Position check error (" + elementIdForLog + "): JS returned error for parent: " + parentRectMap.get("error"));
                return SuperscriptInfo.PositionStatus.CHECK_ERROR;
            }
            // if (parentRectMap.containsKey("warn")) { // Log warning but continue
            //     System.out.println("  Position check warning (" + elementIdForLog + "): " + parentRectMap.get("warn"));
            // }

            double parentTop = ((Number) parentRectMap.get("top")).doubleValue();

            // --- Comparison Logic ---
            double difference = parentTop - supTop; // Positive difference means sup is higher (top coord is smaller)
            // System.out.println("  Debug Pos Check: " + elementIdForLog + " | supTop=" + String.format("%.2f", supTop) + " | parentTop=" + String.format("%.2f", parentTop) + " | diff=" + String.format("%.2f", difference)); // Verbose

            // Define a tolerance. If the superscript's top is more than 'tolerance' pixels
            // *above* the parent's top, consider it ABOVE_BASELINE.
            // Adjust tolerance based on typical font sizes and rendering. 1.5 to 2.5 pixels often works.
            double tolerance = 1.0;
            if (difference > tolerance) {
                return SuperscriptInfo.PositionStatus.ABOVE_BASELINE;
            } else {
                // Includes cases where supTop is roughly equal, slightly above (within tolerance), or below parentTop.
                return SuperscriptInfo.PositionStatus.SAME_LINE;
            }

        } catch (StaleElementReferenceException se) {
            System.err.println("StaleElementReferenceException during coordinate position check for " + elementIdForLog);
            // If element is stale, we can't determine its position relative to parent anymore
            return SuperscriptInfo.PositionStatus.NOT_APPLICABLE;
        } catch (WebDriverException wde) {
            // Catch broader WebDriver exceptions, often related to JS execution or invalid elements
            System.err.println("WebDriverException during coordinate position check for " + elementIdForLog + ": " + wde.getMessage().split("\n")[0]);
            return SuperscriptInfo.PositionStatus.CHECK_ERROR;
        } catch (ClassCastException cce) {
            System.err.println("ClassCastException during coordinate position check for " + elementIdForLog + " (likely JS result conversion): " + cce.getMessage());
            return SuperscriptInfo.PositionStatus.CHECK_ERROR;
        } catch (Exception e) {
            // Catch any other unexpected errors
            System.err.println("Unexpected Error during coordinate position check for " + elementIdForLog + ": " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace(); // Print stack trace for unexpected issues
            return SuperscriptInfo.PositionStatus.CHECK_ERROR;
        }
    }


    // --- Helper: Write FINAL Results To Excel ---
    private static void writeResultsToExcel(List<SuperscriptInfo> details, Map<String, VerificationSummary> summaries, String filename) {
        System.out.println("\n--- Writing Final Results to Excel: " + filename + " ---");
        if (details.isEmpty() && summaries.isEmpty()) {
            System.out.println("No data collected, skipping Excel file generation.");
            return;
        }

        // Use try-with-resources for Workbook and FileOutputStream
        try (Workbook outputWorkbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(filename)) {

            // --- Write Detail Sheet ---
            Sheet detailSheet = outputWorkbook.createSheet("SuperscriptDetail");
            int detailRowIdx = 0;
            // Header Style
            CellStyle headerStyle = outputWorkbook.createCellStyle();
            Font headerFont = outputWorkbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header Row
            Row headerRow = detailSheet.createRow(detailRowIdx++);
            String[] headers = {"Tab/Page Identifier", "Superscript Text", "Preceding Text", "Is Hyperlink", "Link Href", "Position Status", "Position Result Msg"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            System.out.println("Writing " + details.size() + " detail records...");
            for (SuperscriptInfo info : details) {
                Row dataRow = detailSheet.createRow(detailRowIdx++);
                int cellNum = 0;
                dataRow.createCell(cellNum++).setCellValue(info.tabIdentifier);
                dataRow.createCell(cellNum++).setCellValue(info.superscriptText);
                dataRow.createCell(cellNum++).setCellValue(info.precedingText);
                dataRow.createCell(cellNum++).setCellValue(info.isHyperlink ? "YES" : "NO");
                dataRow.createCell(cellNum++).setCellValue(info.linkHref);
                // Handle potential null status gracefully
                dataRow.createCell(cellNum++).setCellValue(info.positionStatus != null ? info.positionStatus.name() : "UNKNOWN_STATUS");
                dataRow.createCell(cellNum++).setCellValue(info.verificationMessage);
            }
            // Auto-size columns after data is written
            for (int i = 0; i < headers.length; i++) {
                detailSheet.autoSizeColumn(i);
            }

            // --- Write Summary Sheet ---
            Sheet summarySheet = outputWorkbook.createSheet("ValidationSummary");
            int summaryRowIdx = 0;
            // Header Row
            Row summaryHeader = summarySheet.createRow(summaryRowIdx++);
            String[] summaryHeaders = {"Page/Tab Identifier", "Total Found", "Numeric Processed", "Position Passed", "Position Failed", "Position Errors/NA", "Hyperlinks Found", "No Hyperlinks", "Empty Tags Found", "Non-Numeric Tags Found"};
            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell cell = summaryHeader.createCell(i);
                cell.setCellValue(summaryHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            System.out.println("Writing " + summaries.size() + " summary records...");
            for (Map.Entry<String, VerificationSummary> entry : summaries.entrySet()) {
                Row summaryDataRow = summarySheet.createRow(summaryRowIdx++);
                VerificationSummary summary = entry.getValue();
                int cellNum = 0;
                summaryDataRow.createCell(cellNum++).setCellValue(entry.getKey()); // Identifier
                summaryDataRow.createCell(cellNum++).setCellValue(summary.totalSuperscriptsFoundInPanel);
                summaryDataRow.createCell(cellNum++).setCellValue(summary.numericSuperscriptsProcessed);
                summaryDataRow.createCell(cellNum++).setCellValue(summary.positionCheckPassed);
                summaryDataRow.createCell(cellNum++).setCellValue(summary.positionCheckFailed);
                summaryDataRow.createCell(cellNum++).setCellValue(summary.positionCheckErrors); // Combined Errors/NA
                summaryDataRow.createCell(cellNum++).setCellValue(summary.hyperlinkCount);
                summaryDataRow.createCell(cellNum++).setCellValue(summary.noHyperlinkCount);
                summaryDataRow.createCell(cellNum++).setCellValue(summary.emptySuperscriptLogs.size());
                summaryDataRow.createCell(cellNum++).setCellValue(summary.nonNumericSuperscriptLogs.size());
                // Log details for empty/non-numeric could be added here or in separate sheets if needed
            }
            // Auto-size columns
            for (int i = 0; i < summaryHeaders.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            // --- Save File ---
            outputWorkbook.write(fileOut);
            System.out.println("----> Excel file successfully written to: " + new File(filename).getAbsolutePath());

        } catch (IOException e) {
            System.err.println("ERROR writing Excel file '" + filename + "': " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected ERROR during Excel writing for '" + filename + "': " + e.getMessage());
            e.printStackTrace();
        }
    }


    // --- Placeholder/Stub Implementations for Required Helper Methods ---
    // Replace these with actual logic, especially for AShot and panel finding.

    /**
     * Takes a full-page screenshot using AShot library.
     * Requires AShot dependency and imports.
     *
     * @param driver WebDriver instance.
     * @return BufferedImage of the full page, or null on error.
     */
    private static BufferedImage takeFullPageScreenshotWithAShot(WebDriver driver) {
        System.out.println("  Attempting full page screenshot...");
        // Requires AShot dependency: com.github.yandex-qatools.ashot:ashot
        // import ru.yandex.qatools.ashot.AShot;
        // import ru.yandex.qatools.ashot.Screenshot;
        // import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
        try {
            // Use viewportPasting for scrolling and stitching. Adjust timeout if needed.
            // Screenshot screenshot = new AShot()
            //         .shootingStrategy(ShootingStrategies.viewportPasting(100)) // 100ms scroll timeout
            //         .takeScreenshot(driver);
            // System.out.println("  Screenshot captured.");
            // return screenshot.getImage();

            // *** Placeholder if AShot is not available ***
            System.err.println("    WARNING: AShot library not implemented/available. Returning null for screenshot.");
            return null; // Placeholder

        } catch (NoClassDefFoundError nc) {
            System.err.println("    ERROR: AShot library not found in classpath. Cannot take full page screenshot.");
            return null;
        } catch (Exception e) {
            System.err.println("    ERROR taking full page screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves the BufferedImage to a file.
     *
     * @param image    The image to save.
     * @param filename The full path and filename for the output PNG file.
     * @throws IOException If an error occurs during file writing.
     */
    private static void saveMarkedScreenshot(BufferedImage image, String filename) throws IOException {
        if (image == null) {
            System.err.println("Skipping screenshot save because the image is null. Filename: " + filename);
            return;
        }
        File outputFile = new File(filename);
        // Ensure parent directory exists
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Failed to create directory for screenshot: " + parentDir.getAbsolutePath());
                throw new IOException("Could not create parent directory for screenshot: " + parentDir.getAbsolutePath());
            }
        }
        try {
            System.out.println("  Saving screenshot to: " + outputFile.getAbsolutePath());
            ImageIO.write(image, "PNG", outputFile);
            System.out.println("  Screenshot saved successfully.");
        } catch (IOException e) {
            System.err.println("ERROR saving screenshot '" + filename + "': " + e.getMessage());
            throw e; // Re-throw to indicate failure
        }
    }


    /**
     * Gets the text content immediately preceding the given element using JavaScript.
     * This attempts to get text nodes before the element within its parent.
     *
     * @param element The WebElement (e.g., the <sup> tag).
     * @param js      JavascriptExecutor instance.
     * @return The preceding text, trimmed. Returns "[N/A]" on error or if no preceding text node.
     */
    private static String getPrecedingText(WebElement element, JavascriptExecutor js) {
        if (element == null || js == null) return "[N/A]";
        try {
            // JS to find the previous sibling text node
            Object result = js.executeScript(
                    "var elem = arguments[0];" +
                            "var parent = elem.parentNode;" +
                            "var previousNode = elem.previousSibling;" +
                            "var precedingText = '';" +
                            // Walk backwards through previous siblings that are text nodes
                            "while (previousNode) {" +
                            "  if (previousNode.nodeType === Node.TEXT_NODE) {" +
                            "    precedingText = previousNode.nodeValue + precedingText;" + // Prepend to handle multiple text nodes
                            "  } else if (previousNode.nodeType === Node.ELEMENT_NODE) {" +
                            // Optional: Stop if we hit another element, or get its text?
                            // For now, just stop at the first non-text node going backwards.
                            "    break;" +
                            "  }" +
                            "  previousNode = previousNode.previousSibling;" +
                            "}" +
                            "return precedingText.trim();", element); // Trim whitespace
            return Objects.toString(result, "[N/A]");
        } catch (StaleElementReferenceException se) {
            System.err.println("  Stale element reference getting preceding text for " + getElementIdentifierForLog(element));
            return "[STALE]";
        } catch (Exception e) {
            System.err.println("  Error getting preceding text for " + getElementIdentifierForLog(element) + ": " + e.getMessage().split("\n")[0]);
            return "[ERROR]";
        }
    }

    /**
     * Gets a meaningful identifier for a tab element (e.g., its text content).
     *
     * @param tabElement The WebElement representing the tab button.
     * @param index      The index of the tab (used as fallback).
     * @return A string identifier for the tab.
     */
    private static String getTabIdentifier(WebElement tabElement, int index) {
        if (tabElement == null) return "[Null Tab Element " + index + "]";
        try {
            String text = tabElement.getText();
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
            // Fallback using attributes if text is empty
            String ariaLabel = tabElement.getAttribute("aria-label");
            if (ariaLabel != null && !ariaLabel.trim().isEmpty()) {
                return ariaLabel.trim();
            }
            return "[Tab Index " + index + "]"; // Fallback to index
        } catch (StaleElementReferenceException se) {
            return "[Stale Tab " + index + "]";
        } catch (Exception e) {
            System.err.println("Error getting identifier for tab index " + index + ": " + e.getMessage());
            return "[Error Tab " + index + "]";
        }
    }

    /**
     * Finds the active panel associated with a clicked/active tab.
     * This requires understanding the HTML structure linking tabs and panels.
     * Common patterns: aria-controls, sibling relationships, data attributes.
     *
     * @param driver     WebDriver instance.
     * @param wait       WebDriverWait instance.
     * @param activeTab  The WebElement representing the currently active tab button.
     * @return The WebElement for the active panel, or null if not found/identifiable.
     */
    private static WebElement findActiveTabPanel(WebDriver driver, WebDriverWait wait, WebElement activeTab) {
        if (activeTab == null) return null;
        System.out.println("  Attempting to find active panel for tab: " + getElementIdentifierForLog(activeTab));

        // --- Strategy 1: aria-controls ---
        try {
            String panelId = activeTab.getAttribute("aria-controls");
            if (panelId != null && !panelId.trim().isEmpty()) {
                System.out.println("    Found aria-controls ID: " + panelId);
                try {
                    By panelLocator = By.id(panelId);
                    // Wait for the panel to be present and preferably visible
                    WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(panelLocator));
                    System.out.println("    Panel found and visible using ID: " + panelId);
                    return panel;
                } catch (TimeoutException | NoSuchElementException e) {
                    System.err.println("    Panel with ID '" + panelId + "' (from aria-controls) not found or not visible.");
                }
            } else {
                System.out.println("    Tab does not have aria-controls attribute.");
            }
        } catch (StaleElementReferenceException se) {
            System.err.println("    Stale element reference trying to read aria-controls from tab.");
            return null; // Cannot proceed if tab is stale
        } catch (Exception e) {
            System.err.println("    Error checking aria-controls: " + e.getMessage());
        }

        // --- Strategy 2: Sibling div with role='tabpanel' (Common pattern) ---
        // This assumes the panel is a sibling or near sibling. Adjust XPath as needed.
        try {
            System.out.println("    Trying sibling strategy: Looking for sibling div[@role='tabpanel']");
            // Example: Find the parent container, then find the panel within it.
            // This XPath is highly dependent on structure.
            // WebElement tabParent = activeTab.findElement(By.xpath("./..")); // Immediate parent
            // WebElement panel = tabParent.findElement(By.xpath("./following-sibling::div[@role='tabpanel'][1]"));

            // Simpler: Assume panel is *somewhere* after the tablist, often with matching aria-labelledby
            String tabId = activeTab.getAttribute("id");
            if (tabId != null && !tabId.isEmpty()) {
                By panelLocator = By.xpath("//div[@role='tabpanel' and @aria-labelledby='" + tabId + "']");
                System.out.println("    Trying locator based on aria-labelledby: " + panelLocator);
                try {
                    WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(panelLocator));
                    System.out.println("    Panel found and visible using aria-labelledby: " + tabId);
                    return panel;
                } catch (TimeoutException | NoSuchElementException e) {
                    System.err.println("    Panel linked by aria-labelledby='" + tabId + "' not found or not visible.");
                }
            } else {
                System.out.println("    Tab does not have an ID for aria-labelledby lookup.");
            }

        } catch (StaleElementReferenceException se) {
            System.err.println("    Stale element reference during sibling panel search.");
        } catch (Exception e) {
            System.err.println("    Error trying sibling/aria-labelledby strategy: " + e.getMessage());
        }

        // --- Fallback ---
        System.err.println("  Failed to find active tab panel using common strategies for tab: " + getElementIdentifierForLog(activeTab));
        return null; // Indicate failure
    }

    /**
     * Generates a user-friendly message based on the position check status.
     *
     * @param status  The PositionStatus enum value.
     * @param supText The text of the superscript (for context).
     * @return A descriptive string message.
     */
    private static String getPositionVerificationMessage(SuperscriptInfo.PositionStatus status, String supText) {
        if (status == null) return "Position status is null";
        switch (status) {
            case ABOVE_BASELINE:
                return "Passed: Superscript '" + supText + "' is positioned above the parent baseline.";
            case SAME_LINE:
                return "Failed: Superscript '" + supText + "' appears on the same line as parent (or below).";
            case CHECK_ERROR:
                return "Error: Could not determine position for superscript '" + supText + "' due to an error during check.";
            case NOT_APPLICABLE:
                return "N/A: Position check not applicable for superscript '" + supText + "' (e.g., element stale, hidden, or zero size).";
            default:
                return "Unknown position status for superscript '" + supText + "'.";
        }
    }

    /**
     * Finds elements using an explicit wait for presence.
     *
     * @param driver  WebDriver instance.
     * @param wait    WebDriverWait instance configured with desired timeout.
     * @param locator The By locator strategy.
     * @param timeout Max time to wait for elements to be present. (Note: wait object already has timeout)
     * @return List of WebElements found. Returns empty list if none found or timeout occurs.
     */
    private static List<WebElement> findElementsWithWait(WebDriver driver, WebDriverWait wait, By locator, Duration timeout) {
        // The timeout parameter here is redundant if the wait object is already configured.
        // We use the timeout from the 'wait' object passed in.
        try {
            // Wait for at least one element to be present
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            // Return all elements matching the locator now that at least one is present
            return driver.findElements(locator);
        } catch (TimeoutException e) {
            System.out.println("Timeout waiting for presence of element(s) using locator: " + locator);
            return Collections.emptyList(); // Return empty list on timeout
        } catch (Exception e) {
            System.err.println("Error finding elements with wait using locator " + locator + ": " + e.getMessage());
            return Collections.emptyList(); // Return empty list on other errors
        }
    }

    /**
     * Helper to get a useful identifier (ID, text, or tag) for logging WebElement details.
     * Handles potential nulls and exceptions gracefully.
     *
     * @param element The WebElement.
     * @return A string identifier for logging.
     */
    private static String getElementIdentifierForLog(WebElement element) {
        if (element == null) return "[NULL_ELEMENT]";
        try {
            String id = element.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                return "[ID:" + id + "]";
            }
            // Try getting text, but be cautious as it can be long or throw errors
            String text = "";
            try {
                text = element.getText();
            } catch (StaleElementReferenceException e) {
                return "[StaleElement]"; // Element became stale
            } catch (Exception e) {
                // Ignore other errors getting text for logging id
            }
            if (text != null && !text.trim().isEmpty()) {
                // Limit length for logging
                String trimmedText = text.trim().replaceAll("\\s+", " "); // Replace newlines/tabs
                return "[Text:" + trimmedText.substring(0, Math.min(trimmedText.length(), 20)) + "...]";
            }
            // Fallback to tag name
            return "[Tag:" + element.getTagName() + "]";
        } catch (StaleElementReferenceException e) {
            return "[StaleElement]"; // Element became stale during attribute/tag retrieval
        } catch (Exception e) {
            // Catch unexpected errors during identification
            return "[UnknownElementError:" + e.getClass().getSimpleName() + "]";
        }
    }

} // End of class SuperscriptProcessor