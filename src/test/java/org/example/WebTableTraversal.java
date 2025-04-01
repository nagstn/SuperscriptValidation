package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

public class WebTableTraversal {

    WebDriver driver;

    public WebTableTraversal() {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\nagar\\IdeaProjects\\chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
    }

    public void openWebPage(String url) {
        driver.get(url);
    }

    public void traverseTable(String tableXPath) {
        WebElement table = driver.findElement(By.xpath(tableXPath));
        List<WebElement> rows = table.findElements(By.tagName("tr"));

        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            for (WebElement cell : cells) {
                System.out.println(cell.getText());
            }
        }
    }

    public void closeBrowser() {
        driver.quit();
    }

    public static void main(String[] args) {
        WebTableTraversal webTableTraversal = new WebTableTraversal();
        webTableTraversal.openWebPage("https://www.wellsfargo.com/savings-cds/platinum/");
        webTableTraversal.traverseTable("//div[@class='table-row center columnheadingrow']");
        webTableTraversal.closeBrowser();
    }
}
