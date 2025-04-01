package org.example;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.Point;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SuperscriptStepDefinitions {

    private WebDriver driver;
    private String pageTitle;
    public String currentURL;
    private static int rowCount = 0; // Static variable to keep track of row count
    private static Workbook workbook;
    private static Sheet sheet;
    private static String timestamp;

    @Given("I have a Chrome browser for Superscript extraction")
    public void i_have_a_chrome_browser_for_superscript_extraction() {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe"); // set correct path
        driver = new ChromeDriver();

        //Generate file name at first run instead
        if (timestamp == null) {
            timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("SuperscriptData");
        }
        rowCount = 0;  // Reset rowCount each scenario outline
    }

    @Given("I navigate to the web page {string}")
    public void i_navigate_to_the_web_page(String url) {
        currentURL = url;
        driver.get(currentURL);
        pageTitle = driver.getTitle();
        System.out.println("Navigated to URL: " + currentURL + ", Page Title: " + pageTitle);
    }

    @When("I handle any initial modal dialogues")
    public void i_handle_any_initial_modal_dialogues() {
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

        } catch (NoSuchElementException e) {
            System.out.println("The zipcode Modal wasn't present");
        }
    }

    @Then("I extract superscript information and take screenshot")
    public void i_extract_superscript_information_and_take_screenshot() throws IOException {
        String screenshotFilename = "superscripts_marked_" + timestamp + ".png";
        takeFullPageScreenshotWithMarkupsAndData(driver, screenshotFilename);
    }

    @Then("I close the browser for Superscript extraction")
    public void i_close_the_browser_for_superscript_extraction() throws IOException {
        if (driver != null) {
            driver.quit();
        }
        //Write excel at the very end
        try (FileOutputStream outputStream = new FileOutputStream("superscript_data_" + timestamp + ".xlsx")) {
            workbook.write(outputStream);
        }
    }

    private void takeFullPageScreenshotWithMarkupsAndData(WebDriver driver, String filename) throws IOException {
        // Get entire page height
        long fullPageHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

        // Set initial window height and create combined image
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
        Dimension initialDim = driver.manage().window().getSize();

        List<BufferedImage> bufferedImages = new ArrayList<> ();
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

        driver.manage().window().setSize(initialDim);
        BufferedImage combinedImage = new BufferedImage(bufferedImages.getFirst ( ).getWidth(), (int) fullPageHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = combinedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, combinedImage.getWidth(), combinedImage.getHeight());
        graphics.setColor(Color.GREEN);
        graphics.setStroke(new BasicStroke(3));

        // Get list of super scripts and write its output to console
        List<WebElement> superscripts = driver.findElements(By.tagName("sup"));
        for (WebElement superscript : superscripts) {
            String superscriptText = superscript.getText().trim();
            Pattern pattern = Pattern.compile("^\\d+$");
            Matcher matcher = pattern.matcher(superscriptText);

            if (matcher.matches()) {
                try {
                    //looking for a link to be highlighter, find a method
                    WebElement parentLink = superscript.findElement(By.xpath(".//ancestor::a[1]"));

                    //This is where it calculates.
                    //This is called after, before we do a check for what exists

                    //Add Graphic
                    Point location = superscript.getLocation();
                    Dimension size = superscript.getSize();

                    // define size of circle/Oval by drawing from locations and sizes of element on webpage.
                    int x = location.getX();
                    int y = location.getY();
                    int width = size.getWidth();
                    int height = size.getHeight();

                    //This draws Oval or circle to the image if parameters are satisfied,
                    graphics.drawOval(x, y, width, height);

                    Row dataRow = sheet.createRow(rowCount++);
                    int cellNum = 0;

                    //This appends to Excel document for validation purposes
                    //Creates Excel spreadsheet columns with their respective names.
                    //Creates PageTitle
                    Cell cell0 = dataRow.createCell(cellNum++);
                    cell0.setCellValue(pageTitle);

                    // Appends the text if it is there
                    Cell cell1 = dataRow.createCell(cellNum++);
                    cell1.setCellValue(getPrecedingText(superscript));

                    Cell cell2 = dataRow.createCell(cellNum++);
                    cell2.setCellValue(superscriptText);

                    Cell cell3 = dataRow.createCell(cellNum++);
                    cell3.setCellValue("Has HyperLink"); // Add hyperlink check here
                    //
                    boolean sameLine = checkSameLine(superscript);
                    Cell cell4 = dataRow.createCell(cellNum++);
                    cell4.setCellValue(sameLine ? "Same Line" : "Not Same Line");


                } catch (org.openqa.selenium.NoSuchElementException e) {
                    Row dataRow = sheet.createRow(rowCount++);
                    int cellNum = 0;
                    Cell cell0 = dataRow.createCell(cellNum++);
                    cell0.setCellValue(pageTitle);

                    // Appends the text if it is there
                    Cell cell1 = dataRow.createCell(cellNum++);
                    cell1.setCellValue(getPrecedingText(superscript));

                    Cell cell2 = dataRow.createCell(cellNum++);
                    cell2.setCellValue(superscriptText);
                    Cell cell3 = dataRow.createCell(cellNum++);
                    cell3.setCellValue("No Hyperlink");
                }
            }
        }
        graphics.dispose();

        // Save the combined and marked screenshot
        File outputFile = new File("screenshots/" + filename);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(combinedImage, "png", outputFile);

        System.out.println("Full page screenshot with superscripts marked saved to: " + outputFile.getAbsolutePath());
    }

    // Helper method to get the preceding text node
    private String getPrecedingText(WebElement element) {
        try {
            // Execute JavaScript to get the previous text node
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object precedingTextObj = js.executeScript(
                    "var elem = arguments[0];\n" +
                            "var prev = elem.previousSibling;\n" +
                            "while (prev && prev.nodeType !== 3) {\n" +
                            "  prev = prev.previousSibling;\n" +
                            "}\n" +
                            "return prev ? prev.textContent : '';", element);

            // Return the text or an empty string if no preceding text node is found
            return (precedingTextObj != null) ? precedingTextObj.toString().trim() : "";

        } catch (Exception e) {
            System.err.println("Error getting preceding text: " + e.getMessage());
            return ""; // Return an empty string in case of an error
        }
    }

    //Helper method to add superscript text is located at the same line
    private boolean checkSameLine(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Boolean sameLine = (Boolean) js.executeScript(
                "var elem = arguments[0];\n" +
                        "var rect = elem.getBoundingClientRect();\n" +
                        "var prev = elem.previousSibling;\n" +
                        "while (prev && prev.nodeType !== 3) {\n" +
                        "  prev = prev.previousSibling;\n" +
                        "}\n" +
                        "if (!prev) return false;\n" +
                        "var prevRect = prev.getBoundingClientRect();\n" +
                        "return rect.top === prevRect.top;", element);
        return Boolean.TRUE.equals ( sameLine );
    }

    @Given("I navigate to the web page {string}")
    public void iNavigateToTheWebPageString () {
    }

    @Then("I verify the presence of numeric superscripts with hyperlinks")
    public void iVerifyThePresenceOfNumericSuperscriptsWithHyperlinks () {
    }

    @Then("I extract the text and URL from the superscript elements")
    public void iExtractTheTextAndURLFromTheSuperscriptElements () {
    }
}