/*
 * scheduler class
 */
package com.archeocomp.silentdrive;

import com.archeocomp.silentdrive.gui.SilentDriveFrameImpl;
import com.archeocomp.silentdrive.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * scheduler class
 */
@Component
public class Scheduler {
	
	private static Logger LOG = LoggerFactory.getLogger(Scheduler.class);
	
	@Autowired
	private SilentDriveFrameImpl frame;
	@Autowired
	private FileUtils fileUtils;
	
	//@Scheduled(cron = "2 0 0 0 * ?")
	@Scheduled(fixedDelay = 5000)
	public void scheduleFixedDelayTask() {
		frame.refresh();
		if (frame.isAutoImportChecked()) {
			fileUtils.importInDir();
		}
	}
	
}
