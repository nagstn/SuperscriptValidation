package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.Point;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FinalSuperscriptScreenshot {

    public static void main ( String[] args ) throws IOException {
        // Set the path to your ChromeDriver executable
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver ( );

        // Navigate to the web page
        driver.get ( "https://www.wellsfargo.com/checking/prime/" );

        try {
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
                    wait.until ( ExpectedConditions.invisibilityOfElementLocated ( By.xpath ( "//div[@class='zip-code-modal']" ) ) );

                    // Take the full page screenshot and markups
                    takeFullPageScreenshotWithMarkups ( driver , "superscripts_marked.png" );
                }

            } catch (org.openqa.selenium.NoSuchElementException e) {
                System.out.println ( "The zipcode Modal wasn't present" );
                // Take full page screenshot directly if there is no zip code modal
                takeFullPageScreenshotWithMarkups ( driver , "superscripts_marked.png" );
            }
        } finally {
            // Close the browser
            driver.quit ( );
        }
    }

    //**Helper Method to Take Full Page Screenshot with Superscript Markups**
    private static void takeFullPageScreenshotWithMarkups ( WebDriver driver , String filename ) throws IOException {
        // Get entire page height
        long fullPageHeight = (long) ((JavascriptExecutor) driver).executeScript ( "return document.body.scrollHeight" );
        System.out.println ( fullPageHeight );

        // Set initial window height and create combined image
        ((JavascriptExecutor) driver).executeScript ( "window.scrollTo(0, 0)" );
        Dimension initialDim = driver.manage ( ).window ( ).getSize ( );

        List<BufferedImage> bufferedImages = new ArrayList<> ( );
        double scrollHeight = 0;

        do {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs ( OutputType.FILE );
            BufferedImage bufferedImage = ImageIO.read ( screenshotFile );
            bufferedImages.add ( bufferedImage );

            scrollHeight += bufferedImage.getHeight ( );
            ((JavascriptExecutor) driver).executeScript ( "window.scrollTo(0, " + scrollHeight + ")" );
            try {
                Thread.sleep ( 200 );
            } catch (InterruptedException e) {
                e.printStackTrace ( );
            }
        } while (scrollHeight < fullPageHeight);

        driver.manage ( ).window ( ).setSize ( initialDim );
        BufferedImage combinedImage = new BufferedImage ( bufferedImages.get ( 0 ).getWidth ( ) , (int) fullPageHeight , BufferedImage.TYPE_INT_RGB );

        Graphics2D graphics = combinedImage.createGraphics ( );
       // graphics.setColor(Color.WHITE);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, combinedImage.getWidth(), combinedImage.getHeight());
        graphics.setColor(Color.BLACK);

        //Reset the scrollHeight to be zero
        scrollHeight = 0;

        //Loop the bufferedImages, paste each of them into result BufferedImage from the scroll height
        for (BufferedImage image : bufferedImages) {
            graphics.drawImage(image, 0, (int) scrollHeight, null);
            scrollHeight += image.getHeight();
        }
        // Find all superscript elements (<sup>)
        List<WebElement> superscripts = driver.findElements ( By.tagName ( "sup" ) );
        graphics.setColor(Color.GREEN);
        graphics.setStroke(new BasicStroke(3));
        for (WebElement superscript : superscripts) {
            String superscriptText = superscript.getText ( ).trim ( );

            Pattern pattern = Pattern.compile ( "^\\d+$" );
            Matcher matcher = pattern.matcher ( superscriptText );

            if ( matcher.matches ( ) ) {
                try {
                    WebElement parentLink = superscript.findElement ( By.xpath ( ".//ancestor::a[1]" ) );

                    //It is an hyperlink

                    Point location = superscript.getLocation ( );
                    Dimension size = superscript.getSize ( );

                    int x = location.getX ( );
                    int y = location.getY ( );
                    int width = size.getWidth ( );
                    int height = size.getHeight ( );

                    graphics.drawOval ( x , y , width , height );

                    //Prints the matched superscripts that are numeric and with Hyperlink with it's values.
                    System.out.println ( "Marked numeric superscript: " + superscriptText + " at x=" + x + ", y=" + y );

                    //Prints the preceding Text values.
                    System.out.println ( "Preceding Text is " + getPrecedingText ( superscript, driver ) );
                    System.out.println ( "Superscript is: " + superscriptText );

                } catch (org.openqa.selenium.NoSuchElementException e) {
                    //if no hyperlink don't do anything.
                    System.err.println ( "Superscript " + superscriptText + " is numeric but does not have a hyperlink" );
                }
            } else {
                System.out.println ( "Skipping non-numeric superscript: " + superscriptText );
            }
        }

        graphics.dispose ( );

        // Save the combined and marked screenshot
        File outputFile = new File ( "screenshots/" + filename );
        outputFile.getParentFile ( ).mkdirs ( );
        ImageIO.write ( combinedImage , "png" , outputFile );

        System.out.println ( "Full page screenshot with superscripts marked saved to: " + outputFile.getAbsolutePath ( ) );
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

    // Helper method to get the preceding text node

        // Execute JavaScript to get the previous text node
        private static String getPrecedingText (WebElement element , Object driver ) {
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
}


