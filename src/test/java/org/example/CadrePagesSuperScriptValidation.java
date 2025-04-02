package org.example;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.Point;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.junit.Assert.assertTrue;

public class CadrePagesSuperScriptValidation {

    private WebDriver driver;
    private String pageTitle;
    // Generate timestamp for unique filenames
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    String timestamp = LocalDateTime.now().format( dtf );
    @Given("I have a Chrome browser")
    public void iHaveAChromeBrowser() {
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );
        driver = new ChromeDriver();
    }

    @When("I navigate to {string}")
    public void iNavigateTo(String CurrentUrl) {
        driver.get(CurrentUrl);
        pageTitle = driver.getTitle();
    }

    @Then("I verify the page and extract superscripts")
    public void iVerifyThePageAndExtractSuperscripts() throws IOException {
        // Handle zip code modal (example, might need adjustments)
        try {
            WebElement zipCodeModal = driver.findElement(By.xpath("//div[@class='zip-code-modal']"));
            if (zipCodeModal.isDisplayed()) {
                WebElement zipCodeInput = zipCodeModal.findElement(By.xpath("//input[@aria-labelledby='zip-code-label']"));
                zipCodeInput.click();
                zipCodeInput.sendKeys("94070");
                WebElement zipCodeSubmit = driver.findElement(By.xpath("//input[@type='submit']"));
                zipCodeSubmit.click();

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@class='zip-code-modal']")));
            }
        } catch (org.openqa.selenium.NoSuchElementException e) {
            System.out.println("The zipcode Modal wasn't present");
        }

        //Take the full web page Screenshot for Verification
        takeFullPageScreenshotWithMarkups(driver, "superscripts_marked.png");
        // Check zip code element

        // Other verifications (superscripts, etc.) are done in the takeFullPageScreenshotWithMarkups() method
    }

    @Then("I write results to an Excel file")
    public void iWriteResultsToAnExcelFile() throws IOException {
        writeExtractedDataToExcel(pageTitle);
        if (driver != null) {
            driver.quit();
        }
    }

    //**Helper Method to Take Full Page Screenshot with Superscript Markups**
    private void takeFullPageScreenshotWithMarkups(WebDriver driver, String filename) throws IOException {
        // Get entire page height
        long fullPageHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

        // Set initial window height and create combined image
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
        Dimension initialDim = driver.manage().window().getSize();

        List<BufferedImage> bufferedImages = new ArrayList<>();
        double scrollHeight = 0;

        do {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            BufferedImage bufferedImage = ImageIO.read(screenshotFile);
            bufferedImages.add(bufferedImage);

            scrollHeight += bufferedImage.getHeight();
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, " + scrollHeight + ")");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (scrollHeight < fullPageHeight);

        driver.manage ( ).window ( ).setSize ( initialDim );
        BufferedImage combinedImage = new BufferedImage(bufferedImages.getFirst ( ).getWidth(), (int) fullPageHeight, BufferedImage.TYPE_INT_RGB);
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
        List<WebElement> superscripts = driver.findElements(By.tagName("sup"));
        //extractedData.clear(); // Clear the data for every page
        graphics.setColor(Color.GREEN);
        graphics.setStroke(new BasicStroke(3));
        for (WebElement superscript : superscripts) {
            String superscriptText = superscript.getText().trim();
            String precedingText = getPrecedingText(superscript, driver);

            Pattern pattern = Pattern.compile("^\\d+$");
            Matcher matcher = pattern.matcher(superscriptText);


            if (matcher.matches()) {
                try {
                    WebElement parentLink = superscript.findElement(By.xpath(".//ancestor::a[1]"));

                    Point location = superscript.getLocation();
                    Dimension size = superscript.getSize();

                    int x = location.getX();
                    int y = location.getY();
                    int width = size.getWidth();
                    int height = size.getHeight();

                    graphics.drawOval(x, y, width, height);
                    System.out.println("Marked numeric superscript: " + superscriptText + " at x=" + x + ", y=" + y);

                    extractedData.add(new String[]{pageTitle, precedingText, superscriptText, "Yes"});//Adding values into array, with Yes

                } catch (org.openqa.selenium.NoSuchElementException e) {
                    //System.out.println("Number superscript: " + superscriptText + " But It's not a hyperlink tag");
                    extractedData.add(new String[]{pageTitle, precedingText, superscriptText, "No"}); //Adding values into array, with NO
                }
            }

        }

        graphics.dispose();

        // Save the combined and marked screenshot
        File outputFile = new File("screenshots/" + filename);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(combinedImage, ".png", outputFile);

        System.out.println("Full page screenshot with superscripts marked saved to: " + outputFile.getAbsolutePath());
    }

    //Helper method to robustly get the link text
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
    //Helper to get the Javascript of webpage.
    private static String getPrecedingText(WebElement element ,WebDriver driver) {

        try {
            // Execute JavaScript to get the previous text node
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object precedingTextObj = js.executeScript(
                    """
                            var elem = arguments[0];
                            var prev = elem.previousSibling;
                            while (prev && prev.nodeType != 3) {
                              prev = prev.previousSibling;
                            }
                            return prev ? prev.textContent : '';""" , element);

            // Return the text or an empty string if no preceding text node is found
            return (precedingTextObj != null) ? precedingTextObj.toString().trim() : "";

        } catch (Exception e) {
            System.err.println("Error getting preceding text: " + e.getMessage());
            return ""; // Return an empty string in case of an error
        }
    }

    private static final List<String[]> extractedData = new ArrayList<>(); // Store the extraction data

    //**Helper Method to Write Data to Excel File***
    private void writeExtractedDataToExcel(String pageTitle) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Superscript Data");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Page Title", "Preceding Text", "Superscript Value", "Superscript With Hyperlink"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Write data rows
        int rowNum = 1;
        for (String[] data : extractedData) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < data.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(data[i]);
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write the workbook to a file
        try (FileOutputStream outputStream = new FileOutputStream("superscript_data.xlsx")) {
            workbook.write(outputStream);
        }
        workbook.close();

        System.out.println("Extracted data written to superscript_data.xlsx");
    }
}
