package org.example;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WebPageComparatorA {

    private static final String OUTPUT_DIR = "comparison_results";
    private static final int PIXEL_DIFFERENCE_THRESHOLD = 100; //Adjust as needed

    public static void main(String[] args) throws IOException, InterruptedException {
        String url1 = "https://www.wellsfargo.com/checking/";  // Replace with your URLs
        String url2 = "https://www.wellsfargo.com/checking/";
        String elementId = "mostpopular";
        //WebElement element = driver.findElement(By.xpath("//div[@id='mostpopular']/div[@class='tabbed-product-table presentedElement']/div[@class='product-table-details']/div[@class='product-table-details-grid']/div[@class='product-table-rows'][2]/div[@class='product-table-value']/p/a[1]")); //Example element ID - Replace with appropriate locator
        System.setProperty ( "webdriver.chrome.driver" , "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe" );// Replace with path to ChromeDriver
        //System.setProperty("webdriver.chrome.driver", "/path/to/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        if (!new File(OUTPUT_DIR).exists()) {
            new File(OUTPUT_DIR).mkdirs();
        }

        boolean areSimilar = compareElementVisually(url1, url2, elementId, OUTPUT_DIR, PIXEL_DIFFERENCE_THRESHOLD);

        if (areSimilar) {
            System.out.println("Elements are visually similar (within the threshold).");
        } else {
            System.out.println("Elements are visually different.");
        }
    }

    public static boolean compareElementVisually(String url1, String url2, String elementId, String outputDir, int pixelDifferenceThreshold) throws IOException, InterruptedException {
        WebDriver driver1 = null;
        WebDriver driver2 = null;
        try {
            driver1 = new ChromeDriver(new ChromeOptions().addArguments("--headless"));
            driver2 = new ChromeDriver(new ChromeOptions().addArguments("--headless"));

            driver1.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

            driver1.get(url1);
            driver2.get(url2);

            WebElement element1 = driver1.findElement(By.id(elementId));
            WebElement element2 = driver2.findElement(By.id(elementId));

            File screenshot1 = element1.getScreenshotAs(OutputType.FILE);
            File screenshot2 = element2.getScreenshotAs(OutputType.FILE);

            File filepath1 = new File(outputDir + "/element1.png");
            File filepath2 = new File(outputDir + "/element2.png");
            FileUtils.copyFile(screenshot1, filepath1);
            FileUtils.copyFile(screenshot2, filepath2);

            int pixelDifference = compareImagesWithPixelmatch(filepath1.getAbsolutePath(), filepath2.getAbsolutePath(), outputDir + "/diff.png");

            System.out.println("Pixel difference: " + pixelDifference);

            return pixelDifference <= pixelDifferenceThreshold;

        } finally {
            if (driver1 != null) {
                driver1.quit();
            }
            if (driver2 != null) {
                driver2.quit();
            }
        }
    }

    private static int compareImagesWithPixelmatch(String filepath1, String filepath2, String diffFile) throws IOException, InterruptedException {
        // Requires pixelmatch globally installed: npm install -g pixelmatch
        // and pngjs: npm install -g pngjs

        ProcessBuilder processBuilder = new ProcessBuilder(
                "pixelmatch",
                filepath1,
                filepath2,
                diffFile,
                "--threshold", "0.1"  // Adjust threshold as needed
        );

        processBuilder.redirectErrorStream(true); // Redirect error stream to standard output
        Process process = processBuilder.start();

        // Capture the output (number of differing pixels)
        java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
        String output = s.hasNext() ? s.next() : "";
        process.waitFor();
        int exitCode = process.exitValue();
        if(exitCode != 0){
            System.err.println("pixelmatch command failed with exit code: " + exitCode);
            System.err.println("Output: " + output);
        }

        try {
            return Integer.parseInt(output.trim()); // pixelmatch outputs the number of differing pixels
        } catch (NumberFormatException e) {
            System.err.println("Error parsing pixelmatch output: " + output);
            return Integer.MAX_VALUE;  // Return a large value to indicate failure
        }
    }

}
