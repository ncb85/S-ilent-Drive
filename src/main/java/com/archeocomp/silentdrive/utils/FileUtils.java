/*
 * file utils
 */
package com.archeocomp.silentdrive.utils;

import com.archeocomp.silentdrive.DiskEmulator;
import com.archeocomp.silentdrive.cpm.CpmFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * file utils
 */
@Component
public class FileUtils {
	
	private static Logger LOG = LoggerFactory.getLogger(FileUtils.class);
	
	public static final String DEFAULT_DIR = "./default/";
	public static final String IN_DIR = "./in/";

	@Autowired
	private DiskEmulator diskEmulator;
	
	public void importInDir() {
		LOG.info("Importing IN dir");
		String path = IN_DIR;
		importDir(path);
		// delete files after import
		try {
			Files.list(Paths.get(path))
					.forEach(file -> file.toFile().delete());
		} catch (Exception ex) {
			LOG.error(String.format("Error deleting files in %s dir.", path), ex);
		}
	}
	
	public void importDefaultDir() {
		LOG.info("Importing DEFAULT dir");
		String path = DEFAULT_DIR;
		importDir(path);
	}
	
	public void importDir(String path) {
		LOG.info("Importing dir: " + path);
		try {
			Files.list(Paths.get(path))
				.forEach(file -> diskEmulator.addFile(file.toFile()));
		} catch (Exception ex) {
			LOG.error(String.format("Error importing %s dir.", path), ex);
		}
		int diskAllocationBlocks = diskEmulator.getTotalNumberOfAllocationBLocks();
		LOG.info("Total blocks:" + diskAllocationBlocks);
		LOG.info("Free blocks:" + diskEmulator.getNumberOfFreeBlocks());
		int numberOfUsedBlocks = diskAllocationBlocks - diskEmulator.getNumberOfFreeBlocks();
		LOG.info("Used blocks:" + numberOfUsedBlocks);
	}
	
	public void exportDisk() {
		List<CpmFile> cpmFileList = diskEmulator.getFileList();
		cpmFileList.forEach(cpmFile -> diskEmulator.saveFile(cpmFile));
	}
	
}
