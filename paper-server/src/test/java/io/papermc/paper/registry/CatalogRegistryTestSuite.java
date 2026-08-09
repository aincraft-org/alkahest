package io.papermc.paper.registry;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({CatalogRegistryTest.class, CatalogNativeBoundaryTest.class, MergedRegistryTest.class})
public class CatalogRegistryTestSuite {
}
