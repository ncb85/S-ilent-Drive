/*
 * a cp/m disk directory
 */
package com.archeocomp.silentdrive.cpm;

import com.archeocomp.silentdrive.DiskEmulator;
import com.archeocomp.silentdrive.exception.ParseException;
import com.archeocomp.silentdrive.utils.ByteUtils;
import com.archeocomp.silentdrive.utils.ListUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * disk directory
 */
@Component
public class Directory {

    private static Logger LOG = LoggerFactory.getLogger(Directory.class);

    @Autowired
    private DiskParameters diskParameters;
    @Autowired
    private DiskEmulator diskEmulator;

    int totalNumberOfDiskAllocationBlocks;
    int totalNumberOfDiskDirectoryEntries;
    private boolean isBigDisk = false;  // allocation block address is 8(false) or 16 bit(true)
    private List<DirEntry> dirEntries;

    public void postConstruct() {
        totalNumberOfDiskAllocationBlocks = diskParameters.getDsm();
        isBigDisk = totalNumberOfDiskAllocationBlocks > DiskEmulator.BYTE_VAL;
        totalNumberOfDiskDirectoryEntries = diskParameters.getDrm();
        dirEntries = IntStream.range(0, totalNumberOfDiskDirectoryEntries)
                .mapToObj(no -> new DirEntry(no, isBigDisk))
                .collect(Collectors.toList());

        LOG.info("directory size: " + dirEntries.size());
    }

	/**
	 * init directory from byte[] array
	 * @param directoryBytes 
	 */
    public void init(byte[] directoryBytes) {
        // check array size
        int expetedByteArraySize = diskParameters.getDrm() * DirEntry.DIR_ENTRY_LEN;
        if (directoryBytes == null || directoryBytes.length != expetedByteArraySize) {
            throw new ParseException("input byte array has incorrect size, expected: "
                    + expetedByteArraySize + ", but was: " + (directoryBytes == null ? "null" : directoryBytes.length));
        }
        // parse bytes and create dir entries
        IntStream.range(0, totalNumberOfDiskDirectoryEntries)
                .forEach((value) -> {
                    dirEntries.get(value).createFromBytes(
                            Arrays.copyOfRange(directoryBytes,
                                    value * DirEntry.DIR_ENTRY_LEN, (value + 1) * DirEntry.DIR_ENTRY_LEN));
                });
    }
    
    /**
     * transforms natural order 0,1,2,3,.. to extent No as implemented in CP/M
     * @param extentPosition
     * @return 
     */
    public int getExtentNo(int extentPosition) {
        int exm = diskParameters.getBsh() - (isBigDisk ? 1 : 0) - 3;
        return ((extentPosition + 1) * (1 << exm)) - 1;
    }

    /**
     * return number of allocation addresses per extent - 16 bytes for addresses available
     * @return 16 for 8bit allocation addressing, 8 for 16bit
     */
    public int getNumberOfAllocationBlocksPerExtent() {
        return isBigDisk ? 8 : 16;
    }
    
	/**
	 * return number of allocation blocks occupied by the directory itself
	 * @return 
	 */
    public int getNumberOfAllocationBlocksOccupiedByDirectoryItself() {
		int result = (dirEntries.size() * DirEntry.DIR_ENTRY_LEN) / diskEmulator.getAllocationBlockSize();
		int mod = (dirEntries.size() * DirEntry.DIR_ENTRY_LEN) % diskEmulator.getAllocationBlockSize();
		return result + mod;
	}
	
    /**
     * quick format - erase directory
     */
    public void eraseDirectory() {
		LOG.debug("eraseDirectory");
        byte[] formattedEntryBytes = new byte[DirEntry.DIR_ENTRY_LEN];
        List<Integer> listOfE5 = ListUtils.getListOf(Byte.toUnsignedInt(DirEntry.EMPTY_SECTOR_BYTE), DirEntry.DIR_ENTRY_LEN);
        ByteUtils.convertIntListToBytes(listOfE5, formattedEntryBytes, 0, false);
        dirEntries.stream()
                .forEach((entry) -> entry.createFromBytes(formattedEntryBytes).makeDiskBytes(diskEmulator.getDiskBytes()));
    }

