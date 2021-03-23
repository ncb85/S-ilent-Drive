/**
 * directory list model for CP/M disk drive
 */
package com.archeocomp.silentdrive.gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.archeocomp.silentdrive.DiskEmulator;
import com.archeocomp.silentdrive.cpm.CpmFile;
import java.util.List;
import javax.swing.DefaultListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * represents directory list view of CP/M disk , each line represent a CP/M file
 */
@Component
public class DirListModel<E> extends DefaultListModel {
	
	private static Logger LOG = LoggerFactory.getLogger(DirListModel.class);
	@Autowired
	private DiskEmulator diskEmulator;
	
	public void refresh() {
		this.clear();
		List<CpmFile> fileList = diskEmulator.getFileList();
		this.addAll(fileList);
		//LOG.info("number of files: " + this.getSize());
	}

}
