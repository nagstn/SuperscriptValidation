Feature: Verify Numeric Superscripts with Hyperlinks
  Scenario Outline: Verify superscript on page and extract data
    Given I have a Chrome browser
    When I navigate to "<url>"
    Then I verify the page and extract superscripts
    Then I write results to an Excel file

    Examples:
      | url                                          |
      | https://www.wellsfargo.com/checking/prime/   |
      | https://www.wellsfargo.com/savings-cds/way2save/ |