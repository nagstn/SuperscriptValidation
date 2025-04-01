package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.io.FileHandler;
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

public class SuperscriptScreenshot {

    public static void main(String[] args) throws IOException {
        // Set the path to your ChromeDriver executable
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver();

        // Navigate to the web page
        driver.get("https://www.wellsfargo.com/checking/prime/");

        try {
            //Handle zip code modal:
            WebElement zipCodeModal;
            try {
                zipCodeModal = driver.findElement(By.xpath("//div[@class='zip-code-modal']"));
                if (zipCodeModal.isDisplayed()) {
                    WebElement zipCodeInput = zipCodeModal.findElement(By.xpath("//input[@aria-labelledby='zip-code-label']"));
                    zipCodeInput.click();
                    zipCodeInput.sendKeys("94070");
                    WebElement zipCodeSubmit = driver.findElement(By.xpath("//input[@type='submit']"));
                    zipCodeSubmit.click();

                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(100));
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@class='zip-code-modal']"))); //

                    // Take the full page screenshot using JavaScript scrolling and combining
                    takeFullPageScreenshotWithMarkups(driver, "superscripts_marked.png");
                }

            } catch (org.openqa.selenium.NoSuchElementException e) {
                System.out.println("The zipcode Modal wasn't present");
                // Take full page screenshot directly if there is no zip code modal
                takeFullPageScreenshotWithMarkups(driver, "superscripts_marked.png");

            }
        } finally {
            // Close the browser
            driver.quit();
        }
    }

    //**Helper Method to Take Full Page Screenshot with Superscript Markups**
    private static void takeFullPageScreenshotWithMarkups(WebDriver driver, String filename) throws IOException {
        // Get entire page height
        long fullPageHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
        System.out.println(fullPageHeight);

        // Set initial window height to something smaller for multiple images to be captured
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
        Dimension initialDim = driver.manage().window().getSize();

        // Get different scrolls of images until end
        List<BufferedImage> bufferedImages = new ArrayList<> ();
        double scrollHeight = 0;

        do {
            //Scrolls down and capture
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            BufferedImage bufferedImage = ImageIO.read(screenshotFile);
            bufferedImages.add(bufferedImage);

            //Scroll down, if not at the end
            scrollHeight = scrollHeight + bufferedImage.getHeight();

            //Scroll the window based on the next scroll
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, " + scrollHeight + ")");
            try {
                Thread.sleep(200); // Adjust sleep time if needed, but avoid overly long waits
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (scrollHeight < fullPageHeight);

        driver.manage().window().setSize(initialDim);
        System.out.println(bufferedImages.size());

        //Create result Image, from the size of the full page
        BufferedImage combinedImage = new BufferedImage(bufferedImages.get(0).getWidth(), (int) fullPageHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = combinedImage.createGraphics();

        //Set the back ground to be white
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

        // Find all superscript elements (<sup>) and draw circles
        List<WebElement> superscripts = driver.findElements(By.tagName("sup"));
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(3));
        for (WebElement superscript : superscripts) {
            try {
                Point location = superscript.getLocation();
                Dimension size = superscript.getSize();

                int x = location.getX();
                int y = location.getY();
                int width = size.getWidth();
                int height = size.getHeight();

                graphics.drawOval(x, y, width, height);
                System.out.println("Marked superscript at: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
            } catch (Exception e) {
                System.err.println("Error marking superscript: " + e.getMessage());
            }
        }
        graphics.dispose();

        // Save the combined and marked screenshot
        File outputFile = new File("screenshots/" + filename);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(combinedImage, "png", outputFile);

        System.out.println("Full page screenshot with superscripts marked saved to: " + outputFile.getAbsolutePath());
    }

    // Helper method to robustly get the link text
    private static String getLinkText(WebElement parentLink) {
        try {
            // First, try to find a span (or other element) within the link
            WebElement textElement = parentLink.findElement(By.xpath(".//*")); // Find any child element
            return textElement.getText();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            // If no span (or other child) exists, get the text directly from the link
            return parentLink.getText();
        }
    }
}