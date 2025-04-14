package org.myPack;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;

import java.util.Date;



// Assuming SuperscriptInfo and VerificationSummary classes/inner classes are defined as above

public class TabSuperscriptCounter {
// Add these inner classes (or separate classes) within or alongside TabSuperscriptCounter

    /**
     * Stores detailed information about a single processed numeric superscript.
     */
    static class SuperscriptInfo {
        String tabIdentifier;
        String superscriptText;
        String precedingText;
        boolean isHyperlink;
        String linkHref;
        PositionStatus positionStatus; // Enum to store position check result
        String verificationMessage; // Detailed message about position

        // Enum for position status
        enum PositionStatus {
            ABOVE_BASELINE,      // Standard superscript, displayed above (PASS condition for position)
            SAME_LINE,           // Styled or rendered on the same baseline (FAIL condition for position)
            EMPTY_TAG,           // Tag exists but has no text content (INFO)
            POSITION_CHECK_ERROR,// Error occurred during the position check
            NOT_APPLICABLE,      // E.g., Element was stale before check
            CHECK_ERROR, ELEMENT_ERROR        // Error finding/accessing the element initially
        }

        // Constructor or setters can be added as needed
        @Override
        public String toString() {
            return "SuperscriptInfo{" +
                    "tab='" + tabIdentifier + '\'' +
                    ", text='" + superscriptText + '\'' +
                    ", preceding='" + precedingText + '\'' +
                    ", isLink=" + isHyperlink +
                    ", href='" + linkHref + '\'' +
                    ", position=" + positionStatus +
                    ", msg='" + verificationMessage + '\'' +
                    '}';
        }
    }

