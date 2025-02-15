/*
 * single directory entry
 */
package com.archeocomp.silentdrive.cpm;

import com.archeocomp.silentdrive.exception.ParseException;
import com.archeocomp.silentdrive.utils.ByteUtils;
import com.archeocomp.silentdrive.utils.ListUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CP/M directory entry is a simple structure. It is 32 bytes long. The very
 * first byte is the status of the directory. An empty entry has a status byte
 * of 0E5h. If CP/M sees this byte, it considers the directory entry available,
 * and will create an entry there. The remainder of the directory entry
 * structure does not have to be created ahead of time, we only need to put OE5h
 * in the first byte of the 64 directory entries on each disk.
 *
 * Bytes: 0 - drive number, E5 - empty entry 1-8 - file name, right padded with
 * space 9-11 - file extension, highest bits mean: Read Only file, System file,
 * Archive 12 - extent number, 0..31H, bits 0..4 13,14 - S1 - system reserved,
 * S2 - 0..6 extent number higher bits, bit 7 - system for FCB 15 - record count
 * (128 byte records) 16 - 31 - allocation block numbers, can by one byte or two
 * byte, when max number of allocation blocks is > 256
 */
public class DirEntry {

    private static Logger LOG = LoggerFactory.getLogger(DirEntry.class);

    public static final byte EMPTY_SECTOR_BYTE = (byte) 0xE5;
    public static final int FILENAME_LEN = 8;
    public static final int FILESUFFIX_LEN = 3;

    public static final int DIR_ENTRY_LEN = 32;
    public static final int POS_USER = 0;
    public static final int POS_FILENAME = 1;
    public static final int POS_SUFFIX = 9;
    public static final int POS_EXTENT = 12;
    public static final int POS_S1 = 13;
    public static final int POS_S2 = 14;
    public static final int POS_RC = 15;
    public static final int POS_ALLOCATION_BLOCKS = 16;

    int index;
    private byte[] dirbuffer = new byte[32];
    private String filename;
    private int extentNumber;
    private int recordCount;
    private List<Integer> allocationBlockList;
    private boolean isBigDisk = false;
    private boolean readOnly = false;
    private boolean systemFile = false;
    private boolean archived = false;

    public DirEntry(int index, boolean isBigDisk) {
		this.index = index;
        this.isBigDisk = isBigDisk;
    }

    /**
     * create dir entry from 32 bytes
     *
     * @param bytes
     * @return dir entry
     */
    public DirEntry createFromBytes(byte[] bytes) {
        if (bytes.length != DIR_ENTRY_LEN) {
            throw new ParseException("incorrect number of bytes for a directory entry");
        }
		dirbuffer = Arrays.copyOfRange(bytes, 0, DIR_ENTRY_LEN);
        // empty entry?
        if (EMPTY_SECTOR_BYTE == bytes[POS_USER]) {
            clear();
            return this;
        }
        // filename
        extractFilename(bytes);
        // extent
        extentNumber = bytes[POS_EXTENT] & 0x1F;
        extentNumber += (bytes[POS_S2] & 0x7F) << 5;
        recordCount = Byte.toUnsignedInt(bytes[POS_RC]);
        // allocation blocks
        byte[] allocationBlocks = Arrays.copyOfRange(bytes, POS_ALLOCATION_BLOCKS, DIR_ENTRY_LEN);
        if (isBigDisk) {
            allocationBlockList = ByteUtils.convertWordsToIntList(allocationBlocks);
        } else {
            allocationBlockList = ByteUtils.convertBytesToIntList(allocationBlocks);
        }
        allocationBlockList.removeIf(intVal -> ((intVal == EMPTY_SECTOR_BYTE) || (intVal == 0)));

        return this;
    }

    /**
     * clear dir entry
     */
    public void clear() {
        dirbuffer[POS_USER] = EMPTY_SECTOR_BYTE;					// mark entry as unused
        allocationBlockList = new ArrayList<>();	// no alloc.blocks used
        filename = null;
        recordCount = 0;
    }

