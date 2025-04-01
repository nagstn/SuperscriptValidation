package org.example;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.poi.ss.formula.functions.Column;
import org.apache.poi.ss.formula.functions.Columns;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;
import java.time.Duration;
import java.util.function.Function;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static java.awt.geom.Path2D.contains;

//import static jdk.internal.agent.Agent.getText;

public class StepDefinitions {
    WebDriver driver;

    @Given("I open the web page {string}")
    public void i_open_the_web_page ( String url ) {
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );
        driver = new ChromeDriver ( );
        driver.get ( url );
    }

    @When("I enter the zip code {string}")
    public void i_enter_the_zip_code ( String zipCode ) throws InterruptedException {
        WebElement zipCodeModal = driver.findElement ( By.xpath ( "//div[@class='zip-code-modal']" ) );
        if ( zipCodeModal.isDisplayed ( ) ) {
            WebElement zipCodeInput = zipCodeModal.findElement ( By.xpath ( "//input[@aria-labelledby='zip-code-label']" ) );
            zipCodeInput.click ( );
            zipCodeInput.sendKeys ( zipCode );
            WebElement zipCodeInput1 = driver.findElement ( By.xpath ( "//input[@type='submit']" ) );
            zipCodeInput1.click ( );
        }
    }

    @And("I find all rates tables on the web page")
    public void i_find_all_rates_tables_on_the_web_page () {
        // This step is just to locate the tables, no action needed here
    }


    @Then("I copy all rates tables into an Excel file")
    public void i_copy_all_rates_tables_into_an_excel_file() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rates");
            String[] tableXPaths = {
                    "//*[(@id = 'way2save_rates')]",
                    "//div[@id='rates_standard_table']",
                    "//div[@id='platinum_premier_prime_rates']",
                    "//div[@id='platinum_premier_rates']"
            };
            int rowNum = 0;
            for (String tableXPath : tableXPaths) {

                // Relocate the table *every time* inside the loop.
                WebElement table = driver.findElement(By.xpath(tableXPath));

                Row headerRow = sheet.createRow(rowNum++);
                int cellNum = 0;

                // Relocate the headings *inside* the conditional blocks
                List<WebElement> headings;
                List<WebElement> headingPlatinum;

                // Robust getText with retry
                String tableText = getElementTextWithRetry(table);

                if (tableText.contains("Way2Save")) {
                    headings = table.findElements(By.xpath(".//h3[@class='ratesSectionHeading']"));
                    for (WebElement heading : headings) {
                        Cell cell = headerRow.createCell(cellNum++);
                        cell.setCellValue(heading.getText());
                    }
                } else {
                    headingPlatinum = table.findElements(By.xpath(".//h3[@class='ratesSubproductHolder']"));
                    for (WebElement heading1 : headingPlatinum) {
                        Cell cell = headerRow.createCell(cellNum++);
                        cell.setCellValue(heading1.getText());
                    }
                }

                List<WebElement> rows = table.findElements(By.xpath(".//div[@role='row']"));

                for (WebElement row : rows) {
                    List<WebElement> cells = row.findElements(By.xpath(".//div[@role='cell']"));
                    if (!cells.isEmpty()) {
                        Row excelRow = sheet.createRow(rowNum++);
                        cellNum = 0;
                        for (WebElement cell : cells) {
                            Cell excelCell = excelRow.createCell(cellNum++);
                            excelCell.setCellValue(cell.getText());
                        }
                    }
                }
            }
            try (FileOutputStream fileOut = new FileOutputStream("rates.xlsx")) {
                workbook.write(fileOut);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getElementTextWithRetry(WebElement element) {
        FluentWait<WebElement> wait = new FluentWait<> ( element )
                .withTimeout ( Duration.ofSeconds ( 5 ) ) // Total wait time
                .pollingEvery ( Duration.ofMillis ( 100 ) ); // Check every 100 milliseconds
               // .ignoreAll ( StaleElementReferenceException.class ); // Ignore StaleElementReferenceException

        try {
            return wait.until ( new Function<WebElement, String> ( ) {
                @Override
                public String apply ( WebElement element ) {
                    try {
                        return element.getText ( );
                    } catch (StaleElementReferenceException e) {
                        return null; // Signal to retry
                    }
                }
            } );
        } catch (TimeoutException e) {
            // If it still fails after the timeout, log an error and return an empty string or throw an exception
            System.err.println ( "Failed to get text after multiple retries: " + e.getMessage ( ) );
            return ""; // Or throw a custom exception
        }
    }
}
