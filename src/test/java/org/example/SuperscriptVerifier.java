package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import java.time.Duration;

public class SuperscriptVerifier {

    public static void main(String[] args) {
        // --- Configuration ---
        WebDriver driver;
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\nagar\\IdeaProjects\\DataValidationRates\\src\\main\\resources\\chromedriver.exe"); // set correct path
        driver = new ChromeDriver();
        String testUrl = "file:///C:/Users/nagar/OneDrive/Desktop/superscript_test.html"; // Replace with the URL of your test page
        // --- End Configuration ---

        driver = null;
        try {
            driver = new ChromeDriver();
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5)); // Implicit wait

            driver.get(testUrl);

            // --- LOCATOR STRATEGY ---
            // **IMPORTANT:** You MUST provide a specific locator for the superscript
            // element you want to test. Choose ONE of the following examples
            // or create your own based on the HTML structure.

            // Example 1: Find a specific superscript by its ID
            // By superscriptLocator = By.id("mySuperscriptId");

            // Example 2: Find a superscript within a specific paragraph containing known text
            By superscriptLocator = By.xpath("//*[@id='line3']/sup" ); // Adjust 'Some preceding text'
//*[@id="line1"]/sup
            // Example 3: Find the first superscript on the page (less reliable if multiple exist)
            // By superscriptLocator = By.tagName("sup");
//*[@id="openAcct"]/div[2]/label/strong[3]/text()[1]
            // Example 4: Find a superscript immediately following a specific span
            // By superscriptLocator = By.xpath("//span[@id='specificTextSpan']/following-sibling::sup[1]");
            // --- END LOCATOR STRATEGY ---


            String verificationMessage = verifySuperscriptPosition(driver, superscriptLocator);
            System.out.println("Verification Result: " + verificationMessage);


        } catch (NoSuchElementException nsee) {
            System.err.println("Error: Could not find the specified superscript element using the provided locator.");
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Verifies the position and content of a superscript element.
     *
     * @param driver             The WebDriver instance.
     * @param superscriptLocator The By locator strategy to find the specific superscript element.
     * @return A message indicating the verification status.
     */
    public static String verifySuperscriptPosition(WebDriver driver, By superscriptLocator) {
        try {
            WebElement supElement = driver.findElement(superscriptLocator);
            String supText = supElement.getText().trim();

            // 1. Check if the superscript tag exists but the number/content is missing
            if (supText.isEmpty()) {
                return "Superscript tag found, but superscript content (number/text) is missing.";
            }

            // 2. Find the immediate parent element for position comparison
            // Using XPath "./.." is a common way to get the parent
            WebElement parentElement;
            try {
                parentElement = supElement.findElement(By.xpath("./.."));
                if (parentElement.getTagName().equalsIgnoreCase("body") || parentElement.getTagName().equalsIgnoreCase("html")) {
                    System.out.println("Warning: Parent element is high up the DOM ('" + parentElement.getTagName() + "'). Positional check might be less meaningful.");
                    // Optionally, you could try finding a preceding sibling text node or element if needed,
                    // but comparing with the immediate parent is often sufficient.
                }
            } catch (NoSuchElementException e) {
                return "Superscript found, but could not determine its parent element for position check.";
            }


            // 3. Get position and size information
            Point supLocation = supElement.getLocation();
            Dimension supSize = supElement.getSize();
            Point parentLocation = parentElement.getLocation();
            // Dimension parentSize = parentElement.getSize(); // Parent size might not be needed directly

            int supY = supLocation.getY();
            int supBottomY = supY + supSize.getHeight();
            int parentY = parentLocation.getY();

            // Debugging output (optional)
            System.out.println("Superscript Text: '" + supText + "'");
            System.out.println("Superscript Y: " + supY + ", Height: " + supSize.getHeight() + ", Bottom Y: " + supBottomY);
            System.out.println("Parent Tag: <" + parentElement.getTagName() + ">, Parent Y: " + parentY);

            // 4. Determine position
            // Condition for "Above": The superscript's top Y-coordinate should be less than
            // the parent's top Y-coordinate. Rendering engines place `<sup>` higher.
            // Allow for a small tolerance if needed, but often `supY < parentY` is enough.
            if (supY < parentY) {
                // This is the expected rendering for a superscript visually above the baseline
                return "SUCCESS: Superscript '" + supText + "' is displayed visually above the baseline of the parent text.";
            } else {
                // Condition for "Same Line": If the superscript's top Y is not less than
                // the parent's top Y, it's effectively rendered on the same line or lower.
                return "ISSUE: Superscript '" + supText + "' is displayed on the same line as the preceding/parent text (Y=" + supY + ", ParentY=" + parentY + ").";
                // We can add more checks here if needed, e.g., compare supBottomY with parentBottomY
                // but supY >= parentY usually indicates it's not positioned *above*.
            }

        } catch (NoSuchElementException e) {
            // This catch is technically redundant if the main method catches it,
            // but good practice within the helper function too.
            return "Superscript element not found using locator: " + superscriptLocator;
        }
    }
}
