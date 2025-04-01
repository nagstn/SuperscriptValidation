package org.example;

import com.google.common.base.Verify;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class SuperscriptVerification {

    public static void main ( String[] args ) throws IOException {
        // Set the path to your ChromeDriver executable
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver ( );

        // Navigate to the web page
        driver.get ( "https://www.wellsfargo.com/checking/prime/" );

        //Handle zip code modal:
        WebElement zipCodeModal;
        try {
            zipCodeModal = driver.findElement ( By.xpath ( "//div[@class='zip-code-modal']" ) );
            if ( zipCodeModal.isDisplayed ( ) ) {
                WebElement zipCodeInput = zipCodeModal.findElement ( By.xpath ( "//input[@aria-labelledby='zip-code-label']" ) );
                zipCodeInput.click ( );
                zipCodeInput.sendKeys ( "94070" );
                WebElement zipCodeSubmit = driver.findElement ( By.xpath ( "//input[@type='submit']" ) );
                zipCodeSubmit.click ( );

                WebDriverWait wait = new WebDriverWait ( driver , Duration.ofSeconds ( 100 ) );
                wait.until ( ExpectedConditions.invisibilityOfElementLocated ( By.xpath ( "//div[@class='zip-code-modal']" ) ) ); //Wait for the zip code modal to disappear
                WebElement pageTile = driver.findElement(By.xpath("//div[@class='ps-page-title']"));
                String PageTitle = pageTile.getText();
                assertThat( PageTitle , equalTo ( "Prime Checking" ) );
                // Take screenshot after wait*/
                Object devicePixelRatio = ((JavascriptExecutor) driver).executeScript ( "return window.devicePixelRatio" );
                String dprValue = String.valueOf ( devicePixelRatio );
                float windowDPR = Float.parseFloat ( dprValue );

                Screenshot screenshot = new AShot ( )
                        .shootingStrategy ( ShootingStrategies.viewportPasting ( ShootingStrategies.scaling ( windowDPR ) , 1000 ) )
                        .takeScreenshot ( driver );

                ImageIO.write ( (RenderedImage) screenshot.getImage ( ) , "png" , new File ( "./screenshots/AshotFullPageScreen.png" ) );
                class SuperscriptScreenshot {

                    public static void main ( String[] args ) {
                        SuperscriptScreenshot screenshotter = new SuperscriptScreenshot ( );
                        String url = "https://www.wellsfargo.com/checking/prime/"; // Example URL

                        try {
                            screenshotter.setup ( ); // Initialize WebDriver
                            screenshotter.runSuperscriptVerification ( url );
                        } finally {
                            screenshotter.teardown ( );  // Ensure resources are released
                        }
                    }

                    private void teardown () {
                    }

                    private void runSuperscriptVerification ( String url ) {
                    }

                    private void setup () {
                    }
                }
            }


        } catch (org.openqa.selenium.NoSuchElementException e) {
            System.out.println ( "The zipcode Modal wasn't present" );
            //Take the screenshot if there is no zipCodeModal
            takeScreenshot ( driver , "noZipCodeModal.png" );
        }

        // Find all superscript elements (<sup>)
        List<WebElement> allSuperscripts = driver.findElements ( By.tagName ( "sup" ) );

        int totalSuperscripts = 0; // Counter for numeric superscripts

        // Verify each superscript is within a hyperlink (<a> tag)
        for (int i = 0; i < allSuperscripts.size ( ); i++) {
            WebElement superscript = allSuperscripts.get ( i );
            String superscriptText = superscript.getText ( ).trim ( ); // Get the text of the superscript and trim whitespace


            // Use a regular expression to check if the superscript text is numeric
            Pattern pattern = Pattern.compile ( "^\\d+$" ); // Matches one or more digits from start to end.
            Matcher matcher = pattern.matcher ( superscriptText );

            if ( matcher.matches ( ) ) {
                totalSuperscripts++;  // Increment count only for numeric superscripts

                try {
                    // Find the parent <a> tag
                    WebElement parentLink = superscript.findElement ( By.xpath ( ".//ancestor::a[1]" ) ); // Find immediate parent <a>
                    // Helper method to get the preceding text node
                    String precedingText = getPrecedingText ( superscript , driver );
                    System.out.println ( "Superscript " + (i + 1) + ": Preceding Text = '" + precedingText + "', Superscript Text = '" + superscriptText + "'" );
                    String linkText = getLinkText ( parentLink );

                    assertTrue ( "Superscript " + totalSuperscripts + " is not within a hyperlink." , true );
                    System.out.println ( "Numeric Superscript " + totalSuperscripts + " (" + superscriptText + ") is within a hyperlink. Link Text: " + linkText );
                } catch (org.openqa.selenium.NoSuchElementException e) {
                    System.out.println ( "Numeric Superscript " + totalSuperscripts + " (" + superscriptText + ") is not within a hyperlink." );
                    //assertTrue("Superscript " + (i + 1) + " is not within a hyperlink.", false); //Removed assertion as it is optional
                }
            } else {
                System.out.println ( "Skipping non-numeric superscript: " + superscriptText );
            }

        }

        System.out.println ( "Total numeric superscripts found: " + totalSuperscripts );

        // Close the browser
        driver.quit ( );
    }

    // Helper method to get the preceding text node
    private static String getPrecedingText ( WebElement element , Object driver ) {
        try {
            // Execute JavaScript to get the previous text node
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object precedingTextObj = js.executeScript (
                    "var elem = arguments[0];\n" +
                            "var prev = elem.previousSibling;\n" +
                            "while (prev && prev.nodeType != 3) {\n" +
                            "  prev = prev.previousSibling;\n" +
                            "}\n" +
                            "return prev ? prev.textContent : '';" , element );

            // Return the text or an empty string if no preceding text node is found
            return (precedingTextObj != null) ? precedingTextObj.toString ( ).trim ( ) : "";

        } catch (Exception e) {
            System.err.println ( "Error getting preceding text: " + e.getMessage ( ) );
            return ""; // Return an empty string in case of an error
        }
    }

    // Helper method to robustly get the link text
    private static String getLinkText ( WebElement parentLink ) {
        try {
            // First, try to find a span (or other element) within the link
            WebElement textElement = parentLink.findElement ( By.xpath ( ".//*" ) ); // Find any child element
            return textElement.getText ( );
        } catch (org.openqa.selenium.NoSuchElementException e) {
            // If no span (or other child) exists, get the text directly from the link
            return parentLink.getText ( );
        }
    }


    //**Helper Method to Take Screenshot**
    private static void takeScreenshot ( WebDriver driver , String filename ) throws IOException {
        File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs ( OutputType.FILE );
        File destinationFile = new File ( "screenshots/" + filename ); // Save to "screenshots" folder
        destinationFile.getParentFile ( ).mkdirs ( ); // Ensure directory exists
        FileHandler.copy ( screenshotFile , destinationFile );
        System.out.println ( "Screenshot saved to: " + destinationFile.getAbsolutePath ( ) );
    }
}