    /**
     * extract filename
     *
     * @param bytes
     * @param dirEntry
     */
    private void extractFilename(byte[] bytes) {
        byte[] filenameBytes = Arrays.copyOfRange(bytes, POS_FILENAME, POS_FILENAME + FILENAME_LEN);
        for (int i = 0; i < 8; i++) {
            filenameBytes[i] &= 0x7F;
        }
        filename = new String(filenameBytes);
        filename = filename.trim() + ".";
        byte[] suffix = Arrays.copyOfRange(bytes, POS_SUFFIX, POS_SUFFIX + FILESUFFIX_LEN);
        for (int i = 0; i < 3; i++) {
            int s = suffix[i];
            int p = Byte.toUnsignedInt(suffix[i]);
            if (s != p) {
                switch (i) {
                    case 0:
                        readOnly = true;
                        break;
                    case 1:
                        systemFile = true;
                        break;
                    case 2:
                        archived = true;
                        break;
                }
            }
            suffix[i] &= 0x7F;
        }
        filename += new String(suffix).trim();
    }

	/**
	 * modifies disk bytes and returns dir entry as byte array
	 * @param diskBytes
	 * @return 
	 */
    public byte[] makeDiskBytes(byte[] diskBytes) {
		if (isUsed()) {
			// filename
			filenameToByteArray(filename, dirbuffer);
			// extent
			dirbuffer[POS_EXTENT] = (byte) (extentNumber & 0x1F);
			dirbuffer[POS_S1] = (byte) 0;
			dirbuffer[POS_S2] = (byte) ((extentNumber >> 5) & 0x7F);
			dirbuffer[POS_RC] = (byte) recordCount;
			// allocation blocks - initialize all buffer bytes to zeroes
			ByteUtils.convertIntListToBytes(ListUtils.getListOf(0, DIR_ENTRY_LEN-POS_ALLOCATION_BLOCKS), dirbuffer, POS_ALLOCATION_BLOCKS, false);
			// allocation blocks - overwrite buffer zeroes with allocated bock positions
			ByteUtils.convertIntListToBytes(allocationBlockList, dirbuffer, POS_ALLOCATION_BLOCKS, isBigDisk);
		}
		// copy to disk bytes
		System.arraycopy(dirbuffer, 0, diskBytes,
					index * DirEntry.DIR_ENTRY_LEN, DirEntry.DIR_ENTRY_LEN);
		return dirbuffer;
    }

    private void filenameToByteArray(String filename, byte[] buffer) {
        String[] filenameParts = filename.split("\\.");
        copyBytes(String.format("%1$-" + FILENAME_LEN + "s", filenameParts[0]), buffer, POS_FILENAME);
        copyBytes(String.format("%1$-" + FILESUFFIX_LEN + "s", filenameParts[1]), buffer, POS_SUFFIX);
    }

    private void copyBytes(String str, byte[] buffer, int destPosition) {
        byte[] bytes = str.getBytes();
        System.arraycopy(bytes, 0, buffer, destPosition, str.length());
    }

    /**
     * is this directory entry used?
     *
     * @return true if used
     */
    public boolean isUsed() {
        return dirbuffer[POS_USER] != EMPTY_SECTOR_BYTE;
    }

    /**
     * entry is used when user is 0..15
     * @return the user
     */
    public int getUser() {
        return Byte.toUnsignedInt(dirbuffer[POS_USER]);
    }

    /**
     * mark entry as used by a user
     * @param user the user to set
     */
    public void setUser(int user) {
        dirbuffer[POS_USER] = Integer.valueOf(user).byteValue();
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the extentNumber
     */
    public int getExtentNumber() {
        return extentNumber;
    }

    /**
     * @param extentNumber the extentNumber to set
     */
    public void setExtentNumber(int extentNumber) {
        this.extentNumber = extentNumber;
    }

    /**
     * @return the length
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * @param length the length to set
     */
    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    /**
     * @return the allocationBlockList
     */
    public List<Integer> getAllocationBlockList() {
        return allocationBlockList;
    }

    /**
     * @param allocationBlockList the allocationBlockList to set
     */
    public void setAllocationBlockList(List<Integer> allocationBlockList) {
        this.allocationBlockList = allocationBlockList;
    }

    /**
     * @return the readOnly
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @return the systemFile
     */
    public boolean isSystemFile() {
        return systemFile;
    }

    /**
     * @return the archived
     */
    public boolean isArchived() {
        return archived;
    }
}