    /**
     * Stores summary statistics for superscript verification within a single tab.
     */
    /*public static class VerificationSummary {
        int totalSuperscriptsFoundInPanel = 0;
        int numericSuperscriptsProcessed = 0;
        int positionCheckPassed = 0; // Count for ABOVE_BASELINE
        int positionCheckFailed = 0; // Count for SAME_LINE
        int positionCheckErrors = 0; // Count for CHECK_ERROR/NOT_APPLICABLE
        int hyperlinkCount = 0;
        int noHyperlinkCount = 0;
        List<String> emptySuperscriptLogs = new ArrayList<>();
        List<String> nonNumericSuperscriptLogs = new ArrayList<>();

        @Override
        public String toString() {
            return "Summary{" +
                    "totalFound=" + totalSuperscriptsFoundInPanel +
                    ", numericProcessed=" + numericSuperscriptsProcessed +
                    ", posPassed=" + positionCheckPassed +
                    ", posFailed=" + positionCheckFailed +
                    ", posErrors=" + positionCheckErrors +
                    ", links=" + hyperlinkCount +
                    ", noLinks=" + noHyperlinkCount +
                    ", emptyTags=" + emptySuperscriptLogs.size() +
                    ", nonNumericTags=" + nonNumericSuperscriptLogs.size() +
                    '}';
        }
    }*/
    public static void main ( String[] args ) {
        WebDriver driver = null;
        List<SuperscriptInfo> allSuperscriptDetails = new ArrayList<> ( );
        Map<String, SuperscriptStepDefinitions.VerificationSummary> tabSummaries = new LinkedHashMap<> ( );

        try {
            System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\DataValidationRates\\src\\main\\resources\\chromedriver.exe" ); // set correct path
            driver = new ChromeDriver ( );
            driver.manage ( ).window ( ).maximize ( );
            WebDriverWait wait = new WebDriverWait ( driver , Duration.ofSeconds ( 15 ) );

            // --- CONFIGURATION Based on Wells Fargo HTML ---
              //String url = "https://www.wellsfargo.com/savings-cds/";
           String url="file:///C:/Users/nagar/OneDrive/Desktop/superscript_react_test.html";
           // String url="https://www.wellsfargo.com/savings-cds/";
            By tabContainerLocator = By.xpath ( "//div[@role='tablist' and contains(@class, 'table-tab-list')]" );
            By tabLocator = By.xpath ( ".//button[@role='tab']" );
            String activePanelIndicatorAttribute = "aria-selected";
            String activePanelIndicatorValue = "true";
            By superscriptLocator = By.tagName ( "sup" );//sup[@class='c20ref']"" );

            // --- END CONFIGURATION ---

            driver.get ( url );
            //public static void  takeFullPageScreenshotWithAShot(driver);
            WebElement tabContainer = wait.until ( ExpectedConditions.visibilityOfElementLocated ( tabContainerLocator ) );
            List<WebElement> initialTabs = tabContainer.findElements ( tabLocator );
            int tabCount = initialTabs.size ( );
            System.out.println ( "Found " + tabCount + " tab elements initially." );

            if ( tabCount == 0 ) { /* Handle no tabs found */
                return;
            }

            List<String> visitedTabIdentifiers = new ArrayList<> ( ); // Can likely remove if index loop is robust

            // --- Iterate using index ---
            for (int i = 0; i < tabCount; i++) {
                WebElement currentTab = null;
                String tabIdentifier = "[Unknown Tab " + i + "]";
                SuperscriptStepDefinitions.VerificationSummary currentTabSummary = new SuperscriptStepDefinitions.VerificationSummary ( ); // Summary for this tab

                try {
                    // ** Re-find the tabs list and get the current tab by index **
                    // Optional: Wait briefly for container visibility again if needed
                    // tabContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(tabContainerLocator));
                    List<WebElement> currentTabsList = driver.findElements ( tabLocator ); // Re-find inside loop

                    if ( i >= currentTabsList.size ( ) ) {
                        System.err.println ( "Tab list size changed or index out of bounds, skipping index " + i );
                        continue;
                    }
                    currentTab = currentTabsList.get ( i );

                    tabIdentifier = getTabIdentifier ( currentTab , i );

                    // Initialize summary map entry for this tab if not already present
                    // (Handles case where identifier might change slightly on reload)
                    tabSummaries.putIfAbsent ( tabIdentifier , currentTabSummary );
                    // Get the potentially existing summary if identifier was already seen
                    currentTabSummary = tabSummaries.get ( tabIdentifier );


                    System.out.println ( "\nProcessing Tab (" + (i + 1) + "/" + tabCount + "): " + tabIdentifier );

                    // --- Click the Tab ONLY IF NEEDED ---
                    String selectedAttributeValue = currentTab.getAttribute ( activePanelIndicatorAttribute );
                    boolean needsClick = ! activePanelIndicatorValue.equalsIgnoreCase ( selectedAttributeValue );

                    if ( needsClick ) {
                        System.out.println ( "Clicking tab: " + tabIdentifier );
                        try {
                            // --- Click Logic (Scroll, Click, Wait for selection) ---
                            ((JavascriptExecutor) driver).executeScript ( "arguments[0].scrollIntoView({block: 'center'});" , currentTab );
                            Thread.sleep ( 200 ); // Pause after scroll
                            wait.until ( ExpectedConditions.elementToBeClickable ( currentTab ) ).click ( );
                        } catch (ElementClickInterceptedException e) {
                            System.out.println ( "Click intercepted, trying JS click for tab: " + tabIdentifier );
                            ((JavascriptExecutor) driver).executeScript ( "arguments[0].click();" , currentTab );
                        }
                        // Wait for the tab to *become* selected
                        wait.until ( ExpectedConditions.attributeToBe ( currentTab , activePanelIndicatorAttribute , activePanelIndicatorValue ) );
                        System.out.println ( "Tab click successful, waiting for content..." );
                        Thread.sleep ( 1000 ); // Pause for content load animation/JS
                    } else {
                        System.out.println ( "Tab already selected: " + tabIdentifier );
                        // ** Even if selected, still need to wait for panel / process content **
                        Thread.sleep ( 700 ); // Small pause even if already selected
                    }

                    // --- Find the Active Content Panel ---
                    // We always try to find the panel, whether we clicked or it was pre-selected
                    // We always try to find the panel, whether we clicked or it was pre-selected
                    WebElement activePanel = findActiveTabPanel ( driver , wait , currentTab ); // Use wait with default timeout

                    if ( activePanel == null ) {
                        System.err.println ( "Could not find/verify the active content panel for tab: " + tabIdentifier + ". Skipping superscript count." );
                        // Update summary to reflect error
                        currentTabSummary.positionCheckErrors++; // Or a specific flag
                        continue; // Skip to next tab
                    }
                    System.out.println ( "Active panel identified/verified for tab: " + tabIdentifier );

                    // --- Count Numeric Superscripts within the Active Panel ---
                    int numericCount = 0;
                    List<WebElement> superscriptsInPanel = new ArrayList<> ( ); // Initialize empty list
                    try {
                        // *** ADD EXPLICIT WAIT FOR SUPERSCRIPTS INSIDE THE PANEL ***
                        System.out.println ( "Waiting for superscripts (class='c20ref') to be present within the active panel..." );
                        WebDriverWait panelWait = new WebDriverWait ( driver , Duration.ofSeconds ( 5 ) ); // Shorter wait for content within panel
                        try {
                            // Wait for PRESENCE of at least one matching element nested within the panel
                            panelWait.until ( ExpectedConditions.presenceOfNestedElementLocatedBy ( activePanel , superscriptLocator ) );
                            System.out.println ( "Superscripts appear to be present in the panel." );
                            // Now find all of them
                            superscriptsInPanel = activePanel.findElements ( superscriptLocator );
                        } catch (TimeoutException te) {
                            // It's okay if no superscripts are found after waiting, means the count is 0 for this panel
                            System.out.println ( "No superscripts (class='c20ref') found in the active panel within the timeout." );
                            // superscriptsInPanel will remain empty, which is correct.
                        }
                        // *** END OF ADDED WAIT ***


                        currentTabSummary.totalSuperscriptsFoundInPanel = superscriptsInPanel.size ( ); // Update total found
                        System.out.println ( "Found " + superscriptsInPanel.size ( ) + " potential superscripts (class='c20ref') in the active panel." );

                        Pattern numericPattern = Pattern.compile ( "^\\d+$" );

                        // --- Loop through found superscripts (if any) ---
                        for (WebElement sup : superscriptsInPanel) {
                            // ... (The rest of your existing processing logic for each sup:
                            //      getText, check empty, check numeric, check position, check link,
                            //      update summary, add details to allSuperscriptDetails) ...

                            String supText = "[Error]";
                            boolean isEmpty = false;
                            boolean isNumeric = false;

                            try {
                                supText = sup.getText ( ).trim ( );
                                isEmpty = supText.isEmpty ( );
                                if ( ! isEmpty ) {
                                    isNumeric = numericPattern.matcher ( supText ).matches ( );
                                }

                                if ( isEmpty ) {
                                    currentTabSummary.emptySuperscriptLogs.add ( "Empty tag near: " + getPrecedingText ( sup , driver ) );
                                    continue;
                                }

                                if ( isNumeric ) {
                                    currentTabSummary.numericSuperscriptsProcessed++;
                                    SuperscriptInfo info = new SuperscriptInfo ( );
                                    // ... populate info ...
                                    info.tabIdentifier = tabIdentifier;
                                    info.superscriptText = supText;
                                    info.precedingText = getPrecedingText ( sup , driver );

                                    // Position Check
                                    SuperscriptInfo.PositionStatus status = checkSuperscriptPosition ( sup , driver );
                                    info.positionStatus = status;
                                    info.verificationMessage = getPositionVerificationMessage ( status , supText );
                                    if ( status == SuperscriptInfo.PositionStatus.ABOVE_BASELINE )
                                        currentTabSummary.positionCheckPassed++;
                                    else if ( status == SuperscriptInfo.PositionStatus.SAME_LINE )
                                        currentTabSummary.positionCheckFailed++;
                                    else currentTabSummary.positionCheckErrors++;

                                    // Hyperlink Check
                                    try {
                                        WebElement parentLink = sup.findElement ( By.xpath ( ".//ancestor::a[1]" ) );
                                        String href = parentLink.getAttribute ( "href" );
                                        info.isHyperlink = (href != null && ! href.trim ( ).isEmpty ( ) && ! href.toLowerCase ( ).startsWith ( "javascript:" ));
                                        info.linkHref = href != null ? href : "N/A (Empty href)";
                                        if ( info.isHyperlink ) currentTabSummary.hyperlinkCount++;
                                        else currentTabSummary.noHyperlinkCount++;
                                    } catch (NoSuchElementException | StaleElementReferenceException eLink) {
                                        info.isHyperlink = false;
                                        info.linkHref = "NO HYPERLINK";
                                        currentTabSummary.noHyperlinkCount++;
                                    }

                                    allSuperscriptDetails.add ( info );

                                } else {
                                    currentTabSummary.nonNumericSuperscriptLogs.add ( supText );
                                }

                            } catch (StaleElementReferenceException staleSup) { /* handle */
                                currentTabSummary.positionCheckErrors++;
                            } catch (Exception supEx) { /* handle */
                                currentTabSummary.positionCheckErrors++;
                            }
                        } // End loop through superscripts in panel

                    } catch (Exception findSupErr) { // Catch errors during the waiting/finding process
                        System.err.println ( "Error waiting for or finding superscripts within panel for tab " + tabIdentifier + ": " + findSupErr.getMessage ( ) );
                        // Update summary to reflect error state?
                    }

                    System.out.println ( "Finished processing panel for tab '" + tabIdentifier + "'. Numeric Processed: " + currentTabSummary.numericSuperscriptsProcessed );
                    // Don't mark visited here, let the loop index handle progress

                } catch (StaleElementReferenceException staleTabEx) { /* Handle stale tab */ } catch (
                        TimeoutException timeoutEx) { /* Handle timeout */ } catch (
                        Exception e) { /* Handle other exceptions */ }
            } // End of loop through tabs

            // --- Final Report ---
            // ... (same reporting logic as before) ...
            System.out.println ( "\n--- Verification Summary Per Tab ---" );
            for (Map.Entry<String, SuperscriptStepDefinitions.VerificationSummary> entry : tabSummaries.entrySet ( )) {
                System.out.println ( "Tab: " + entry.getKey ( ) + " -> " + entry.getValue ( ) );
            }

            System.out.println ( "\n--- Detailed Superscript Info (Numeric Only) ---" );
            if ( allSuperscriptDetails.isEmpty ( ) ) {
                System.out.println ( "No numeric superscripts processed." );
            } else {
                allSuperscriptDetails.forEach ( System.out::println );
            }
        } catch (Exception e) {
            System.err.println ( "A major error occurred during Selenium processing: " + e.getMessage ( ) );
            e.printStackTrace ( );
        } finally {
            // --- EXCEL WRITING LOGIC ---
            System.out.println ( "\n--- Preparing to write Excel file ---" );
            if ( allSuperscriptDetails.isEmpty ( ) ) {
                System.out.println ( "No detailed numeric superscript data was collected to write to Excel." );
            } else {
                Workbook workbook = new XSSFWorkbook ( ); // Create a new workbook for export
                Sheet detailSheet = workbook.createSheet ( "SuperscriptDetail" );
                int rowIndex = 0;

                // Create Header Row
                Row headerRow = detailSheet.createRow ( rowIndex++ );
                String[] headers = {"Tab/Page Identifier" , "Superscript Text" , "Preceding Text" , "Is Hyperlink" , "Link Href" , "Position Status" , "Position Result Msg"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell ( i );
                    cell.setCellValue ( headers[i] );
                }

                // Populate Data Rows from the collected list
                System.out.println ( "Writing " + allSuperscriptDetails.size ( ) + " detail records to Excel..." );
                for (SuperscriptInfo info : allSuperscriptDetails) {
                    Row dataRow = detailSheet.createRow ( rowIndex++ );
                    int cellNum = 0;
                    // Handle potential null values from the info object gracefully
                    dataRow.createCell ( cellNum++ ).setCellValue ( info.tabIdentifier != null ? info.tabIdentifier : "N/A" );
                    dataRow.createCell ( cellNum++ ).setCellValue ( info.superscriptText != null ? info.superscriptText : "N/A" );
                    dataRow.createCell ( cellNum++ ).setCellValue ( info.precedingText != null ? info.precedingText : "N/A" );
                    dataRow.createCell ( cellNum++ ).setCellValue ( info.isHyperlink ? "YES" : "NO" );
                    dataRow.createCell ( cellNum++ ).setCellValue ( info.linkHref != null ? info.linkHref : "N/A" );
                    dataRow.createCell ( cellNum++ ).setCellValue ( info.positionStatus != null ? info.positionStatus.name ( ) : "N/A" );
                    dataRow.createCell ( cellNum++ ).setCellValue ( info.verificationMessage != null ? info.verificationMessage : "N/A" );
                }

                // Auto-size columns for better readability (optional)
                for (int i = 0; i < headers.length; i++) {
                    detailSheet.autoSizeColumn ( i );
                }

                // Write the workbook to a file
                String timestamp = new SimpleDateFormat ( "yyyyMMdd_HHmmss" ).format ( new Date ( ) );
                String filename = "superscript_details_" + timestamp + ".xlsx";
                java.io.File outputFile = new java.io.File(filename);//Write to project root, or specify a path e.g., "output/" + filename

                try (FileOutputStream fileOut = new FileOutputStream ( outputFile )) {
                    workbook.write ( fileOut );
                    System.out.println ( "----> Excel file successfully written to: " + outputFile.getAbsolutePath ( ) );
                } catch (IOException e) {
                    System.err.println ( "ERROR writing Excel file '" + filename + "': " + e.getMessage ( ) );
                    e.printStackTrace ( );
                } finally {
                    // Close the workbook to release resources
                    try {
                        workbook.close ( );
                        if ( driver != null ) driver.quit ( );
                    } catch (IOException e) {
                        System.err.println ( "Error closing workbook: " + e.getMessage ( ) );
                    }
                }
            }
        }
    }
    // --- Helper Methods ---
    /**
     * Helper method to get a meaningful identifier for a tab element.
     * Uses ID, aria-label, or text content.
     */
    public static String getTabIdentifier ( WebElement tabElement , int index ) {
        // Prioritize ID as it's likely unique
        try {
            String id = tabElement.getAttribute("id");
            if (id != null && !id.isEmpty()) return "ID:" + id; // Use ID directly

            String label = tabElement.getAttribute("aria-label"); // Check aria-label next
            if (label != null && !label.isEmpty()) return "Label:" + label;

            // Use text content within the button (specifically the link text if present)
            try {
                WebElement link = tabElement.findElement(By.tagName("a"));
                String text = link.getText();
                if (text != null && !text.trim().isEmpty()) return "Text:" + text.trim();
            } catch (NoSuchElementException e) {
                // If no link, try button's direct text
                String text = tabElement.getText();
                if (text != null && !text.trim().isEmpty()) return "Text:" + text.trim();
            }

            return "Index:" + index; // Fallback if no other identifier found
        } catch (StaleElementReferenceException e) {
            return "[Stale Tab Index:" + index + "]";
        } catch (Exception e) {
            return "[Error Getting Tab ID Index:" + index + "]";
        }
    }

