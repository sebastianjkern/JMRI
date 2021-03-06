package jmri.jmrix.loconet.sdfeditor;

import jmri.util.JUnitUtil;

import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 */
public class TwoByteMacroEditorTest {

    @Test
    public void testCTor() {
        TwoByteMacroEditor t = new TwoByteMacroEditor(new jmri.jmrix.loconet.sdf.TwoByteMacro(1,2));
        Assert.assertNotNull("exists",t);
    }

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }

    // private final static Logger log = LoggerFactory.getLogger(TwoByteMacroEditorTest.class);

}
