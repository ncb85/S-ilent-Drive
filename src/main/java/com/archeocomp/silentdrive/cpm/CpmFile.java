/*
 * one CP/M file
 */
package com.archeocomp.silentdrive.cpm;

import com.archeocomp.silentdrive.DiskEmulator;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * one CP/M file
 */
public class CpmFile {

    private String filename;
    private int filesize;
    private boolean readOnly = false;
    private boolean system = false;
    private boolean archived = false;

    public CpmFile(DirEntry dirEntry) {
        this.filename = dirEntry.getFilename();
        this.readOnly = dirEntry.isReadOnly();
        this.system = dirEntry.isArchived();
        this.archived = dirEntry.isArchived();
        this.filesize = 0;
    }

    public int adjustFilesize(int numberOfRecords) {
        this.filesize += numberOfRecords;
        return filesize;
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
     * number of 128 byte records
     *
     * @return the filesize
     */
    public int getFilesize() {
        return filesize;
    }

    /**
     * @param filesize the filesize to set
     */
    public void setFilesize(int filesize) {
        this.filesize = filesize;
    }

    /**
     * @return the readOnly
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @param readOnly the readOnly to set
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * @return the system
     */
    public boolean isSystem() {
        return system;
    }

    /**
     * @param system the system to set
     */
    public void setSystem(boolean system) {
        this.system = system;
    }

    /**
     * @return the archived
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * @param archived the archived to set
     */
    public void setArchived(boolean archived) {
        this.archived = archived;
    }
	
	@Override
	public String toString() {
		float kBytes = filesize * DiskEmulator.SECTOR_LEN / (float)DiskEmulator.ONE_KB;
		BigDecimal size = new BigDecimal(kBytes).setScale(2, RoundingMode.HALF_UP);
		return filename + " " + size + "kB";
	}

}
