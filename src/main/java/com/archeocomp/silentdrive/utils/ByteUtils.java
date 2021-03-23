/*
 * byte (array) utils
 */
package com.archeocomp.silentdrive.utils;

import com.archeocomp.silentdrive.DiskEmulator;
import com.archeocomp.silentdrive.exception.CheckSumException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * byte (array) utils
 */
public class ByteUtils {
	
	private static Logger LOG = LoggerFactory.getLogger(ByteUtils.class);
	
	public static Integer convertWordToInt(byte[] data) {
		if (data == null || data.length != 2) {
			return null;
		}
		return Integer.valueOf((0xFF & data[1]) << 8 | (0xFF & data[0]));
	}
	
	/**
	 * converts array of bytes to a list of ints, where each byte is converted to an int
	 * @param bytes
	 * @return 
	 */
	public static List<Integer> convertBytesToIntList(byte[] bytes) {
		final List<Integer> list = new ArrayList<>();
		for (byte b : bytes) {
			list.add(Integer.valueOf(b)); // FIXME use Byte.toUnsi..
		}
		return list;
	}
	
	/**
	 * converts array of words (byte,byte) to list a of int, where words (two bytes)
	 * are converted to an int
	 * @param bytes
	 * @return 
	 */
	public static List<Integer> convertWordsToIntList(byte[] bytes) {
		final List<Integer> list = new ArrayList<>();
		for (int i=0; i<bytes.length; i+=2) {
			list.add(convertWordToInt(Arrays.copyOfRange(bytes,i, i+2)));
		}
		return list;
	}
	
	/**
	 * copy list of Integers into byte array buffer, making conversion
	 * @param intList
	 * @param buffer
	 * @param destPosition
	 * @param wordAlign convert to words if true, to bytes otherwise
	 * @return byte array
	 */
	public static byte[] convertIntListToBytes(List<Integer> intList, byte[] buffer,
			int destPosition, boolean wordAlign) {
		for (int i=0; i<intList.size(); i++) {
			Integer value = intList.get(i);
			if (!wordAlign) {
				buffer[i+destPosition] = value.byteValue();
			} else {
				buffer[i*2 + destPosition] = value.byteValue();
				buffer[i*2 + destPosition + 1] = Integer.valueOf(value / 256).byteValue();
			}
		}
		return buffer;
	}
	
	/**
	 * read and convert a file to byte array
	 * @param file
	 * @return file bytes
	 */
	public static byte[] fileToBytes(File file) {
		Path path = file.toPath();
		LOG.debug("reading file: " + path.toString());
		byte[] byteArray = null;
		try {
			byteArray = java.nio.file.Files.readAllBytes(path);
		} catch (IOException ex) {
			LOG.error("Coud not read file: ", ex);
		}
		return byteArray;
	}
	
	/**
	 * copy file bytes to allocated blocks in disk buffer
	 * @param diskBytes
	 * @param fileBytes
	 * @param allocatioBlockSize
	 * @param allocationBlocksForFile 
	 */
	public static void copyFileBytesToDisk(byte[] diskBytes, byte[] fileBytes, 
			int allocatioBlockSize, List<Integer> allocationBlocksForFile) {
		
		int totalLen = fileBytes.length;
		for (int i=0; i<allocationBlocksForFile.size(); i++) {
			Integer allocationBlockNumber = allocationBlocksForFile.get(i);
			int len = Math.min(allocatioBlockSize, totalLen - i * allocatioBlockSize);
			System.arraycopy(fileBytes, i * allocatioBlockSize, diskBytes,
					allocationBlockNumber * allocatioBlockSize, len);
		}
	}
	
	public static byte[] getFileBytesFromDisk(int filesize, int allocatioBlockSize, 
			List<Integer> allocationBlocksForFile, byte[] diskBytes) {
		byte[] result = new byte[filesize];
		int totalLen = filesize;
		for (int i=0; i<allocationBlocksForFile.size(); i++) {
			Integer allocationBlockNumber = allocationBlocksForFile.get(i);
			int len = Math.min(allocatioBlockSize, totalLen - i * allocatioBlockSize);
			System.arraycopy(diskBytes, allocationBlockNumber * allocatioBlockSize, result,
					i * allocatioBlockSize, len);
		}
		return result;
	}
	
	/**
	 * first 128 bytes are sector data, last byte is checksum
	 * @param bytes
	 * @return sector data
	 */
	public static byte[] checksumBytes(byte[] bytes) {
		int checksum = 0;
		for (int i=0; i<DiskEmulator.SECTOR_LEN; i++) {
			checksum += Byte.toUnsignedInt(bytes[i]);
			checksum &= 0xFF; // DiskEmulator.BYTE_VAL
		}
		//checksum = checksum % 256;
		//checksum = checksum == 0 ? checksum : 256-checksum;
		int receivedChecksum = Byte.toUnsignedInt(bytes[DiskEmulator.SECTOR_LEN]);
		if (checksum != receivedChecksum) {
			throw new CheckSumException("checksum error!");
		}
		
		return Arrays.copyOf(bytes, DiskEmulator.SECTOR_LEN);
	}
}
