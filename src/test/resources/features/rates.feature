Feature: Copy rates tables to Excel

  Scenario: Copy rates tables from Wells Fargo to Excel
    Given I open the web page "https://www.wellsfargo.com/savings-cds/rates/"
    When I enter the zip code "94105"
    And I find all rates tables on the web page
    Then I copy all rates tables into an Excel file
   ## Examples:
    ##/*url
   ##https://www.wellsfargo.com/savings-cds/rates/
    ##*/