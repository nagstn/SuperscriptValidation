package org.myPack;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class PageObjects {
    @Override
    public String toString () {
       String ZipCodeNumber="97040";
        return super.toString ( );
    }
   //

    public WebElement footnote;
    @FindBy(xpath = "//button[@type='submit']")
    public WebElement loginButton;

    @FindBy(xpath = "//div[@class='zip-code-modal']")
    public WebElement zipCodeModal;

    @FindBy(xpath = "//input[@aria-labelledby='zip-code-label']")
    public WebElement zipCodeInput;

    @FindBy(xpath = "//input[@type='submit']")
    public WebElement zipCodeSubmit;

    public PageObjects(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }
}