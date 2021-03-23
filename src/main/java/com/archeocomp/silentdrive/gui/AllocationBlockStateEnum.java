/*
 * allocation block state enum
 */
package com.archeocomp.silentdrive.gui;

/**
 * allocation block state enum
 */
public enum AllocationBlockStateEnum {
	FILE_EMPTY(0xFFDCDC),
    FILE_OCCUPIED(0xFF7878),
    DIRECTORY_EMPTY(0xDCFFDC),
    DIRECTORY_PARTIALLY_OCCUPIED(0xA8FFA8),
    DIRECTORY_OCCUPIED(0x32CD32),
	NOT_A_BLOCK(0xC8C8C8); // this just fill up view space
	
	private final int stateColorRGB;

    AllocationBlockStateEnum(int stateColorRGB) {
        this.stateColorRGB = stateColorRGB;
    }
    
    public int getStateColorRGB() {
        return this.stateColorRGB;
    }
}
