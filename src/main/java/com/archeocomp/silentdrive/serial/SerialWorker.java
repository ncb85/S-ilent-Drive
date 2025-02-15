/*
 * serial worker
 */
package com.archeocomp.silentdrive.serial;

import com.archeocomp.silentdrive.DiskEmulator;
import com.archeocomp.silentdrive.utils.ByteUtils;
import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * serial worker
 * Protocol is:
 * :RTXXSXX[BYTES..128]   to read one record
 * :WTXXSXX[BYTES..128]Z@ to write one record (Z is checksum)
 */
@Component
public class SerialWorker {
	
	private static Logger LOG = LoggerFactory.getLogger(SerialWorker.class);
	
	private static final char COLON = ':';
	private static final String SERIAL_COMMAND_SPEC = "CTbSb"; // b is a binary 8 bit number
	private static final char READ_COMMAND = 'R';
	private static final char WRITE_COMMAND = 'W';
	private static final char ERROR_RESPONSE = 'E';
	private static final char ACKNOWLEDGE = '@';
	private static final int COLON_POS = 0;
	private static final int COMMAND_POS = 0;
	private static final int TRACK_POS = 2;
	private static final int SECTOR_POS = 4;
	
	@Autowired
	private SerialUtil serialUtil;
	@Autowired
	private DiskEmulator diskEmulator;
	
	private boolean working = false;
	
	@Async
	public void work() {
		LOG.info("Serial worker started");
		if (working == true) {
			LOG.warn("worker runs already, returning");
			return;
		}
		working = true;
		try {
			SerialPort serialPort = serialUtil.getActiveCommPort();
			while (serialPort.isOpen()) {
				byte[] readData = serialUtil.readSerialPort(1);
				if (readData != null && readData[0] != 0) {
					// check COLON
					//char p = (char)readData[COLON_POS];
					int readByte = Byte.toUnsignedInt(readData[COLON_POS]);

					switch (readByte) {
						case COLON:
							readData = serialUtil.readSerialPort(SERIAL_COMMAND_SPEC.length());
							// write or read command?
							readByte = Byte.toUnsignedInt(readData[COMMAND_POS]);
							// track and sector number
							int trackNo = Byte.toUnsignedInt(readData[TRACK_POS]);
							int sectorNo = Byte.toUnsignedInt(readData[SECTOR_POS]);
							switch (readByte) {
								case READ_COMMAND:
									readCommand(trackNo, sectorNo);
									break;
								case WRITE_COMMAND:
									writeCommand(trackNo, sectorNo);
									break;
								default:
									LOG.error("unknown command byte!");
									break;
							}	break;
						case ERROR_RESPONSE:
							LOG.error("ERROR response from CP/M machine!");
							break;
						default:
							LOG.error(String.format("Unknown command from CP/M machine! %d", readByte));
							break;
					}
				}
			}
		} catch(Exception ex) {
			LOG.error("work exeption", ex);
		} finally {
			working = false;
		}
	}
	
	private void readCommand(int trackNo, int sectorNo) {
		LOG.info("read: " + trackNo + ", " + sectorNo);
		byte[] sectorData = diskEmulator.getSectorData(trackNo, sectorNo);
		serialUtil.writeSerialPort(sectorData);
	}
	
	private void writeCommand(int trackNo, int sectorNo) {
		LOG.info("write: " + trackNo + ", " + sectorNo);
		byte[] readData = serialUtil.readSerialPort(DiskEmulator.SECTOR_LEN + 1); // plus checksum
		//LOG.info(HexUtils.dumpBytes(readData));
		readData = ByteUtils.checksumBytes(readData);		// return data bytes only
		diskEmulator.setSectorData(trackNo, sectorNo, readData);
		// send acknowledge
		serialUtil.writeSerialPort(new byte[] {ACKNOWLEDGE});
	}
	
}
