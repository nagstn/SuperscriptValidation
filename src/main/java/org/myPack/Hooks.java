// --- In Hooks.java ---
package org.myPack; // Or your hooks package

import io.cucumber.java.AfterAll;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

// Import the specific inner/nested classes if they are defined that way,
// otherwise import the top-level classes
// Assuming they are defined in SuperscriptStepDefinitions for this example:
import static org.myPack.SuperscriptStepDefinitions.SuperscriptInfo;
import static org.myPack.SuperscriptStepDefinitions.VerificationSummary;
// OR if they are top-level classes:
// import org.myPack.SuperscriptInfo;
// import org.myPack.VerificationSummary;


public class Hooks {

    @AfterAll
    public static void afterAllScenarios() {
        System.out.println("--- Running @AfterAll Hook ---");

        // Access the STATIC workbook and timestamp from the Step Definition class
        if (SuperscriptStepDefinitions.workbook != null && SuperscriptStepDefinitions.timestamp != null) {
            String excelFilename = "superscript_validation_" + SuperscriptStepDefinitions.timestamp + ".xlsx";
            System.out.println("Attempting to write final Excel file: " + excelFilename);

            try {
                Sheet detailSheet = SuperscriptStepDefinitions.workbook.getSheet("SuperscriptDetail");
                Sheet summarySheet = SuperscriptStepDefinitions.workbook.getSheet("ValidationSummary");

                // --- Populate Detail Sheet ---
                if (detailSheet != null) {
                    System.out.println("Populating Detail Sheet... Found " + SuperscriptStepDefinitions.allSuperscriptDetails.size() + " detail records.");
                    int detailDataRowIndex = 1;
                    for (SuperscriptInfo info : SuperscriptStepDefinitions.allSuperscriptDetails) {
                        Row dataRow = detailSheet.createRow(detailDataRowIndex++);
                        // ... (Populate detail row cells) ...
                        int cellNum = 0;
                        dataRow.createCell(cellNum++).setCellValue(info.tabIdentifier != null ? info.tabIdentifier : "N/A");
                        // ... other cells ...
                    }
                    // ... (Auto-size columns) ...
                    System.out.println("Detail Sheet populated.");
                } else { System.err.println("ERROR: Detail sheet is null!"); }


                // --- Populate Summary Sheet ---
                if (summarySheet != null) {
                    System.out.println("Populating Summary Sheet... Found " + SuperscriptStepDefinitions.tabSummaries.size() + " summary entries.");
                    int summaryDataRowIndex = 1; // Start after header
                    // Iterate the map which now contains ONE entry per page/tab processing run
                    for (Map.Entry<String, VerificationSummary> entry : SuperscriptStepDefinitions.tabSummaries.entrySet()) {
                        Row summaryDataRow = summarySheet.createRow(summaryDataRowIndex++);
                        VerificationSummary summary = entry.getValue();
                        int cellNum = 0;
                        summaryDataRow.createCell(cellNum++).setCellValue(entry.getKey()); // Page/Tab Run Identifier
                        // ... (Populate counts: totalFound, numericProcessed, posPassed, etc., from summary object) ...
                        // Write validation columns
                        Cell expectedCell = summaryDataRow.createCell(cellNum++);
                        if (summary.expectedNumericTotal == -1) expectedCell.setCellValue("N/A");
                        else expectedCell.setCellValue(summary.expectedNumericTotal);
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.finalResult); // Write PASS/FAIL/SKIPPED
                    }
                    // ... (Auto-size columns) ...
                    System.out.println("Summary Sheet populated.");
                } else { System.err.println("ERROR: Summary sheet is null!"); }


                // --- Write to File ---
                try (FileOutputStream outputStream = new FileOutputStream(excelFilename)) {
                    SuperscriptStepDefinitions.workbook.write(outputStream);
                    System.out.println("----> Final Excel data successfully written to: " + new File(excelFilename).getAbsolutePath());
                } catch (IOException e) { // Catch specific IO exception for writing
                    System.err.println("ERROR writing final Excel file in @AfterAll: " + e.getMessage());
                    e.printStackTrace();
                }

            } catch (Exception e) { // Catch broader exceptions during sheet population
                System.err.println("UNEXPECTED error during final Excel processing in @AfterAll: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Close the workbook resource
                try {
                    if (SuperscriptStepDefinitions.workbook != null) {
                        SuperscriptStepDefinitions.workbook.close();
                        System.out.println("Workbook closed in @AfterAll.");
                    }
                } catch (IOException e) {
                    System.err.println("Error closing workbook in @AfterAll: " + e.getMessage());
                }
            }
        } else {
            System.err.println("@AfterAll: Workbook or Timestamp was null. Cannot write Excel file.");
        }
        System.out.println("--- Finished @AfterAll Hook ---");
    }
}