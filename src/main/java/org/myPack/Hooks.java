package org.myPack; // Or your hooks package

import io.cucumber.java.AfterAll;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet; // Add import
import java.io.File; // Add import
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class Hooks {

    @AfterAll
    public static void afterAllScenarios() {
        System.out.println("--- Running @AfterAll Hook ---");

        // Access the static workbook and timestamp
        if (SuperscriptStepDefinitions.workbook != null && SuperscriptStepDefinitions.timestamp != null) {
            String excelFilename = "superscript_validation_" + SuperscriptStepDefinitions.timestamp + ".xlsx";
            System.out.println("Attempting to write final Excel file: " + excelFilename);

            try {
                // --- Get References to Sheets ---
                Sheet detailSheet = SuperscriptStepDefinitions.workbook.getSheet("SuperscriptDetail");
                Sheet summarySheet = SuperscriptStepDefinitions.workbook.getSheet("ValidationSummary");

                if (detailSheet == null) {
                    System.err.println("ERROR: Detail sheet is null in @AfterAll!");
                } else {
                    // --- Populate Detail Sheet from static list ---
                    System.out.println("Populating Detail Sheet... Found " + SuperscriptStepDefinitions.allSuperscriptDetails.size() + " detail records.");
                    int detailDataRowIndex = 1; // Start after header
                    for (SuperscriptStepDefinitions.SuperscriptInfo info : SuperscriptStepDefinitions.allSuperscriptDetails) {
                        Row dataRow = detailSheet.createRow(detailDataRowIndex++); // Use local counter for writing
                        int cellNum = 0;
                        dataRow.createCell(cellNum++).setCellValue(info.tabIdentifier);
                        dataRow.createCell(cellNum++).setCellValue(info.superscriptText);
                        dataRow.createCell(cellNum++).setCellValue(info.precedingText);
                        dataRow.createCell(cellNum++).setCellValue(info.isHyperlink ? "YES" : "NO");
                        dataRow.createCell(cellNum++).setCellValue(info.linkHref);
                        dataRow.createCell(cellNum++).setCellValue(info.positionStatus != null ? info.positionStatus.name() : "N/A"); // Null check
                        dataRow.createCell(cellNum++).setCellValue(info.verificationMessage != null ? info.verificationMessage : "N/A"); // Null check
                    }
                    System.out.println("Detail Sheet populated.");
                }

                if (summarySheet == null) {
                    System.err.println("ERROR: Summary sheet is null in @AfterAll!");
                } else {
                    // --- Populate Summary Sheet from static map ---
                    System.out.println("Populating Summary Sheet... Found " + SuperscriptStepDefinitions.tabSummaries.size() + " tab summaries.");
                    int summaryDataRowIndex = 1; // Start after header
                    for (Map.Entry<String, SuperscriptStepDefinitions.VerificationSummary> entry : SuperscriptStepDefinitions.tabSummaries.entrySet()) {
                        Row summaryDataRow = summarySheet.createRow(summaryDataRowIndex++); // Use local counter for writing
                        SuperscriptStepDefinitions.VerificationSummary summary = entry.getValue();
                        int cellNum = 0;
                        summaryDataRow.createCell(cellNum++).setCellValue(entry.getKey()); // Tab Identifier
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.totalSuperscriptsFoundInPanel);
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.numericSuperscriptsProcessed);
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.positionCheckPassed);
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.positionCheckFailed);
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.positionCheckErrors);
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.hyperlinkCount);
                        summaryDataRow.createCell(cellNum++).setCellValue(summary.noHyperlinkCount);
                        // Add more summary columns if needed (e.g., empty count, non-numeric count)
                    }
                    System.out.println("Summary Sheet populated.");
                }

                // --- Write to File ---
                try (FileOutputStream outputStream = new FileOutputStream(excelFilename)) {
                    SuperscriptStepDefinitions.workbook.write(outputStream);
                    System.out.println("----> Final Excel data successfully written to: " + new File(excelFilename).getAbsolutePath());
                }

            } catch (IOException e) {
                System.err.println("ERROR writing final Excel file in @AfterAll: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("UNEXPECTED error during final Excel processing/writing in @AfterAll: " + e.getMessage());
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
    }
}