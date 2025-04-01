
Feature: Verify Numeric Superscripts with Hyperlinks
  Scenario Outline: Verify superscript on web page and extract data
  Given I have a Chrome browser for Superscript extraction
  Given I navigate to the web page "<url>"
  When I handle any initial modal dialogues
  #Then I verify the presence of numeric superscripts with hyperlinks
  #Then I extract the text and URL from the superscript elements
    Then I extract superscript information and take screenshot
  Then  I close the browser for Superscript extraction
  Then I write results to an Excel file


    Examples:
      | url                                          |
      | https://www.wellsfargo.com/checking/prime/   |
      | https://www.wellsfargo.com/savings-cds/way2save/ |