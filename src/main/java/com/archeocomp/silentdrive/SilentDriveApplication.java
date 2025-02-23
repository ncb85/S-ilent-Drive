package com.archeocomp.silentdrive;

import com.archeocomp.silentdrive.gui.SilentDriveFrameImpl;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesScan
@SpringBootApplication
@EnableAsync
@EnableScheduling
@ComponentScan("com.archeocomp.silentdrive")
public class SilentDriveApplication implements CommandLineRunner {
	
	private static Logger LOG = LoggerFactory.getLogger(SilentDriveApplication.class);
	
	@Autowired
	private SilentDriveFrameImpl frame;
	@Autowired
	private Scheduler scheduler; // start scheduler automatically
	
	private void createAndShowGui() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
	}
	
    @Override
    public void run(String... args) {
		LOG.info("EXECUTING : command line runner");
 
        for (int i = 0; i < args.length; ++i) {
            LOG.info("args[{}]: {}", i, args[i]);
        }
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGui();
			}
		});
		
		LOG.info("DONE : command line runner");
	}
	
	public static void main(String[] args) {
		LOG.info("STARTING S(ilent)Drive");
		new SpringApplicationBuilder(SilentDriveApplication.class)
                .headless(false)
                .run(args);
    }

}
