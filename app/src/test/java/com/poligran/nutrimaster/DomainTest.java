package com.poligran.nutrimaster;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
public class DomainTest {
    @Test public void allDomainRules(){assertTrue(DomainChecks.runSuite()>50);}
}
