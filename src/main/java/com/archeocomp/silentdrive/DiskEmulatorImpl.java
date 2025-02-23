/*
 * disk emulator
 */
package com.archeocomp.silentdrive;

import com.archeocomp.silentdrive.cpm.CpmFile;
import com.archeocomp.silentdrive.cpm.DirEntry;
import com.archeocomp.silentdrive.cpm.Directory;
import com.archeocomp.silentdrive.cpm.DiskParameters;
import com.archeocomp.silentdrive.exception.InvalidConfigurationException;
import com.archeocomp.silentdrive.exception.NotEnoughSpaceException;
import com.archeocomp.silentdrive.gui.SilentDriveFrame;
import com.archeocomp.silentdrive.utils.ByteUtils;
import static com.archeocomp.silentdrive.utils.FileUtils.OUT_DIR;
import com.archeocomp.silentdrive.utils.HexUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * disk emulator
 */
@Component
public class DiskEmulatorImpl implements DiskEmulator {

	private static final Logger LOG = LoggerFactory.getLogger(DiskEmulatorImpl.class);

	@Autowired
	private DiskParameters diskParameters;
	@Autowired
	private Directory directory;
	@Autowired
        @Lazy
	private SilentDriveFrame silentDriveFrame;

    private boolean isBigdisk = false;  // allocation block address is 8(false) or 16 bit(true)
    private int exm = -1;               // extent mask
    private int totalNumberOfDiskAllocationBlocks;
    private byte[] diskBytes;

    @PostConstruct
    public void postConstruct() {
        totalNumberOfDiskAllocationBlocks = diskParameters.getDsm();
        LOG.info("total drive capacity: " + getTotalDriveCapacity() * ONE_KB);
        diskBytes = new byte[getTotalDriveCapacity() * ONE_KB];
        isBigdisk = totalNumberOfDiskAllocationBlocks > BYTE_VAL;
        LOG.info("allocation block address is " + (isBigdisk ? 16 : 8) + " bit");
        // compute extent mask
        exm = diskParameters.getBsh() - (isBigdisk ? 1 : 0) - 3;
        if (exm < 0) {
            throw new InvalidConfigurationException("disk parameters invalid");
        }
        exm = (1 << exm) - 1;
        LOG.info("extent mask: " + exm);
		directory.postConstruct();
    }

    /**
     * compute extent mask, based on BLS and DSM
     * BLS     DSM<256 DSM>255
     * 1024    0       N/A
     * 2,048   1       0
     * 4,096   3       1
     * 8,192   7       3
     * 16,384  15      7
     * @return EXM
     */
    public int getExtentMask() {
        return exm;
    }

    @Override
    public void initDirectory(byte[] directoryBytes) {
        directory.init(directoryBytes);
    }

    @Override
    public List<CpmFile> getFileList() {
        return directory.getFileList();
    }

    @Override
    public void formatDisk() {
        directory.eraseDirectory();
    }

    @Override
    public int getNumberOfRequiredBlocksForFile(File file) {
        int filesize = (int) file.length();
        int noOfReqBlocks = filesize / getAllocationBlockSize();
        noOfReqBlocks += (filesize % getAllocationBlockSize() == 0) ? 0 : 1;

        return noOfReqBlocks;
    }

    @Override
    public void addFile(File file) {
		if (directory.containsFile(file.getName())) {
			LOG.warn("file already exists - deleting");
			deleteFile(file);
		}
        // check free space before copy
        int numberOfFreeAllocBlocks = getNumberOfFreeBlocks();
        int freeCapacityKb = numberOfFreeAllocBlocks * ((1 << diskParameters.getBsh()) * SECTOR_LEN) / ONE_KB;
        if (freeCapacityKb < (file.length() / ONE_KB)) {
            throw new NotEnoughSpaceException("Not enough free space on drive");
        }
        // assign dir entries and allocation blocks
        List<Integer> allocationBlocksForFile = directory.assignFreeAllocationBlocks(file, diskBytes);
        // read contents of file to byte array
        byte[] fileBytes = ByteUtils.fileToBytes(file);
        // copy contents of file to byte array slots corresponding to allocation blocks
        int allocationBlockSize = getAllocationBlockSize();
        ByteUtils.copyFileBytesToDisk(diskBytes, fileBytes, allocationBlockSize, allocationBlocksForFile);
    }

    @Override
    public List<Integer> getAllocationBlocksOfFile(File file) {
        String filename = file.getName();
        return directory.getAllocationBlocksOfFile(filename);
    }

    public List<Integer> getAllocationBlocksOfFile(CpmFile cpmFile) {
        String filename = cpmFile.getFilename();
        return directory.getAllocationBlocksOfFile(filename);
    }

    @Override
	public List<Integer> deleteFile(String filename) {
        return directory.releaseFileAllocationBlocks(filename);
	}
	
    @Override
	public List<Integer> deleteFile(CpmFile file) {
        return directory.releaseFileAllocationBlocks(file);
	}
	
