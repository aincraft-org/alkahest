package org.bukkit;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ParticleTest.class, MaterialInitializationOrderTest.class})
public class ParticleCompatibilityTestSuite {
}
