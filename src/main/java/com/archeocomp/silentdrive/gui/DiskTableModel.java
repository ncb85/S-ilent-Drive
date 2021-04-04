/**
 * table model for CP/M disk drive
 */
package com.archeocomp.silentdrive.gui;

import javax.annotation.PostConstruct;
import javax.swing.table.AbstractTableModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.archeocomp.silentdrive.DiskEmulator;
import com.archeocomp.silentdrive.cpm.DirEntry;
import com.archeocomp.silentdrive.cpm.Directory;
import java.util.List;

/**
 * represents a CP/M disk drive view, each cell represents one allocation block
 */
@Component
public class DiskTableModel extends AbstractTableModel {
	
	private final static int COLUMNS = 32;
	
	@Autowired
	private DiskEmulator diskEmulator;
	@Autowired
	private Directory directory;
	
    private int diskAllocationBlocks = 0;
    private int dirAllocationBlocks = 0;
	private long numberOfAllEntries = 0L;

	@PostConstruct
	public void postConstruct() {
		diskAllocationBlocks = diskEmulator.getTotalNumberOfAllocationBLocks();
		dirAllocationBlocks = diskEmulator.getDirectoryOccupiesKB() * DiskEmulator.ONE_KB / diskEmulator.getAllocationBlockSize();
		numberOfAllEntries = directory.getDirEntries().size();
	}
	
    public int getRowCount() {
        return (diskAllocationBlocks / COLUMNS) +
				((diskAllocationBlocks % COLUMNS) > 0 ? 1 : 0);
    }

    public int getColumnCount() {
        return COLUMNS;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
		int index = rowIndex * COLUMNS + columnIndex;
		if (index >= diskEmulator.getTotalNumberOfAllocationBLocks()) {
			return AllocationBlockStateEnum.NOT_A_BLOCK.name();
		}
		if (index < dirAllocationBlocks) {
			List<DirEntry> dirEntriesList = directory.getDirEntries();
			long numberOfUsedEntries = dirEntriesList.stream()
					.filter(dirEntry -> dirEntry.isUsed())
					.count();
			long dirEntryUsage = dirAllocationBlocks * numberOfUsedEntries / numberOfAllEntries;
			long modUsage = dirAllocationBlocks * numberOfUsedEntries % numberOfAllEntries;
			String color = AllocationBlockStateEnum.DIRECTORY_EMPTY.name();
			if (index < dirEntryUsage) {
				color = AllocationBlockStateEnum.DIRECTORY_OCCUPIED.name();
			}
			if (index == dirEntryUsage && modUsage > 0) {
				color = AllocationBlockStateEnum.DIRECTORY_PARTIALLY_OCCUPIED.name();
			}
			return color;
		}
		
		List<Integer> usedAllocationBlockList = diskEmulator.getUsedAllocationBlocks();
		if (usedAllocationBlockList.contains(rowIndex *COLUMNS + columnIndex)) {
			return AllocationBlockStateEnum.FILE_OCCUPIED.name();
		} else {
			return AllocationBlockStateEnum.FILE_EMPTY.name();
		}
    }

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

}
