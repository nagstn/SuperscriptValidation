package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class TableDataToExcel {

    public static void main(String[] args) throws IOException {
        // Set the path to your ChromeDriver executable
        //System.setProperty("webdriver.chrome.driver", "path/to/chromedriver");
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );
        // Initialize WebDriver
        WebDriver driver = new ChromeDriver();

        // Navigate to the web page
        driver.get("https://www.wellsfargo.com/checking/prime/");
        WebElement zipCodeModal = driver.findElement ( By.xpath ( "//div[@class='zip-code-modal']" ) );
        if ( zipCodeModal.isDisplayed ( ) ) {
            WebElement zipCodeInput = zipCodeModal.findElement ( By.xpath ( "//input[@aria-labelledby='zip-code-label']" ) );
            zipCodeInput.click ( );
            zipCodeInput.sendKeys ( "94070" );
            WebElement zipCodeInput1 = driver.findElement ( By.xpath ( "//input[@type='submit']" ) );
            zipCodeInput1.click ( );
        }
        // Updated XPath expression to find table-like structures.  Adjust as needed.
        String tableXPath = "//*[contains(@class, 'table') or contains(@role, 'table')]";  // Example
        List<WebElement> tables = driver.findElements(By.xpath(tableXPath));

        // Create a new Excel workbook
        Workbook workbook = new XSSFWorkbook();

        // Iterate through each "table"
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            WebElement table = tables.get(tableIndex);

            // Create a new sheet for each table
            String sheetName = "Table " + (tableIndex + 1);
            Sheet sheet = workbook.createSheet(sheetName);

            int rowNum = 0;

            // Adjust these XPaths to find headings and rows based on the identified table structure
            String headingXPath = ".//*[contains(@class, 'heading') or @role='columnheader']"; // Example
            String rowXPath = ".//*[contains(@class, 'row') or @role='row']"; // Example
            String cellXPath = ".//*[contains(@class, 'cell') or @role='cell']"; // Example

            // Extract column headings
            List<WebElement> columnHeadings = table.findElements(By.xpath(headingXPath));
            Row headerRow = sheet.createRow(rowNum++);
            int cellNum = 0;
            for (WebElement heading : columnHeadings) {
                Cell cell = headerRow.createCell(cellNum++);
                cell.setCellValue(heading.getText());
            }

            // Extract row data
            List<WebElement> rows = table.findElements(By.xpath(rowXPath));
            // Start from the correct row (skipping header row if it exists)
            // int startRow = columnHeadings.isEmpty() ? 0 : 1; // No longer needed with adjusted rowXPath

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                WebElement row = rows.get(rowIndex);
                List<WebElement> cells = row.findElements(By.xpath(cellXPath));
                Row excelRow = sheet.createRow(rowNum++);
                cellNum = 0;
                for (WebElement cell : cells) {
                    Cell excelCell = excelRow.createCell(cellNum++);
                    excelCell.setCellValue(cell.getText());
                }
            }
        }

        // Write the workbook to a file
        try (FileOutputStream outputStream = new FileOutputStream("table_data.xlsx")) {
            workbook.write(outputStream);
        }

        // Close the workbook
        workbook.close();

        // Close the browser
        driver.quit();

        System.out.println("Table data written to table_data.xlsx");
    }
}