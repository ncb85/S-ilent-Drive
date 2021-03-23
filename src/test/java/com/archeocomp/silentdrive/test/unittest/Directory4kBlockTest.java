/*
 * directory test
 */
package com.archeocomp.silentdrive.test.unittest;

import com.archeocomp.silentdrive.DiskEmulatorImpl;
import com.archeocomp.silentdrive.cpm.Directory;
import com.archeocomp.silentdrive.cpm.DiskParameters4k;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * directory test - 44 files A: ASM COM : DDT COM : DUMP COM : ED COM A: LOAD
 * COM : PIP COM : STAT COM : SUBMIT COM A: XSUB COM : CPM REF : CPM22 ASM :
 * MBASIC COM A: LIB COM : LINK COM : MAC COM : FDC COM A: LUNAR BAS : SUNUP TXT
 * : IDE COM : BENCH BAS A: XM5 COM : XM5V2 COM : PPIP COM : PPIP DOC A: DIRX
 * COM : SUPERSUB COM : UNZIP COM : UNARCA COM A: UNCR8080 COM : MUART COM : IIC
 * COM : ENV COM A: CLCK COM : MORGANA PIC : GIRLZ PIC : CATCHUM COM A: CATCHUM
 * DAT : CATCONF COM : CP SUB : DPB BAS A: CPC3 SUB : CPC SUB : LADCONF COM :
 * LADDER COM
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DiskEmulatorImpl.class, Directory.class,
     DiskParameters4k.class})
@SpringBootTest
public class Directory4kBlockTest {

    @Autowired
    private Directory directory;

    @BeforeEach
    public void beforeEachTest() {
    }

    @Test
    public void testExtentNoA() {
        int noZero = directory.getExtentNo(0);
        Assertions.assertEquals(1, noZero);
        int noOne = directory.getExtentNo(1);
        Assertions.assertEquals(3, noOne);
	}
	
    @Test
    public void testExtentNoB() {
        int noTwo = directory.getExtentNo(2);
        Assertions.assertEquals(5, noTwo);
        int noThree = directory.getExtentNo(3);
        Assertions.assertEquals(7, noThree);
    }

    @Test
    public void testExtentNoC() {
        int noTen = directory.getExtentNo(10);
        Assertions.assertEquals(21, noTen);
    }

}
