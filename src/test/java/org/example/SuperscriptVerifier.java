package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import java.time.Duration;
import java.util.ArrayList; // Import ArrayList
import java.util.List;

public class SuperscriptVerifier {

    public static void main(String[] args) {
        WebDriver driver = null; // Initialize outside try

        try {
            // --- Configuration ---
            System.setProperty("webdriver.chrome.driver", "C:\\Users\\nagar\\IdeaProjects\\DataValidationRates\\src\\main\\resources\\chromedriver.exe");
            driver = new ChromeDriver(); // Initialize driver here
            String testUrl = "file:///C:/Users/nagar/OneDrive/Desktop/superscript_test.html";
            // --- End Configuration ---

            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            driver.get(testUrl);

            // --- CORRECTED LOCATOR STRATEGY ---
            // Find ALL superscript elements with the specific class you want to test
            // This locator finds <sup> tags with class 'c20ref' anywhere on the page
            By superscriptLocator = new By.ByTagName ("sup");
            List<WebElement> superscriptElements = driver.findElements(superscriptLocator);
            // --- END LOCATOR STRATEGY ---

            System.out.println("Found " + superscriptElements.size() + " superscript elements with locator: " + superscriptLocator);

            if (superscriptElements.isEmpty()) {
                System.out.println("No superscript elements found to verify.");
            } else {
                // --- CORRECTED ITERATION ---
                // Create a list to store results if needed, or just print directly
                List<String> verificationResults = new ArrayList<>();
                System.out.println("\n--- Verification Results ---");

                // Loop through each found superscript element
                for (WebElement supElement : superscriptElements) {
                    // Call verifySuperscriptPosition for EACH element
                    String resultMessage = verifySingleSuperscriptPosition(supElement, driver);
                    System.out.println(resultMessage); // Print result for each element
                    verificationResults.add(resultMessage); // Optionally store results
                }
                System.out.println("--- End of Verification ---");
            }

        } catch (Exception e) { // Catch broader exceptions
            System.err.println("An error occurred during execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Verifies the position and content of a SINGLE superscript element.
     *
     * @param superscriptElement The WebElement representing the <sup> tag.
     * @param driver             The WebDriver instance (needed for context/JS execution if added later).
     * @return A message indicating the verification status for this element.
     */
    public static String verifySingleSuperscriptPosition(WebElement superscriptElement, WebDriver driver) {
        String identifier = getElementIdentifierForLog(superscriptElement); // Get ID or text for logging
        try {
            String superscriptText = superscriptElement.getText().trim();

            // 1. Check for empty tag (contains tag but no value)
            // Note: We already found the element, so the tag exists. Check if text is empty.
            if (superscriptText.isEmpty()) {
                // Find preceding text to give context to the empty tag
                String preceding = getPrecedingTextUsingSeleniumOnly(superscriptElement, driver); // Use helper
                return "INFO: Superscript element " + identifier + " found, but content (text/value) is missing. Preceding: '" + preceding + "'";
            }

            // --- Numeric Check (Optional but good based on previous examples) ---
            // Pattern numericPattern = Pattern.compile("^\\d+$");
            // if (!numericPattern.matcher(superscriptText).matches()) {
            //     return "INFO: Superscript " + identifier + " has non-numeric text: '" + superscriptText + "'. Skipping position check.";
            // }
            // --- End Numeric Check ---


            // 2. Find Parent Element
            WebElement parentElement;
            try {
                parentElement = superscriptElement.findElement(By.xpath(".//.."));
            } catch (NoSuchElementException e) {
                return "ERROR: Superscript " + identifier + " found, but could not determine its parent element.";
            }

            // 3. Get Position Info
            Point supLocation = superscriptElement.getLocation();
            Dimension supSize = superscriptElement.getSize();
            Point parentLocation = parentElement.getLocation();

            // Basic check if element is actually displayed and has size
            if (!superscriptElement.isDisplayed() || supSize.getHeight() == 0 || supSize.getWidth() == 0) {
                return "INFO: Superscript " + identifier + " found in DOM but is not displayed or has zero size. Skipping position check.";
            }


            int supY = supLocation.getY();
            int parentY = parentLocation.getY();

            // Debugging output
            // System.out.println("  Checking: " + identifier + " | Text: '" + superscriptText + "' | SupY: " + supY + " | ParentY: " + parentY + " | ParentTag: " + parentElement.getTagName());

            // 4. Determine Position (Compare Y coordinates)
            // Allow a small tolerance (e.g., 1-2 pixels) for rendering variations
            if (supY < (parentY - 3)) { // If superscript top is clearly above parent top
                return "PASS: Superscript " + identifier + " ('" + superscriptText + "') is displayed visually ABOVE the baseline.";
            } else {
                // Consider it on the same line if its top is not clearly above the parent's top
                return "FAIL: Superscript " + identifier + " ('" + superscriptText + "') is displayed on the SAME LINE as parent baseline (SupY=" + supY + ", ParentY=" + parentY + ").";
            }

        } catch (StaleElementReferenceException se) {
            // Handle cases where the element becomes stale during processing
            System.err.println("StaleElementReferenceException for " + identifier + ". Element might have been removed or changed.");
            return "ERROR: Stale element reference for " + identifier + ". Cannot verify.";
        } catch (Exception e) {
            // Catch other unexpected errors for this specific element
            System.err.println("Unexpected error verifying " + identifier + ": " + e.getMessage());
            // e.printStackTrace(); // Uncomment for full trace if needed
            return "ERROR: Exception verifying " + identifier + ": " + e.getMessage().split("\n")[0];
        }
    }

    /**
     * Helper to get a useful identifier (ID or first part of text) for logging.
     */
    private static String getElementIdentifierForLog(WebElement element) {
        try {
            String id = element.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                return "[ID:" + id + "]";
            }
            String text = element.getText();
            if (text != null && !text.isEmpty()) {
                return "[Text:" + text.substring(0, Math.min(text.length(), 10)) + "...]";
            }
            return "[Tag:" + element.getTagName() + "]"; // Fallback
        } catch (Exception e) {
            return "[UnknownElement]";
        }
    }

    /**
     * Limited Selenium-only preceding text finder (See previous explanation for limitations).
     * It's highly recommended to use the JavaScriptExecutor version for accuracy.
     */
    private static String getPrecedingTextUsingSeleniumOnly(WebElement element, WebDriver driver) {
        if (element == null) return "[Null Element]";
        try {
            WebElement precedingSiblingElement = element.findElement(By.xpath("./preceding-sibling::*[1]"));
            String text = precedingSiblingElement.getText();
            return (text != null) ? text.trim() : "";
        } catch (NoSuchElementException e) {
            return "[No Preceding Sibling Element]";
        } catch (Exception e) {
            return "[Error Getting Preceding]";
        }
    }

}