    /**
     * Helper method to find the currently active tab panel based on the clicked tab.
     * Uses the 'aria-controls' attribute strategy for Wells Fargo page.
     */
    public static WebElement findActiveTabPanel ( WebDriver driver , WebDriverWait wait , WebElement clickedTab ) {
        // --- STRATEGY: Panel ID derived from Tab's aria-controls (Matches Wells Fargo HTML) ---
        String panelId = null;
        try {
            panelId = clickedTab.getAttribute("aria-controls");
            if (panelId != null && !panelId.isEmpty()) {
                System.out.println("Attempting to find panel by aria-controls ID: #" + panelId);
                // Wait for visibility specifically
                return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(panelId)));
            } else {
                System.err.println("Tab '" + getTabIdentifier(clickedTab, -1) + "' does not have a valid 'aria-controls' attribute.");
                return null;
            }
        } catch (StaleElementReferenceException e) {
            System.err.println("StaleElementReferenceException trying to get aria-controls from clicked tab. Cannot find panel.");
            return null;
        } catch (TimeoutException e) {
            System.err.println("Timed out waiting for panel with ID: #" + panelId + " (controlled by " + getTabIdentifier(clickedTab, -1) + ")");
            return null;
        } catch (Exception e) {
            System.err.println("Error finding panel using aria-controls (ID: " + panelId + "): " + e.getMessage());
            return null;
        }
    }
    // getPrecedingText remains the same (using robust JS)
    public static String getPrecedingText ( WebElement element , WebDriver driver ) {
        try {
            // Execute JavaScript to get the previous text node
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object precedingTextObj = js.executeScript (
                    """
                            var elem = arguments[0];
                            var prev = elem.previousSibling;
                            while (prev && prev.nodeType != 3) {
                              prev = prev.previousSibling;
                            }
                            return prev ? prev.textContent : '';""" , element );

            // Return the text or an empty string if no preceding text node is found
            return (precedingTextObj != null) ? precedingTextObj.toString ( ).trim ( ) : "";

        } catch (Exception e) {
            System.err.println ( "Error getting preceding text: " + e.getMessage ( ) );
            return ""; // Return an empty string in case of an error
        }
    }

    /**
     * Checks the vertical position of a superscript relative to its preceding sibling or parent.
     * Uses JavaScript for accuracy across different rendering scenarios.
     *
     * @param supElement The superscript WebElement.
     * @param driver     The WebDriver instance.
     * @return PositionStatus indicating if it's above or on the same line.
     */
     /*
     * Checks the vertical position of a superscript relative to its preceding sibling or parent.
     * Uses JavaScript for accuracy across different rendering scenarios.
     * FOCUSES ON VERTICAL EXTENT RATHER THAN JUST TOP COMPARISON.
     *
     * @param supElement The superscript WebElement.
     * @param driver     The WebDriver instance.
     * @return PositionStatus indicating if it's above or on the same line.
     */
       /**
     * Checks the vertical position of a superscript by directly querying its
     * computed 'vertical-align' CSS style using JavaScript. This is generally
     * more reliable for detecting baseline alignment than coordinate math.
     *
     * @param supElement The superscript WebElement.
     * @param driver     The WebDriver instance.
     * @return PositionStatus indicating if it's above or on the same line (baseline).
     */
       static SuperscriptInfo.PositionStatus checkSuperscriptPosition ( WebElement supElement , WebDriver driver ) {
           if ( supElement == null ) return SuperscriptInfo.PositionStatus.CHECK_ERROR;
           try {
           /* JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                    "var elem = arguments[0]; if (!elem) return 'CHECK_ERROR: Element is null';" +
                            "try { var style = window.getComputedStyle(elem); return style.getPropertyValue('vertical-align'); }" +
                            "catch (e) { return 'CHECK_ERROR: JS Error - ' + e.message; }", supElement);

*/
               Point supLocation = supElement.getLocation ( );
               Dimension supSize = supElement.getSize ( );
               Point parentLocation = supElement.getLocation ( );
               // Dimension parentSize = parentElement.getSize(); // Parent size might not be needed directly

               int supY = supLocation.getY ( );
               int supBottomY = supY + supSize.getHeight ( );
               int parentY = parentLocation.getY ( );

               // Debugging output (optional)
               System.out.println ( "Superscript Text: '" + supElement + "'" );
               System.out.println ( "Superscript Y: " + supY + ", Height: " + supSize.getHeight ( ) + ", Bottom Y: " + supBottomY );
               System.out.println ( "Parent Tag: <" + supElement.getTagName ( ) + ">, Parent Y: " + parentY );

               // 4. Determine position
               // Condition for "Above": The superscript's top Y-coordinate should be less than
               // the parent's top Y-coordinate. Rendering engines place `<sup>` higher.
               // Allow for a small tolerance if needed, but often `supY < parentY` is enough.
               if ( supY < parentY - 3 ) {
                   // This is the expected rendering for a superscript visually above the baseline
                   return SuperscriptInfo.PositionStatus.ABOVE_BASELINE;
               } else {
                   // Condition for "Same Line": If the superscript's top Y is not less than
                   // the parent's top Y, it's effectively rendered on the same line or lower.
                   return SuperscriptInfo.PositionStatus.SAME_LINE;
                   //return "ISSUE: Superscript '" + supElement + "' is displayed on the same line as the preceding/parent text (Y=" + supY + ", ParentY=" + parentY + ").";
                   // We can add more checks here if needed, e.g., compare supBottomY with parentBottomY
                   // but supY >= parentY usually indicates it's not positioned *above*.
               }
           } catch (NoSuchElementException e) {
               // This catch is technically redundant if the main method catches it,
               // but good practice within the helper function too.
               return SuperscriptInfo.PositionStatus.CHECK_ERROR;
               //return ("Superscript element not found using locator: " + supElement);
           }
       }
    /**
     * Generates a human-readable message based on the position check status.
     */
    public static String getPositionVerificationMessage ( SuperscriptInfo.PositionStatus status , String supText ) {
        switch (status) {
            case ABOVE_BASELINE:
                return "PASS: Superscript '" + supText + "' is displayed above the baseline.";
            case SAME_LINE:
                return "FAIL: Superscript '" + supText + "' is displayed on the SAME LINE as preceding text baseline.";
            case CHECK_ERROR:
                return "ERROR: Could not reliably determine position for superscript '" + supText + "'.";
            case NOT_APPLICABLE:
                return "N/A: Position check skipped for '" + supText + "' (e.g., due to stale element).";
            default:
                return "Unknown position status for '" + supText + "'.";
        }
    }

} // End of class
