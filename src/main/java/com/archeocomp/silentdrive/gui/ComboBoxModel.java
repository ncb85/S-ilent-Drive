/**
 * combo box model for serialports
 */
package com.archeocomp.silentdrive.gui;

import com.archeocomp.silentdrive.serial.SerialUtil;
import com.archeocomp.silentdrive.serial.SerialWorker;
import com.fazecast.jSerialComm.SerialPort;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.swing.DefaultComboBoxModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * represents a serial port list
 */
@Component
public class ComboBoxModel extends DefaultComboBoxModel {
	
	private static Logger LOG = LoggerFactory.getLogger(ComboBoxModel.class);
	
	@Autowired
	private SerialWorker serialWorker;
	@Autowired
	private SerialUtil serialUtil;

	@PostConstruct
	private void postConstruct() {
		this.addElement("");
		serialUtil.getSerialPorts()
				.stream()
				.sorted((sp1, sp2) -> sp1.getSystemPortName().compareTo(sp2.getSystemPortName()))
				.forEach(serialPort -> this.addElement(serialPort.getSystemPortName()));
	}
	
	public void itemSelected(String portName) {
		LOG.info("Serial port selected: " + portName);
		SerialPort serialPort = serialUtil.getActiveCommPort();
		if (portName.isEmpty()) {
			// close port
			serialPort.closePort();
			LOG.info(String.format("Serial port %s closed", serialPort.getSystemPortName()));
		} else {
			// close opened port
			if (serialPort != null && serialPort.isOpen()) {
				serialPort.closePort();
				LOG.info(String.format("Serial port %s closed", serialPort.getSystemPortName()));
			}
			// open selected port
			serialPort = serialUtil.getSerialPorts()
					.stream()
					.filter(sp -> portName.equals(sp.getSystemPortName()))
					.findFirst().orElseThrow();
			serialUtil.setActiveCommPort(serialPort);
			serialUtil.openSerialPortNonBlocking(serialPort);
			LOG.info(String.format("Serial port %s opened", serialPort.getSystemPortName()));
			serialWorker.work();
		}
	}
}
