package org.myPack;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/*
public class PageObjects {

 private WebDriverWait wait;*/
    //
/*    public static String pageTitleED = "Everyday Checking";
    public static String pageTitleCABA= "Choose a bank account";
    public static String pageTitleCAB = "Everyday Checking";
    public String pageTitleSTC = "Student and Teen Checking";
    public static String SavingsAccontsLanding = "Savings Accounts and CDs";
    public static String CheckingAccountsLanding = "Checking Accounts";
    public static String zipcodeNum = "94070";*/

   // --- Example structure for PageObjects.java ---

   public class PageObjects {

      private WebDriver driver;
      private WebDriverWait wait;

      // Assuming pageTitle2 corresponds to an element holding the main title/header
      @FindBy(xpath = "  //h1[@id='skip']") // <-- *** USE CORRECT LOCATOR FOR YOUR PAGE TITLE ELEMENT ***
      private WebElement pageTitleElement; // Rename for clarity maybe?
      /*private String pageTitleED = "Everyday Checking";
      private String pageTitleCABA= "Choose a bank account";
      private String pageTitleCAB = "Everyday Checking";
      private String pageTitleSTC = "Student and Teen Checking";
      private String SavingsAccontsLanding = "Savings Accounts and CDs";
      private String CheckingAccountsLanding = "Checking Accounts";
      private int ExpectedTotalSupportsED  = 14;
      private int ExpectedTotalSupportsCABA = 14;
      private int ExpectedTotalSupportsCAB = 14;
      private int ExpectedTotalSupportsSTC = 14;
      private int ExpectedTotalSupportsSAL = 14;
      private int ExpectedTotalSupportsSAC = 14;*/
      // Define locators for your expected titles if they are elements
      // @FindBy(id="expectedTitleIdED")
      // private WebElement pageTitleEDElement;
      // ... etc ...

      // Define locators for modal elements if managing them here
      @FindBy(xpath = "//button[@id='tab-mostpopular']")
      WebElement tabMostPopular;
      @FindBy(xpath = "//button[@id='tab-overdraftfeefree']")
      WebElement tabOverdraftFeeFree;
      @FindBy(xpath = "//button[@id='tab-teenandstudent']")
      WebElement tabTeenAndStudent;
      @FindBy(xpath = "//button[@id='tab-premiumaccounts']")
      WebElement tabPremiumAccounts;
      @FindBy(xpath = "//button[@id='tab-platinumsavings']")
      private WebElement tabPlatinumSavings;
      @FindBy(xpath = "//button[@id='tab-certificatesofdeposit']")
      private WebElement tabCertificatesOfDeposit;

      @FindBy(xpath = "//div[@class='zip-code-modal']")
      private WebElement zipCodeModal;

      @FindBy(xpath = "//div[@class='zip-code-modal']//input[@aria-labelledby='zip-code-label']") // More specific
      private WebElement zipCodeInput;

      @FindBy(xpath = "//div[@class='zip-code-modal']//input[@value='Continue']") // More specific
      private WebElement zipCodeSubmitButton;

      @FindBy(xpath = "//div[@id='c28lightbox']")
      private WebElement c28Lightbox;

      @FindBy(xpath = "//div[@id='c28lightbox']//input[@id='zipCode']")
      private WebElement c28ZipInput;

      @FindBy(xpath = "//div[@id='c28lightbox']//input[@value='Continue']")
      private WebElement c28SubmitButton;

      @FindBy(xpath = "//h1[@id='skip']")
      WebElement ActualCHECKING_ACCOUNTS;
      @FindBy(xpath = "//h1[@id='skip']")
      WebElement   ActualSAVINGS_ACCOUNTS;;

      public PageObjects ( WebDriver driver ) {
         if ( driver == null ) {
            throw new IllegalArgumentException ( "WebDriver instance cannot be null for PageObjects." );
         }
         this.driver = driver;
         this.wait = new WebDriverWait ( driver , Duration.ofSeconds ( 4 ) ); // Default wait
         // *** Initialize elements AFTER driver is assigned ***
         PageFactory.initElements ( driver , this );
         System.out.println ( "PageObjects Constructor: PageFactory initialized." );
      }

      // --- Methods to interact safely ---
      // Constructor remains the same


      // --- Existing methods (getActualPageTitleText, modal handlers, expandAllCollapsibleHeaders) ---
      // ... (keep them as they are) ...


      // --- NEW: Method to handle Checking Account Tabs ---
      public void clickAndVerifyCheckingTabs () {
         System.out.println ( "--- Processing Checking Account Tabs ---" );
         // Create a list of the checking tab WebElements
         // Ensure these elements are correctly located by the @FindBy annotations above
         List<WebElement> checkingTabs = Arrays.asList (
                 tabMostPopular ,
                 tabOverdraftFeeFree ,
                 tabTeenAndStudent ,
                 tabPremiumAccounts
         );

         for (WebElement tab : checkingTabs) {
            // Pass the specific tab WebElement to the helper method
            clickAndVerifyTab ( tab );
         }
         System.out.println ( "--- Finished Processing Checking Account Tabs ---" );
      }

      // --- NEW: Method to handle Savings Account Tabs ---
      public void clickAndVerifySavingsTabs () {
         System.out.println ( "--- Processing Savings Account Tabs ---" );
         // Create a list of the savings tab WebElements
         List<WebElement> savingsTabs = Arrays.asList (
                 tabMostPopular ,
                 tabPlatinumSavings ,
                 tabCertificatesOfDeposit
         );

         for (WebElement tab : savingsTabs) {
            // Pass the specific tab WebElement to the helper method
            clickAndVerifyTab ( tab );
         }
         System.out.println ( "--- Finished Processing Savings Account Tabs ---" );
      }


      // --- NEW: Reusable Helper Method to Click and Verify a Single Tab ---
      private void clickAndVerifyTab ( WebElement tabElement ) {
         String tabIdentifier = getElementIdentifier ( tabElement ); // Helper to get ID or Label for logging
         System.out.println ( "Processing Tab: " + tabIdentifier );

         try {
            // 1. Wait for tab to be clickable and scroll if needed
            wait.until ( ExpectedConditions.elementToBeClickable ( tabElement ) );
            ((JavascriptExecutor) driver).executeScript ( "arguments[0].scrollIntoView({block: 'center'});" , tabElement );
            Thread.sleep ( 200 ); // Short pause after scroll

            // 2. Click the tab (try JS click first for robustness)
            try {
               System.out.println ( "Attempting JS click on tab: " + tabIdentifier );
               ((JavascriptExecutor) driver).executeScript ( "arguments[0].click();" , tabElement );
            } catch (Exception e) {
               System.out.println ( "JS click failed for tab " + tabIdentifier + ", trying standard click. Error: " + e.getMessage ( ) );
               // Wait again before standard click fallback
               wait.until ( ExpectedConditions.elementToBeClickable ( tabElement ) ).click ( );
            }
            System.out.println ( "Clicked Tab: " + tabIdentifier );
            Thread.sleep ( 1000 ); // Pause for content to start loading

            // 3. Verification: Wait for associated content panel to appear
            //    *** THIS IS CRITICAL and requires you to define how panels are identified ***
            //    Example: Assume panel ID is related to tab ID (e.g., tab 'tab-foo' reveals div 'panel-foo')
            String panelId = null;
            String tabId = tabElement.getAttribute("id");
            if (tabId != null && !tabId.isEmpty()) {
               // Replace "tab-" with "panel-" or use a similar convention
               panelId = tabId.replaceFirst("tab-", "panel-");
            } else {
               // Fallback: Use aria-controls if available
               panelId = tabElement.getAttribute("aria-controls");
            }

            if (panelId == null || panelId.isEmpty()) {
               System.out.println("WARNING: Cannot determine content panel ID for tab: " + tabIdentifier + ". Skipping content verification.");
               return; // Skip verification if panel cannot be identified
            }/*
            String panelId = "";
            String tabId = tabElement.getAttribute ( "id" );
            if ( tabId != null && ! tabId.isEmpty ( ) ) {
               // Simple convention: replace "tab-" with "panel-" or similar
               panelId = tabId.replaceFirst ( "tab-?" , "panel-" ); // Adjust replacement logic
               // Alternative: Use aria-controls if available
               // String controlsId = tabElement.getAttribute("aria-controls");
               // if (controlsId != null && !controlsId.isEmpty()) panelId = controlsId;
            } else {
               // Fallback if no ID - difficult to verify reliably without more info
               System.out.println ( "WARNING: Cannot determine content panel ID for tab: " + tabIdentifier + ". Skipping content verification." );
               return; // Skip verification if panel cannot be identified
            }*/

            if ( ! panelId.isEmpty ( ) ) {
               By panelLocator = By.id ( panelId );
               // By panelLocator = By.xpath("//div[@aria-labelledby='" + tabId + "']"); // Example using aria-labelledby

               System.out.println ( "Waiting for content panel: " + panelId );
               try {
                  // Wait for the panel itself to be present/visible
                  wait.until ( ExpectedConditions.visibilityOfElementLocated ( panelLocator ) );
                  System.out.println ( "VERIFIED: Content panel '" + panelId + "' is visible for tab '" + tabIdentifier + "'." );
                  // You could add more specific checks here (e.g., check for specific text within the panel)
               } catch (TimeoutException te) {
                  System.err.println ( "VERIFICATION FAILED: Timed out waiting for content panel '" + panelId + "' after clicking tab '" + tabIdentifier + "'." );
                  // Decide how to handle failure: Assert.fail? Log only?
               }
            }

         } catch (NoSuchElementException nsee) {
            System.err.println ( "Error processing tab: Element not found initially for identifier: " + tabIdentifier + ". Might be timing or locator issue." );
         } catch (ElementClickInterceptedException ecie) {
            System.err.println ( "Error processing tab: Click intercepted for " + tabIdentifier + ". Check for overlays or sticky elements." );
         } catch (Exception e) {
            System.err.println ( "Error processing tab '" + tabIdentifier + "': " + e.getMessage ( ) );
            // Print stack trace for unexpected errors during tab processing
            e.printStackTrace ( );
         }
      }

      // --- NEW: Helper to get a meaningful identifier for logging ---
      private String getElementIdentifier ( WebElement element ) {
         String id = element.getAttribute ( "id" );
         if ( id != null && ! id.isEmpty ( ) ) return "ID:" + id;
         String label = element.getAttribute ( "aria-label" );
         if ( label != null && ! label.isEmpty ( ) ) return "Label:" + label;
         String text = element.getText ( );
         if ( text != null && ! text.isEmpty ( ) )
            return "Text:" + text.substring ( 0 , Math.min ( text.length ( ) , 20 ) ) + "...";
         return element.getTagName ( ); // Fallback
      }

      public String getActualPageTitleText() {
         try {
            // Wait for the title element defined by @FindBy
            WebElement visibleTitle = wait.until(ExpectedConditions.visibilityOf(pageTitleElement));
            return visibleTitle.getText();
         } catch (Exception e) {
            System.err.println("Error getting page title text: " + e.getMessage());
            return null; // Return null or throw exception on failure
         }
      }

      public boolean isZipCodeModalDisplayed(Duration timeout) {
         try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOf(zipCodeModal));
            return true;
         } catch (Exception e) {
            return false;
         }
      }

      public void enterZipAndSubmitInModal(String zip) {
         try {
            wait.until(ExpectedConditions.visibilityOf(zipCodeInput)).sendKeys(zip);
            wait.until(ExpectedConditions.elementToBeClickable(zipCodeSubmitButton)).click();
            // Wait for invisibility AFTER clicking submit
            wait.until(ExpectedConditions.invisibilityOf(zipCodeModal));
         } catch (Exception e) {
            System.err.println("Error interacting with zip code modal: " + e.getMessage());
            // Decide how to handle - throw exception?
         }
      }

      public boolean isc28LightboxDisplayed(Duration timeout) {
         try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOf(c28Lightbox));
            return true;
         } catch (Exception e) {
            return false;
         }
      }

      public void enterZipAndSubmitInC28(String zip) {
         try {
            wait.until(ExpectedConditions.visibilityOf(c28ZipInput)).sendKeys(zip);
            wait.until(ExpectedConditions.elementToBeClickable(c28SubmitButton)).click();
            // Wait for invisibility AFTER clicking submit
            wait.until(ExpectedConditions.invisibilityOf(c28Lightbox));
         } catch (Exception e) {
            System.err.println("Error interacting with c28 lightbox: " + e.getMessage());
            // Decide how to handle - throw exception?
         }
      }

      // Method to expand headers (now part of PageObjects)
      public void expandAllCollapsibleHeaders() {
         // Find headers that are currently collapsed
         List<WebElement> collapsibleHeaders = driver.findElements(By.xpath("//summary[@aria-expanded='false']"));
         System.out.println("Found " + collapsibleHeaders.size() + " collapsed headers to expand.");

         if (collapsibleHeaders.isEmpty()) {
            System.out.println("No collapsed headers found.");
            return;
         }

         for (WebElement header : collapsibleHeaders) {
            String headerIdentifier = header.getAttribute("aria-controls"); // Use a stable identifier for logging
            if (headerIdentifier == null || headerIdentifier.isEmpty()) {
               // Fallback if aria-controls is not present
               try { headerIdentifier = header.getText().substring(0, Math.min(header.getText().length(), 10)) + "..."; } catch (Exception ignored) { headerIdentifier = "[header]"; }
            }

            try {
               // --- SOLUTION 1: Try JavaScript Click First (Most Reliable) ---
               System.out.println("Attempting JavaScript click on header: " + headerIdentifier);
               ((JavascriptExecutor) driver).executeScript("arguments[0].click();", header);
               System.out.println("JavaScript click successful for header: " + headerIdentifier);

               // Add a short pause AFTER the click for the expansion animation/content loading
               Thread.sleep(10); // Adjust timing if necessary

            } catch (Exception e1) {
               System.err.println("JavaScript click failed for header: " + headerIdentifier + ". Error: " + e1.getMessage() + ". Trying standard click with scroll...");

               // --- SOLUTION 2: Try Standard Click with Smart Scrolling ---
               try {
                  // Scroll element into view, aligned to bottom (true = top, false = bottom)
                  // Aligning to bottom often helps avoid top sticky headers.
                  System.out.println("Scrolling header into view (align bottom): " + headerIdentifier);
                  ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(false);", header);
                  Thread.sleep(5); // Pause after scrolling

                  // Wait briefly for clickability AFTER scrolling
                  WebElement clickableHeader = new WebDriverWait(driver, Duration.ofSeconds(3))
                          .until(ExpectedConditions.elementToBeClickable(header));

                  System.out.println("Attempting standard click on header: " + headerIdentifier);
                  clickableHeader.click();
                  System.out.println("Standard click successful for header: " + headerIdentifier);
                  Thread.sleep(5); // Pause after click

               } catch (ElementClickInterceptedException eIntercept) {
                  // Specifically catch the interception error after trying scroll+standard click
                  System.err.println("Standard click STILL intercepted for header: " + headerIdentifier + ". Blocker likely persistent. Error: " + eIntercept.getMessage());
                  // You could log more details about the intercepting element if needed from eIntercept.getRawMessage()

               } catch (Exception e2) {
                  // Catch other errors during scroll/wait/standard click
                  System.err.println("Error during standard click attempt for header: " + headerIdentifier + ". Error: " + e2.getMessage());
               }
            }
         }
         System.out.println("Finished attempting to expand headers.");
      }

      /* public int getExpectedTotalSupports () {
           return ExpectedTotalSupports;
       }

       public void setExpectedTotalSupports ( int expectedTotalSupports ) {
           ExpectedTotalSupports = expectedTotalSupports;
       }

       public int getExpectedTotalSupportsED () {
           return ExpectedTotalSupportsED;
       }

       public void setExpectedTotalSupportsED ( int expectedTotalSupportsED ) {
           ExpectedTotalSupportsED = expectedTotalSupportsED;
       }

       public int getExpectedTotalSupportsCABA () {
           return ExpectedTotalSupportsCABA;
       }

       public void setExpectedTotalSupportsCABA ( int expectedTotalSupportsCABA ) {
           ExpectedTotalSupportsCABA = expectedTotalSupportsCABA;
       }

       public int getExpectedTotalSupportsCAB () {
           return ExpectedTotalSupportsCAB;
       }

       public void setExpectedTotalSupportsCAB ( int expectedTotalSupportsCAB ) {
           ExpectedTotalSupportsCAB = expectedTotalSupportsCAB;
       }

       public int getExpectedTotalSupportsSTC () {
           return ExpectedTotalSupportsSTC;
       }

       public void setExpectedTotalSupportsSTC ( int expectedTotalSupportsSTC ) {
           ExpectedTotalSupportsSTC = expectedTotalSupportsSTC;
       }

       public int getExpectedTotalSupportsSAL () {
           return ExpectedTotalSupportsSAL;
       }

       public void setExpectedTotalSupportsSAL ( int expectedTotalSupportsSAL ) {
           ExpectedTotalSupportsSAL = expectedTotalSupportsSAL;
       }

       public int getExpectedTotalSupportsSAC () {
           return ExpectedTotalSupportsSAC;
       }

       public void setExpectedTotalSupportsSAC ( int expectedTotalSupportsSAC ) {
           ExpectedTotalSupportsSAC = expectedTotalSupportsSAC;
       }

       public String getCheckingAccountsLanding () {
           return CheckingAccountsLanding;
       }

       public void setCheckingAccountsLanding ( String checkingAccountsLanding ) {
           CheckingAccountsLanding = checkingAccountsLanding;
       }

       public String getSavingsAccontsLanding () {
           return SavingsAccontsLanding;
       }

       public void setSavingsAccontsLanding ( String savingsAccontsLanding ) {
           SavingsAccontsLanding = savingsAccontsLanding;
       }

       public String getPageTitleSTC () {
           return pageTitleSTC;
       }

       public void setPageTitleSTC ( String pageTitleSTC ) {
           this.pageTitleSTC = pageTitleSTC;
       }

       public String getPageTitleCAB () {
           return pageTitleCAB;
       }

       public void setPageTitleCAB ( String pageTitleCAB ) {
           this.pageTitleCAB = pageTitleCAB;
       }

       public String getPageTitleCABA () {
           return pageTitleCABA;
       }

       public void setPageTitleCABA ( String pageTitleCABA ) {
           this.pageTitleCABA = pageTitleCABA;
       }

       public String getPageTitleED () {
           return pageTitleED;
       }

       public void setPageTitleED ( String pageTitleED ) {
           this.pageTitleED = pageTitleED;
       }*/

       // ... other methods for your page ...
   }