package org.myPack;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuperscriptStepDefinitions {

    private WebDriver driver;
    private String pageTitle;
    private String currentURL;
    private static int rowCount = 0;
    private static Workbook workbook;
    private static Sheet sheet;
    private static String timestamp;

    public static void main ( String[] args ) {

    }

    @Given("I have a Chrome browser for Superscript extraction")
    public void i_have_a_chrome_browser_for_superscript_extraction () {
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\DataValidationRates\\src\\main\\resources\\chromedriver.exe" ); // set correct path
        driver = new ChromeDriver ( );

        //Generate timestamp and file name at first run
        if ( timestamp == null ) {
            timestamp = new SimpleDateFormat ( "yyyyMMdd_HHmmss" ).format ( new Date ( ) );
            workbook = new XSSFWorkbook ( );
            sheet = workbook.createSheet ( "SuperscriptData" );

            //Write header row to spreadsheet
            Row headerRow = sheet.createRow ( rowCount++ );
            String[] headers = {"Page Title" , "PreceedingSuperScript" , "SuperScript Value" , "SuperSCriptWithHyperlink (YES/NO)" , "superscriptHyperlinkValue" , "superscriptNotOnSameLine (YES/NO)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell ( i );
                cell.setCellValue ( headers[i] );
            }
        }
    }

    @Given("I navigate to the web page {string}")
    public void i_navigate_to_the_web_page ( String url ) throws InterruptedException {
        currentURL = url;
        driver.get ( currentURL );
        WebDriverWait wait = new WebDriverWait ( driver , Duration.ofSeconds ( 15 ) );
        // String page = driver.getTitle();
        pageTitle = Objects.requireNonNull ( driver.getTitle ( ) ).length ( ) > 5 ? driver.getTitle ( ).substring ( 0 , 15 ) : driver.getTitle ( );
        System.out.println ( "Navigated to URL: " + currentURL + ", Page Title: " + pageTitle );
        // wait.until(ExpectedConditions.titleContains(pageTitle));
        int totalSuperscripts = 0;
    }

    @When("I handle any initial modal dialogues")
    public void i_handle_any_initial_modal_dialogues () {
        WebElement zipCodeInput = null;  // Declare here so it's accessible outside the try blocks
        WebElement zipCodeSubmit = null; // Declare here to be accessible
        zipCodeSubmit = driver.findElement ( By.xpath ( "//input[@value='Continue']" ) );
        try {

            WebElement zipCodeModal = driver.findElement ( By.xpath ( "//div[@class='zip-code-modal']" ) );

            if ( zipCodeModal.isDisplayed ( ) ) {
                zipCodeInput = zipCodeModal.findElement ( By.xpath ( "//input[@aria-labelledby='zip-code-label']" ) );
                zipCodeInput.click ( );
                zipCodeInput.sendKeys ( "12345" );
                //zipCodeInput.sendKeys ( PageObjects.ZipCodeNumber);
                zipCodeInput.submit ( );
                WebDriverWait wait = new WebDriverWait ( driver , Duration.ofSeconds ( 100 ) );
                wait.until ( ExpectedConditions.invisibilityOfElementLocated ( By.xpath ( "//div[@class='zip-code-modal']" ) ) );
                List<WebElement> collapsibleHeaders = driver.findElements ( By.xpath ( "//summary[@aria-expanded='false']" ) );

                // Click each collapsible header to expand its content
                for (WebElement header : collapsibleHeaders) {
                    try {
                        header.click ( );
                        System.out.println ( "Clicked and expanded a header." + header.getText ( ) );
                    } catch (Exception e) {
                        System.err.println ( "Error clicking a header: " + e.getMessage ( ) );
                    }
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println ( "The zipcode Modal wasn't present, trying alternative." );
            // Handle the case where the zipCodeModal is not found and code keeps running.
        }

        try {
            WebElement c28mainContainer = driver.findElement ( By.xpath ( "//div[@class='c28mainContainer']" ) );
            WebElement c28mainContainerInput = c28mainContainer.findElement ( By.xpath ( "//input[@class='big_input']" ) ); // Use c28mainContainer, not zipCodeModal
            if ( c28mainContainer.isDisplayed ( ) && c28mainContainer.isEnabled ( ) ) {


                c28mainContainerInput.click ( );
                c28mainContainerInput.sendKeys ( "94105" );
                zipCodeSubmit.submit ( );
                System.out.println ( "The c28mainContainer Modal was present." );
               /* WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@class='c28lightbox']"))); //*/
            }
        } catch (NoSuchElementException e) {
            System.out.println ( "The c28mainContainer Modal wasn't present." );
        }

        // Common operations to both try and catch code
        // Check if zipCodeInput or c28mainContainerInput was found and clicked
        /*if (c28mainContainerInput!= null){
            c28mainContainerInput.sendKeys("94070");
        }

        else
        {
            System.out.println ("Zip code input not found in the either form to send keys.");
        }
*/
        /*try {

            zipCodeSubmit.submit ( );

        } catch (NoSuchElementException e) {

            System.out.println ( "Submit was not  button not found for zipCode form" + e.getMessage ( ) );
        }*/

        try {

            WebDriverWait wait = new WebDriverWait ( driver , Duration.ofSeconds ( 15 ) );
            wait.until ( ExpectedConditions.invisibilityOfElementLocated ( By.xpath ( "//div[@id='c28mainContainer']" ) ) );

        } catch (Exception e) {
            //Handle if cannot find element by xpath
            System.out.println ( "No Zop Code Dialog is found to timeout and wait" + e.getMessage ( ) );
        }
    }




    @Then ("I extract superscript information and take screenshot")
    public void i_extract_superscript_information_and_take_screenshot() throws IOException, InterruptedException{
        //Short the Page title so image name is not too long with only the "page part" from the excel sheet
        String shortenedPageTitle = pageTitle.length() > 50 ? pageTitle.substring(0, 50) : pageTitle; // Adjust the length (50) as you wish
        String screenshotFilename = "superscripts_marked_" + shortenedPageTitle + "_" + timestamp + ".png";

        // Call the methods then combine into 1
        List<WebElement> superscripts = driver.findElements(By.xpath ("//sup[@class='c20ref']"));
        BufferedImage combinedImage = takeFullPageScreenshot(driver);
        markAllNumericSuperscripts(driver, combinedImage, superscripts );

        //Save the screenshot to directory file to local machine.
        saveMarkedScreenshot(combinedImage, screenshotFilename);


    }
    @Then("I write results to an Excel file")
    public void i_write_results_to_an_excel_file() throws IOException {
        //File writing is handled at teardown.
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

    private BufferedImage takeFullPageScreenshot(WebDriver driver) throws IOException {
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
                Thread.sleep(200); // Adjust sleep time if needed, but avoid overly long waits
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (scrollHeight < fullPageHeight);

        driver.manage().window().setSize(initialDim);
        BufferedImage combinedImage = new BufferedImage(bufferedImages.getFirst ( ).getWidth(), (int) fullPageHeight, BufferedImage.TYPE_INT_RGB);

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
        //Disposes at finish
        graphics.dispose();
        return combinedImage;
    }

    //Markup for all, and check the image is located at same spot.
    private void markAllNumericSuperscripts( WebDriver driver, BufferedImage combinedImage, List<WebElement> superscripts ) {
        //It adds Graphic to the image, and is set on another method
        Graphics2D graphics = combinedImage.createGraphics();

        graphics.setColor(Color.GREEN);
        graphics.setStroke(new BasicStroke(3));
        //It finds all SuperScript on webpage.
        int count=0;
        for (int i = 0; i < superscripts.size(); i++) {
            WebElement superscript = superscripts.get(i);
            String superscriptText = superscript.getText();
            //If text have more then one parameters
            Pattern pattern = Pattern.compile("^\\d+$");
            Matcher matcher = pattern.matcher(superscriptText);

            //It will circle all numbers,
            if (matcher.matches()) {
                count++;
                By superscriptLocator = null;
                try {
                    //It is hyperLink
                    superscriptLocator = By.xpath ( "//sup" );
                    WebElement parentLink = superscript.findElement ( By.xpath ( ".//ancestor::a[1]" ) );
                    String linkText = parentLink.getAttribute ( "href" );
                    //Get location and dimension point.
                    Point location = superscript.getLocation ( );
                    Dimension size = superscript.getSize ( );

                    int x = location.getX ( );
                    int y = location.getY ( );
                    int width = size.getWidth ( );
                    int height = size.getHeight ( );
                    graphics.drawOval ( x , y , width , height );

                    //Prints where everything is marked up based on those 2 arguments
                    //System.out.println("Super script at  (x,y) : " + x + " - " + y );
                    boolean sameLine = checkSameLine ( superscript , driver );
                    String verificationMessage = verifySuperscriptPosition ( driver , superscriptLocator );
                    System.out.println ( "Verification Result: " + verificationMessage );
                    Row dataRow = sheet.createRow ( rowCount++ );
                    int cellNum = 0;
                    Cell cell0 = dataRow.createCell ( cellNum++ );
                    cell0.setCellValue ( pageTitle );

                    // This append the text
                    Cell cell1 = dataRow.createCell ( cellNum++ );
                    cell1.setCellValue ( getPrecedingText ( superscript , driver ) );

                    //This is superScript
                    Cell cell2 = dataRow.createCell ( cellNum++ );
                    cell2.setCellValue ( superscriptText );

                    //Check for the HyperLink
                    Cell cell3 = dataRow.createCell ( cellNum++ );
                    cell3.setCellValue ( "YES" );

                    //This shows which value.
                    Cell cell4 = dataRow.createCell ( cellNum++ );
                    cell4.setCellValue ( linkText );

                    //It displays it on excel file by YES or NO
                    Cell cell5 = dataRow.createCell ( Integer.parseInt ( verificationMessage ) );
                    cell5.setCellValue ( sameLine ? "YES" : "NO" );

                } catch (org.openqa.selenium.NoSuchElementException e) {
                    //It is to display with not to do anything with supercsript
                    //Get location and dimension point.
                    graphics.setColor ( Color.RED );
                    graphics.setStroke ( new BasicStroke ( 3 ) );
                    Point location = superscript.getLocation ( );
                    Dimension size = superscript.getSize ( );

                    int x = location.getX ( );
                    int y = location.getY ( );
                    int width = size.getWidth ( );
                    int height = size.getHeight ( );
                    graphics.drawOval ( x , y , width , height );
                    System.err.println ( "This has numeric superscripts in web elements but does not have elements" + superscriptText );
                    boolean sameLine = checkSameLine ( superscript , driver );
                    String verificationMessage = verifySuperscriptPosition ( driver , superscriptLocator );
                    System.out.println ( "Verification Result: " + verificationMessage );


                }

                //If test does not meet the follow requirements, does not run any of those
                if ( checkSameLine ( superscript , driver ) ) {
                    return;
                }

            } //If it has no name does this
            else {
                System.out.println("Skipping non-numeric superscript: " + superscriptText);
            }
        }
        System.out.println("Total number of numeric superscript: " + count);
        //Assert.assertEquals(count, Integer.parseInt( String.valueOf ( totalSuperscripts ) ));
        //Release grapchics for memory safe
        graphics.dispose();
    }

    // It saves images to folder.
    private void saveMarkedScreenshot(BufferedImage combinedImage, String filename) throws IOException {
//truncate page title for file name length



        File outputFile = new File("screenshots/" + filename);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(combinedImage, "png", outputFile);

        System.out.println("Full page screenshot with superscripts marked saved to: " + outputFile.getAbsolutePath());
    }

    //Check to see if all conditions are followed in the image. If the javascript fails at the bottom.
    //This returns an exception and code will not work.
    // Helper method to check if the superscript and preceding text are on the same line
    private boolean checkSameLine(WebElement element, WebDriver driver) {
        try {
            // Execute JavaScript to compare the top positions of the element and its preceding text
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                    "var elem = arguments[0];" +
                            "var rect = elem.getBoundingClientRect();" +
                            "var prev = elem.previousSibling;" +
                            "while (prev && prev.nodeType != 3) {" +
                            "  prev = prev.previousSibling;" +
                            "}" +
                            "if (!prev) return false;" +
                            "if (prev.nodeType === 1) {" +
                            "    var prevRect = prev.getBoundingClientRect();" +
                            "    return rect.top === prevRect.top;" +
                            "} else {" +
                            "    return false;" +
                            "}", element);

            // Check if the result is null before casting to Boolean
            if (result == null) {
                System.err.println("JavaScript returned null for same-line check.");
                return false; // Or handle the null case as appropriate for your logic
            }

            Boolean sameLine = (Boolean) result;
            return sameLine;

        } catch (Exception e) {
            System.err.println("Error checking same line: " + e.getMessage());
            return false; // Return false in case of an error
        }
    }

    // Helper method to get the preceding text node
    // This tests for 3 = type Node
    private String getPrecedingText ( WebElement element , WebDriver driver ) {
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