    @Override
    public List<Integer> deleteFile(File file) {
        return directory.releaseFileAllocationBlocks(file);
    }

    @Override
    public int getLinearSectorNo(int trackNumber, int sectorNumber) {
        int linearSectorNo = trackNumber * diskParameters.getSpt();
        linearSectorNo += sectorNumber;
        return linearSectorNo;
    }

    @Override
	public List<Integer> getUsedAllocationBlocks() {
		return directory.getUsedAllocationBlocks();
	}
	
    @Override
    public int getNumberOfFreeBlocks() {
        int numberOfUsedBlocks = directory.getNumberOfUsedBlocks();
		int numberOfFreeBlocks = getTotalNumberOfAllocationBLocks() - numberOfUsedBlocks;
		numberOfFreeBlocks -= directory.getNumberOfAllocationBlocksOccupiedByDirectoryItself();
        return numberOfFreeBlocks;
    }

    @Override
    public int getTotalNumberOfAllocationBLocks() {
        return diskParameters.getDsm();
    }

    @Override
    public int getTotalNoOfSectors() {
        return (1 << diskParameters.getBsh()) * diskParameters.getDsm();
    }

    @Override
    public int getTotalDriveCapacity() {
        return (diskParameters.getDsm() * ((1 << diskParameters.getBsh()) * SECTOR_LEN)) / ONE_KB;
    }

    @Override
    public int getDirectoryOccupiesKB() {
        return (diskParameters.getDrm() * DirEntry.DIR_ENTRY_LEN) / ONE_KB;
    }

    @Override
    public int getAllocationBlockSize() {
        return (1 << diskParameters.getBsh()) * SECTOR_LEN;
    }

	@Override
	public String dumpDirEntry(int entryNo) {
		List<DirEntry> dirEntryList = directory.getDirEntries();
		DirEntry dirEntry = dirEntryList.get(entryNo);
		byte[] entryBytes = dirEntry.makeDiskBytes(diskBytes);
		return HexUtils.dumpBytes(entryBytes);
	}

    /**
     * Drive Characteristics Total no.of sectors: 10240, total drive capacity:
     * 1280 kB, directory occupies: 2 kB no.of directory entries: 64, sectors
     * per track: 128, cylinder offset: 0 no.of allocation blocks: 640, size of
     * allocation blocks: 2048
     *
     * TODO: 128: Checked Directory Entries 128: Records/ Extent
     */
    @Override
    public String getDriveStats() {
        String stats = "Total no.of sectors: " + getTotalNoOfSectors();
        stats += ", total drive capacity: " + getTotalDriveCapacity() + " kB";
        stats += ", directory occupies: " + getDirectoryOccupiesKB() + " kB";
        stats += ", no.of directory entries: " + diskParameters.getDrm();
        stats += ", sectors per track: " + diskParameters.getSpt();
        stats += ", cylinder offset: " + diskParameters.getOff();
        stats += ", no.of allocation blocks: " + diskParameters.getDsm();
        stats += ", size of allocation block: " + getAllocationBlockSize();
        stats += ", extent mask: " + exm;
        return stats;
    }

    @Override
    public void exportDisk() {
        List<CpmFile> fileList = getFileList();
		for (CpmFile cpmFile: fileList) {
			saveFile(cpmFile);
		}
    }
	
	@Override
	public void saveFile(CpmFile cpmFile) {
		int filesize = cpmFile.getFilesize() * SECTOR_LEN;
		List<Integer> allocationBlockList = getAllocationBlocksOfFile(cpmFile);
		File outFile = new File(OUT_DIR + "/" + cpmFile.getFilename());
		//Path pathToFile = Paths.get(filename);
		System.out.println(outFile.getAbsolutePath());
		System.out.println(outFile.canWrite());
		byte[] dataForWriting = ByteUtils.getFileBytesFromDisk(filesize,
				getAllocationBlockSize(), allocationBlockList, diskBytes);
		try {
			Files.write(outFile.toPath(), dataForWriting);
		} catch (IOException ex) {
			LOG.error("error saving: ", ex);
		}
	}

	@Override
	public byte[] getSectorData(int trackNumber, int sectorNumber) {
		int linearSectorNo = getLinearSectorNo(trackNumber, sectorNumber);
		byte[] sectorData = new byte[SECTOR_LEN];
		System.arraycopy(diskBytes, linearSectorNo * SECTOR_LEN, sectorData,
					0, SECTOR_LEN);
		silentDriveFrame.readLedOn();
		return sectorData;
	}

	@Override
	public void setSectorData(int trackNumber, int sectorNumber, byte[] sectorData) {
		int linearSectorNo = getLinearSectorNo(trackNumber, sectorNumber);
		System.arraycopy(sectorData, 0, diskBytes,
					linearSectorNo * SECTOR_LEN, SECTOR_LEN);

		directory.notifyChange(linearSectorNo);
		silentDriveFrame.writeLedOn();
	}
	
	@Override
	public byte[] getDiskBytes() {
		return this.diskBytes;
	}
	
}
