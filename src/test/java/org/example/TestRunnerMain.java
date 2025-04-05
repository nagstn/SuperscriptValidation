package org.example;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features/SuperscriptStepDefinitions.feature", // Path to your feature file
        glue = "org.myPack"
        //plugin = {"pretty", "html:target/cucumber-report.html"} // Optional: Report formatting
)
public class TestRunnerMain {
}

