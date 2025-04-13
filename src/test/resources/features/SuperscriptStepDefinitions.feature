Feature: Verify Numeric Superscripts with Hyperlinks and Line Position

  Scenario Outline: Verify Superscripts on Web Page
    Given I have a Chrome browser for Superscript extraction
    Given I navigate to the web page "<url>"
    #Given Validate the total number of superscripts "<totalSuperscripts>"
    When I handle any initial modal dialogues
    Then I extract superscript information and take screenshot
    Then I write results to an Excel file
    Then I close the browser for Superscript extraction

    Examples:
      | url | totalSuperscripts
      | https://www.wellsfargo.com/checking/prime/ | 16
    #  | https://www.wellsfargo.com/checking/premier/                                  | 15
    #  | https://www.wellsfargo.com/savings-cds/way2save/                              | 5
    #  | https://www.wellsfargo.com/savings-cds/platinum/                              | 6
    #  | https://www.wellsfargo.com/savings-cds/certificate-of-deposit/                | 6
    #  | https://www.wellsfargo.com/savings-cds/rates/                                 | 4
    #  | https://www.wellsfargo.com/savings-cds/certificate-of-deposit/apply/          | 6
    #  | https://www.wellsfargo.com/investing/retirement/ira/select/destination/rates/ | 4
    #  | https://www.wellsfargo.com/checking/everyday// | 3
  #  | https://www.wellsfargo.com/checking/ | 3