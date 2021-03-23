/*
 * disk parameters
 */
package com.archeocomp.silentdrive.cpm;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * disk parameters, as found in disk parameter block DPB
 */
public class DiskParameters2k implements DiskParameters {
	private final Integer bsh = 4;
	private final Integer dsm = 639;
	private final Integer drm = 63;
	private final Integer off = 0;
	private final Integer spt = 128;

	public Integer getBsh() {
		return bsh;
	}

	public Integer getDsm() {
		return dsm+1;
	}
       
	public Integer getDrm() {
		return drm+1;
	}
       
	public Integer getOff() {
		return off;
	}

	public Integer getSpt() {
		return spt;
	}
       
}
