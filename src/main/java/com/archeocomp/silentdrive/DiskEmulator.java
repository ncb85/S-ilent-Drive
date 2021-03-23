/*
 * disk emulator interface
 */
package com.archeocomp.silentdrive;

import com.archeocomp.silentdrive.cpm.CpmFile;
import java.io.File;
import java.util.List;

/**
 * disk emulator interface
 */
public interface DiskEmulator {

    public static final int DEFAULT_USER = 0;
    public static final int SECTOR_LEN = 128;
    public static final int BYTE_VAL = 256;
    public static final int ONE_KB = 1024;

    public void formatDisk();
    public void exportDisk();
    public void initDirectory(byte[] directoryBytes);
    public void addFile(File file);
	public List<Integer> deleteFile(String filename);
    public List<Integer> deleteFile(File file);
	public List<Integer> deleteFile(CpmFile file);
    public void saveFile(CpmFile cpmFile);
	public List<Integer> getUsedAllocationBlocks();
    public int getNumberOfFreeBlocks();
    public List<CpmFile> getFileList();
    public int getNumberOfRequiredBlocksForFile(File file);
    public List<Integer> getAllocationBlocksOfFile(File file);
    public int getTotalNumberOfAllocationBLocks();
    public String getDriveStats();
    public int getTotalNoOfSectors();
    public int getTotalDriveCapacity();
    public int getDirectoryOccupiesKB();
    public int getAllocationBlockSize();
    public int getLinearSectorNo(int trackNumber, int sectorNumber);
	public byte[] getSectorData(int trackNumber, int sectorNumber);
	public void setSectorData(int trackNumber, int sectorNumber, byte[] sectorData);
	public String dumpDirEntry(int entryNo);
	public byte[] getDiskBytes();
	public void postConstruct();
}
