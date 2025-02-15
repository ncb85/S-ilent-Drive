/*
 * disk parameters
 */
package com.archeocomp.silentdrive.cpm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * disk parameters, as found in disk parameter block DPB
 */
@Validated
@ConfigurationProperties(prefix = "silent-drive.disk")
public class DiskParametersImpl implements DiskParameters {

    private final Integer bsh;
    private final Integer dsm;
    private final Integer drm;
    private final Integer off;
    private final Integer spt;

    public DiskParametersImpl(Integer bsh, Integer dsm, Integer drm, Integer off, Integer spt) {
        this.bsh = bsh;
        this.dsm = dsm;
        this.drm = drm;
        this.off = off;
        this.spt = spt;
    }

    /**
     * Block shift. 3 => 1k, 4 => 2k, 5 => 4k...
     *
     * @return bsh
     */
    public Integer getBsh() {
        return bsh;
    }

    /**
     * (no. of blocks on the disc)-1
     *
     * @return dsm+1
     */
    public Integer getDsm() {
        return dsm + 1;
    }

    /**
     * (no. of directory entries)-1
     *
     * @return drm+1
     */
    public Integer getDrm() {
        return drm + 1;
    }

    /**
     * Offset, number of reserved tracks
     *
     * @return offset
     */
    public Integer getOff() {
        return off;
    }

    /**
     * Number of 128-byte records per track
     *
     * @return the spt
     */
    public Integer getSpt() {
        return spt;
    }

}
