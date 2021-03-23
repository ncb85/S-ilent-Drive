/*
 * 
 */
package com.archeocomp.silentdrive.test.unittest;

import com.archeocomp.silentdrive.utils.ByteUtils;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
/**
 *
 */
public class ByteUtilsTest {
 
    @BeforeAll
    public static void initSuite() {
    }
 
    @BeforeEach
    public void beforeEachTest() {
    }
 
    @AfterEach
    public void afterEachTest() {
    }
 
    @Test
    public void testCreateEntryFromBytes() {
		byte[] bytes = {(byte)0x0F,(byte)0x42,(byte)0x41,(byte)0x44,(byte)0x20,(byte)0x20,(byte)0x20,(byte)0x20,
			(byte)0x20,(byte)0xC3,(byte)0x52,(byte)0x43,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,
			(byte)0xAA,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
			(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        List<Integer> intList = ByteUtils.convertBytesToIntList(bytes);
 
        Assertions.assertEquals((byte)15, intList.get(0));
		Assertions.assertEquals((byte)195, intList.get(9));
    }
 
    @Test
    public void testWordsToIntList() throws Exception {
		byte[] bytes = {(byte)0x0F,(byte)0x42,(byte)0x41,(byte)0x44,(byte)0x20,(byte)0x20};
		List<Integer> intList = ByteUtils.convertWordsToIntList(bytes);
		assertTrue(intList.size() == 3);
		Assertions.assertEquals(0x420F, intList.get(0));
    }
 
}
