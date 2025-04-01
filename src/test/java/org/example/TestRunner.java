package org.example;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "C:/Users/nagar/ideaProjects/DataValidationRates/src/test/resources/features/SuperScriptValidation.feature", glue = "classpath:org.example"

)
public class TestRunner {
}