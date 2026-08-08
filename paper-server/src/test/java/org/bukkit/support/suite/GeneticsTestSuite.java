package org.bukkit.support.suite;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite(failIfNoTests = false)
@SuiteDisplayName("mintychochip genetics façade and hook structural tests")
@IncludeTags("Normal")
@SelectPackages({"dev.mintychochip.genetics"})
@ConfigurationParameter(key = "TestSuite", value = "Normal")
public class GeneticsTestSuite {
}