    public List<CpmFile> getFileList() {
		//LOG.debug("getFileList");
        List<CpmFile> dirList = new ArrayList<>();
        for (DirEntry dirEntry : dirEntries) {
            if (dirEntry.isUsed()) {
                String filename = dirEntry.getFilename();
                CpmFile cpmFile = containsCpmFile(dirList, filename);
                if (cpmFile == null) {
                    cpmFile = new CpmFile(dirEntry);
                    dirList.add(cpmFile);
                }
                cpmFile.adjustFilesize(dirEntry.getRecordCount());
            }
        }

        return dirList;
    }
    
    private CpmFile containsCpmFile(final List<CpmFile> list, final String name) {
        return list.stream().filter(cpmFile -> cpmFile.getFilename().equals(name)).findFirst().orElse(null);
    }

	public boolean containsFile(String filename) {
		boolean empyAllocBlockList = getAllocationBlocksOfFile(filename.toUpperCase()).isEmpty();
		return !empyAllocBlockList;
	}
	
    /**
     * assign allocation free blocks (not yet referenced by directory) to a new
     * file, compute extent numbers and set them to dir entries, set entry user
     * @param file
	 * @param diskBytes
     * @return free blocks
     */
    public List<Integer> assignFreeAllocationBlocks(File file, byte diskBytes[]) {
		LOG.debug("assignFreeAllocationBlocks: " + file);
        List<Integer> usedBlocks = getUsedAllocationBlocks();
        int noOfReqBlocks = diskEmulator.getNumberOfRequiredBlocksForFile(file);
        // find free blocks
        List<Integer> freeBlocks = IntStream.range(
						getNumberOfAllocationBlocksOccupiedByDirectoryItself(),
						totalNumberOfDiskAllocationBlocks)
                .boxed()
                .filter(alBlckNr -> !usedBlocks.contains(alBlckNr))
                .limit(noOfReqBlocks)
                .collect(Collectors.toList());
		int numberOfRecords = getNumberOfRequiredRecordsForFile(file);
		int maxFilePartSizePerExtent = getMaxFilePartSizePerExtent();
		int numberOfAllocBlocksPerExtent = getNumberOfAllocationBlocksPerExtent();
        // assign them to dirEntry(s) (and set extent number(s))
		String fileName = file.getName().toUpperCase();
		for (int i=0, j=0; !freeBlocks.isEmpty() && i<dirEntries.size(); i++) {
            DirEntry dirEntry = dirEntries.get(i);
            if (!dirEntry.isUsed()) {
                dirEntry.setUser(DiskEmulator.DEFAULT_USER);    // now it is used
                dirEntry.setExtentNumber(getExtentNo(j++));
                dirEntry.setFilename(fileName);
				// when file spans multiple extents, last one contains only remainder of file
				dirEntry.setRecordCount(Math.min(numberOfRecords, maxFilePartSizePerExtent));
				numberOfRecords -= maxFilePartSizePerExtent;
                numberOfAllocBlocksPerExtent = Math.min(numberOfAllocBlocksPerExtent, freeBlocks.size());
                List<Integer> oneExtentBlocks = freeBlocks.subList(0, numberOfAllocBlocksPerExtent);
                dirEntry.getAllocationBlockList().addAll(oneExtentBlocks);
                freeBlocks.removeAll(oneExtentBlocks);
				byte[] entryBytes = dirEntry.makeDiskBytes(diskBytes); // create disk byte representation
            }
        }

        return getAllocationBlocksOfFile(fileName);
    }

	public List<Integer> releaseFileAllocationBlocks(CpmFile file) {
		LOG.debug("releaseFileAllocationBlocks: " + file);
		String filename = file.getFilename();
		return releaseFileAllocationBlocks(filename);
	}
	
	public List<Integer> releaseFileAllocationBlocks(File file) {
		LOG.debug("releaseFileAllocationBlocks: " + file);
		String filename = file.getName().toUpperCase();
		return releaseFileAllocationBlocks(filename);
	}
	
