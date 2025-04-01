package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.apache.commons.io.FileUtils;  // Need to add dependency: commons-io
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;  // for image processing if using Java's built-in methods
import java.awt.image.BufferedImage;  // for image processing if using Java's built-in methods

public class WebPageComparator {

    private static final String OUTPUT_DIR = "comparison_results";
    private static final double THRESHOLD = 0.05; // 5% threshold for visual difference

    public static void main ( String[] args ) throws IOException, InterruptedException {
        String url1 = "https://www.wellsfargo.com/checking/";  // Replace with your URLs
        String url2 = "https://www.wellsfargo.com/checking/";

        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );// Replace with path to ChromeDriver
       // System.setProperty ( "image-diff.path" , "C:\\Users\\nagar\\IdeaProjects\\image-diff.exe" ); // Replace with path to image-diff executable
        if ( ! new File ( OUTPUT_DIR ).exists ( ) ) {
            new File ( OUTPUT_DIR ).mkdirs ( );
        }

        boolean areSimilar = compareWebPages ( url1 , url2 , OUTPUT_DIR , THRESHOLD );

        if ( areSimilar ) {
            System.out.println ( "Web pages are visually similar (within the threshold)." );
        } else {
            System.out.println ( "Web pages are visually different." );
        }
    }

    public static boolean compareWebPages(String url1, String url2, String outputDir, double threshold) throws IOException, InterruptedException {
        WebDriver driver1 = null;
        WebDriver driver2 = null;
        try {
            // Initialize drivers
            driver1 = new ChromeDriver();
            driver2 = new ChromeDriver();

            driver1.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS); // Adjust timeout as needed
            driver2.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

            // Navigate to the URLs
            driver1.get(url1);
            driver2.get(url2);

            // Take screenshots
            File screenshot1 = ((TakesScreenshot) driver1).getScreenshotAs(OutputType.FILE);
            File screenshot2 = ((TakesScreenshot) driver2).getScreenshotAs(OutputType.FILE);

            File filepath1 = new File(outputDir + "/page1.png");
            File filepath2 = new File(outputDir + "/page2.png");
            FileUtils.copyFile(screenshot1, filepath1);
            FileUtils.copyFile(screenshot2, filepath2);


            // Compare using external tool (image-diff) - Preferred approach
            double differencePercentage =  compareImagesWithLooksSame(filepath1.getAbsolutePath(), filepath2.getAbsolutePath());


            if (differencePercentage <= threshold) {
                return true;
            } else {
                System.out.println("Difference percentage: " + String.format("%.2f", differencePercentage * 100) + "%");
                return false;
            }

        } finally {
            // Quit drivers in the finally block
            if (driver1 != null) {
                driver1.quit();
            }
            if (driver2 != null) {
                driver2.quit();
            }
        }
    }

    // Helper methods

    private static double compareImagesWithLooksSame(String filepath1, String filepath2) throws IOException, InterruptedException {
        // Execute image-diff command
        ProcessBuilder processBuilder = new ProcessBuilder(
                "C:\\Users\\nagar\\npm-global\\pixelmatch.cmd",
                "--threshold", "0.05",  // Adjust as needed
                filepath1,
                filepath2
        );


        processBuilder.redirectErrorStream(true); // Redirect error stream to standard output
        Process process = processBuilder.start();

        // Capture the output (difference percentage)
        java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
        String output = s.hasNext() ? s.next() : "";
        process.waitFor();
        int exitCode = process.exitValue();
        if(exitCode != 0){
            System.err.println("image-diff command failed with exit code: " + exitCode);
            System.err.println("Output: " + output);
            return 1.0; //Consider as 100% difference in case of error
        }


        try {
            return Double.parseDouble(output.trim());  // Parse the output to a double (the difference percentage)
        } catch (NumberFormatException e) {
            System.err.println("Error parsing image-diff output: " + output);
            return 1.0; // Return 1.0 (100% different) if parsing fails.
        }
    }

    //Alternative Method using Java, slower and less reliable
    private static double compareImagesJava(String filepath1, String filepath2) throws IOException {
        File file1 = new File(filepath1);
        File file2 = new File(filepath2);

        BufferedImage img1 = ImageIO.read(file1);
        BufferedImage img2 = ImageIO.read(file2);

        int width1 = img1.getWidth();
        int height1 = img1.getHeight();
        int width2 = img2.getWidth();
        int height2 = img2.getHeight();

        if (width1 != width2 || height1 != height2) {
            System.err.println("Images have different dimensions.  Cannot compare accurately.");
            return 1.0; // Treat as completely different.
        }

        long diff = 0;
        for (int y = 0; y < height1; y++) {
            for (int x = 0; x < width1; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = rgb1 & 0xff;
                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = rgb2 & 0xff;
                diff += Math.abs(r1 - r2);
                diff += Math.abs(g1 - g2);
                diff += Math.abs(b1 - b2);
            }
        }

        double totalPixels = width1 * height1 * 3;
        double avgDiff = diff / totalPixels;

        return avgDiff / 255; // Normalize to 0-1 range.
    }



    public static boolean comparePageContent(String url1, String url2, By elementLocator) {
        WebDriver driver1 = null;
        WebDriver driver2 = null;
        try {
            driver1 = new ChromeDriver();
            driver2 = new ChromeDriver();

            driver1.get(url1);
            driver2.get(url2);

            WebElement element1 = driver1.findElement(elementLocator);
            WebElement element2 = driver2.findElement(elementLocator);

            String text1 = element1.getText().trim();
            String text2 = element2.getText().trim();

            return text1.equals(text2);
        } finally {
            if (driver1 != null) {
                driver1.quit();
            }
            if (driver2 != null) {
                driver2.quit();
            }
        }
    }

}