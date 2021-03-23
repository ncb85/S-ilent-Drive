/*
 * serial util
 */
package com.archeocomp.silentdrive.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * enumerate/open/read/write/close serial port(s)
 */
@Component
public class SerialUtil {
	
	private static Logger LOG = LoggerFactory.getLogger(SerialUtil.class);
	
	private static final int TIMEOUT = 5000;

	@Value("${silent-drive.serial.baudrate}")
	private int baudrate;
	@Value("${silent-drive.serial.databits}")
	private int databits;
	@Value("${silent-drive.serial.stopbits}")
	private int stopbits;
	@Value("${silent-drive.serial.parity}")
	private int parity;
	
	private SerialPort activeCommPort = null;
	
	@PreDestroy
	private void close() {
		if (getActiveCommPort() != null && getActiveCommPort().isOpen()) {
			getActiveCommPort().closePort();
			LOG.info(String.format("Serial port %s closed", getActiveCommPort().getSystemPortName()));
		}
	}

	public List<SerialPort> getSerialPorts() {
		List<SerialPort> serialPorts = Arrays.asList(SerialPort.getCommPorts());
		return serialPorts;
	}
	
	public void openSerialPortNonBlocking(SerialPort comPort) {
		if (comPort != null) {
			comPort.openPort();
			comPort.setComPortParameters(baudrate,
					databits, stopbits,
					parity);
			comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
					TIMEOUT, TIMEOUT);
			LOG.info(String.format("Serial port %s open. Baud rate %d, data bits %d, stop bits %d, parity %d" +
					"\nRead buffer size %d, write buffer size %d",
					comPort.getSystemPortName(),
					comPort.getBaudRate(), comPort.getNumDataBits(),
					comPort.getNumStopBits(), comPort.getParity(),
					comPort.getDeviceReadBufferSize(),
					comPort.getDeviceWriteBufferSize()));
		}
	}

	public byte[] readSerialPort(int numberOfBytes) {
		byte[] readBuffer = null;
		if (getActiveCommPort() != null) {
			while(getActiveCommPort().bytesAvailable() < numberOfBytes); // wait for data from serial
			readBuffer = new byte[numberOfBytes];
			int numRead = getActiveCommPort().readBytes(readBuffer, numberOfBytes);
			LOG.debug(String.format("readSerialPort read %d bytes", numRead));
		}
		
		return readBuffer;
	}
	
	public void writeSerialPort(byte[] writeBuffer) {
		if (getActiveCommPort() != null) {
			int numWritten = getActiveCommPort().writeBytes(writeBuffer, writeBuffer.length);
			LOG.debug(String.format("writeSerialPort written %d bytes", numWritten));
		}
	}

	/**
	 * @return the activeCommPort
	 */
	public SerialPort getActiveCommPort() {
		return activeCommPort;
	}

	/**
	 * @param activeCommPort the activeCommPort to set
	 */
	public void setActiveCommPort(SerialPort activeCommPort) {
		this.activeCommPort = activeCommPort;
	}
	
}