	public List<Integer> releaseFileAllocationBlocks(String filename) {
		LOG.debug("releaseFileAllocationBlocks: " + filename);
		List<Integer> releaseAllocatiobBlocks = dirEntries.stream()
                .filter(dirEntry -> dirEntry.isUsed())
                .filter(dirEntry -> filename.equalsIgnoreCase(dirEntry.getFilename()))
				.map(dirEntry -> {
					List<Integer> allocBlocks = dirEntry.getAllocationBlockList();
					dirEntry.clear();
					dirEntry.makeDiskBytes(diskEmulator.getDiskBytes()); // write to disk
					return allocBlocks.stream();
				})
				.flatMap(i -> i)
				.collect(Collectors.toList());
		return releaseAllocatiobBlocks;
	}
	
	public void notifyChange(int sectorNr) {
		int numberOfDirSectors = dirEntries.size() * DirEntry.DIR_ENTRY_LEN / DiskEmulator.SECTOR_LEN;
		if (sectorNr < numberOfDirSectors) {
			// refresh four entries (sector is 128 byte)
			int numberOfEntriesPerSector = DiskEmulator.SECTOR_LEN / DirEntry.DIR_ENTRY_LEN;
			int firstEntry = sectorNr * numberOfEntriesPerSector;
			for (int i=0; i<numberOfEntriesPerSector; i++) {
				int dirEntryNr = firstEntry + i;
				dirEntries.get(dirEntryNr).
						createFromBytes(Arrays.copyOfRange(diskEmulator.getDiskBytes(),
                                    dirEntryNr * DirEntry.DIR_ENTRY_LEN,
									(dirEntryNr + 1) * DirEntry.DIR_ENTRY_LEN));
			}
		}
	}
	
    /**
     * return number of used allocation blocks (referenced by directory)
     * @return number of used allocation blocks
     */
    public int getNumberOfUsedBlocks() {
		LOG.debug("getNumberOfUsedBlocks");
        int numberOfUsedBlocks = dirEntries.stream()
                .filter(dirEntry -> dirEntry.isUsed())
                .map(dirEntry -> dirEntry.getAllocationBlockList().size())
                .collect(Collectors.summingInt(Integer::intValue));
        return numberOfUsedBlocks;
    }

    /**
     * return used allocation blocks (referenced by directory)
     * @return allocation block numbers
     */
    public List<Integer> getUsedAllocationBlocks() {
		//LOG.debug("getUsedAllocationBlocks");
        List<Integer> usedBlocks = dirEntries.stream()
                .filter(dirEntry -> dirEntry.isUsed())
                .map(dirEntry -> dirEntry.getAllocationBlockList())
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return usedBlocks;
    }

    /**
     * return file's allocation blocks (referenced by directory)
     * @param filename
     * @return allocation block numbers
     */
    public List<Integer> getAllocationBlocksOfFile(String filename) {
		LOG.debug("getAllocationBlocksOfFile");
        List<Integer> usedBlocks = dirEntries.stream()
                .filter(dirEntry -> dirEntry.isUsed())
                .filter(dirEntry -> filename.equals(dirEntry.getFilename()))
                .map(dirEntry -> dirEntry.getAllocationBlockList())
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return usedBlocks;
    }

	/**
	 * return number of records ofr given file(size)
	 * @param file
	 * @return number of 128byte records
	 */
    public int getNumberOfRequiredRecordsForFile(File file) {
		LOG.debug("getNumberOfRequiredRecordsForFile: " + file);
        int filesize = (int) file.length();
        int noOfRecords = filesize / DiskEmulator.SECTOR_LEN;
        noOfRecords += (filesize % DiskEmulator.SECTOR_LEN == 0) ? 0 : 1;

        return noOfRecords;
    }
	
	/**
	 * how much of a file can we store in one extent (max number of 128byte records)
	 * per extent
	 * @return max number of records
	 */
	public int getMaxFilePartSizePerExtent() {
		LOG.debug("getMaxFilePartSizePerExtent");
		return getNumberOfAllocationBlocksPerExtent() *
				diskEmulator.getAllocationBlockSize() / DiskEmulator.SECTOR_LEN;
	}
	
    /**
     * @return the dirEntries
     */
    public List<DirEntry> getDirEntries() {
        return dirEntries;
    }

    /**
     * @param dirEntries the dirEntries to set
     */
    public void setDirEntries(List<DirEntry> dirEntries) {
        this.dirEntries = dirEntries;
    }

}
