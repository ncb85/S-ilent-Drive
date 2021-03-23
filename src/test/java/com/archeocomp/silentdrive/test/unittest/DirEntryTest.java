/*
 * 
 */
package com.archeocomp.silentdrive.test.unittest;

import com.archeocomp.silentdrive.DiskEmulatorImpl;
import com.archeocomp.silentdrive.cpm.DirEntry;
import com.archeocomp.silentdrive.cpm.Directory;
import com.archeocomp.silentdrive.cpm.DiskParameters2k;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DiskEmulatorImpl.class, Directory.class,
     DiskParameters2k.class})
@SpringBootTest
public class DirEntryTest {

    private static DirEntry dirEntry;
    private static byte[] diskBytes;

    @BeforeAll
    public static void initSuite() {
    }

    @BeforeEach
    public void beforeEachTest() {
		diskBytes = new byte[1024];
        dirEntry = new DirEntry(0, false);
    }

    @AfterEach
    public void afterEachTest() {
    }

    @Test
    public void testCreateEntryFromBytes() {
        byte[] bytes = {(byte) 0x0F, (byte) 0x42, (byte) 0x41, (byte) 0x44, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0xC3, (byte) 0x52, (byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        dirEntry = dirEntry.createFromBytes(bytes);

        Assertions.assertEquals(0, dirEntry.getExtentNumber());
        Assertions.assertEquals("BAD.CRC", dirEntry.getFilename());
        Assertions.assertEquals(1, dirEntry.getRecordCount());
        Assertions.assertEquals(15, dirEntry.getUser());
        Assertions.assertTrue(dirEntry.isReadOnly());
        Assertions.assertFalse(dirEntry.isSystemFile());
        Assertions.assertFalse(dirEntry.isArchived());
    }

    @Test
    public void testCreateEntryFromBytes2() {
        byte[] bytes = {(byte) 0x05, (byte) 0x42, (byte) 0x41, (byte) 0x44, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x43, (byte) 0xD2, (byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        dirEntry = dirEntry.createFromBytes(bytes);

        Assertions.assertEquals(0, dirEntry.getExtentNumber());
        Assertions.assertEquals("BAD.CRC", dirEntry.getFilename());
        Assertions.assertEquals(1, dirEntry.getRecordCount());
        Assertions.assertEquals(5, dirEntry.getUser());
        Assertions.assertFalse(dirEntry.isReadOnly());
        Assertions.assertTrue(dirEntry.isSystemFile());
        Assertions.assertFalse(dirEntry.isArchived());
    }

    @Test
    public void testCreateEntryFromBytes3() {
        byte[] bytes = {(byte) 0x01, (byte) 0x42, (byte) 0x41, (byte) 0x44, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x43, (byte) 0x52, (byte) 0xC3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        dirEntry = dirEntry.createFromBytes(bytes);

        Assertions.assertEquals(0, dirEntry.getExtentNumber());
        Assertions.assertEquals("BAD.CRC", dirEntry.getFilename());
        Assertions.assertEquals(1, dirEntry.getRecordCount());
        Assertions.assertEquals(1, dirEntry.getUser());
        Assertions.assertFalse(dirEntry.isReadOnly());
        Assertions.assertFalse(dirEntry.isSystemFile());
        Assertions.assertTrue(dirEntry.isArchived());
    }

    @Test
    public void testCreateEntryFromBytes4() {
        byte[] bytes = {(byte) 0xE5, (byte) 0x42, (byte) 0x41, (byte) 0x44, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0xC3, (byte) 0x52, (byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        dirEntry = dirEntry.createFromBytes(bytes);

        Assertions.assertFalse(dirEntry.isUsed());
    }

    @Test
    public void testIncorrectBuffer() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            dirEntry = dirEntry.createFromBytes(new byte[]{(byte) 128, (byte) 25});
        });
    }

    @Test
    public void testEntryAsBytes1() {
        byte[] bytes = {(byte) 0x01, (byte) 0x42, (byte) 0x41, (byte) 0x44, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x43, (byte) 0x52, (byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        dirEntry = dirEntry.createFromBytes(bytes);

        byte[] buffer = dirEntry.makeDiskBytes(diskBytes);

        for (int i = 0; i < bytes.length; i++) {
            Assertions.assertEquals(bytes[i], buffer[i]);
        }
    }

}
