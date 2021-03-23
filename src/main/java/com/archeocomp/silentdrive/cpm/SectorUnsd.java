/*
 * one sector
 */
package com.archeocomp.silentdrive.cpm;


/**
 * one sector
 */
public class SectorUnsd {

	private byte[] sectorData;

	/**
	 * creates sector of given length
	 * @param size sector length
	 */
	public SectorUnsd(int size) {
		sectorData = new byte[size];
	}
	
	/**
	 * @return the sectorData
	 */
	public byte[] getSectorData() {
		return sectorData;
	}

	/**
	 * @param sectorData the sectorData to set
	 */
	public void setSectorData(byte[] sectorData) {
		this.sectorData = sectorData;
	}
}
