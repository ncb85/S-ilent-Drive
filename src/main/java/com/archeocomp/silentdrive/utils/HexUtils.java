/*
 * hex utils
 */
package com.archeocomp.silentdrive.utils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * hex utils
 */
public class HexUtils {
	
	private static final String DELIMITER = "    ";
	
	private static final int BUFFER_SIZE = 65536;
	private static final int PAGE_SIZE = 256;
	
	private int buffer[];
	private int newArray[];
	
    /**
     * exports data as intel hex. each line consists of following fields
     * : len address type data checksum
     * @param startAddress
     * @param endAddress
     */
    public void exportIntelHex(BufferedWriter bw, String startAddressStr, String endAddressStr, int bytesPerLine) throws IOException {
        int startAddress = hexToByte(startAddressStr.substring(0, 2)) * PAGE_SIZE + hexToByte(startAddressStr.substring(2, 4));
        if ((startAddress <0) || (startAddress > BUFFER_SIZE)) {
            startAddress = 0;
        }
        int endAddress = hexToByte(endAddressStr.substring(0, 2)) * PAGE_SIZE + hexToByte(endAddressStr.substring(2, 4)) + 1;
        if ((endAddress < 0) || (endAddress > BUFFER_SIZE)) {
            endAddress = BUFFER_SIZE;
        }
        String line = null;
        int checksum = 0;
        boolean firstLine = true;
        for (int i=startAddress;i<endAddress; i++) {
            int recordLength;
            if (firstLine == true) {
                int nextLineEndAddress = startAddress + bytesPerLine;
                if (startAddress % bytesPerLine != 0) {
                    nextLineEndAddress = (startAddress / bytesPerLine) * bytesPerLine + bytesPerLine;
                }
                if (nextLineEndAddress > endAddress) {
                    nextLineEndAddress = endAddress;
                }
                recordLength = nextLineEndAddress - startAddress;
            } else {
                recordLength = ((i + bytesPerLine) <= endAddress) ? bytesPerLine : endAddress - i;
            }
            if (((i % bytesPerLine) == 0) || (firstLine == true)) {
                firstLine = false;
                // start new line
                checksum = 0;
                line = ":";
                // length
                line += byteToHex(recordLength);
                checksum += recordLength;
                // address
                line += byteToHex(i / PAGE_SIZE) + byteToHex(i % PAGE_SIZE);
                checksum += i / PAGE_SIZE;
                checksum += i % PAGE_SIZE;
                // record type
                line += "00";
                checksum += 0;
            }
            // data
            for (int j=0; j<recordLength; j++) {
                line += byteToHex(buffer[i + j]);
                checksum += buffer[i + j];
                checksum %= PAGE_SIZE;
            }
            i += recordLength - 1;
            // close last line
            if ((line != null) && (line.length() > 0)) {
                line += byteToHex(PAGE_SIZE - checksum) + "\n";
                bw.write(line);
            }
        }
        // write EOF record
        bw.write(":00000001FF\n");
    }

    public void processLine(String line) {
        if (line.charAt(0) != ':') {
            return;
        }
        line = line.substring(1);

        int checksum = 0;

        // length
        int length = hexToByte(line.substring(0, 2));
        checksum = length;
        line = line.substring(2);

        // address
        int address = hexToByte(line.substring(0, 2)) * PAGE_SIZE + hexToByte(line.substring(2, 4));
        checksum += hexToByte(line.substring(0, 2)) + hexToByte(line.substring(2, 4));
        checksum %= PAGE_SIZE;
        line = line.substring(4);

        // record type
        if (hexToByte(line.substring(0, 2)) != 0) {
            return;
        }
        checksum += hexToByte(line.substring(0, 2));
        checksum %= PAGE_SIZE;
        line = line.substring(2);

        // data
        for (int i=0; i<length; i++) {
            buffer[address + i] = hexToByte(line.substring(0, 2));
            checksum += hexToByte(line.substring(0, 2));
            checksum %= PAGE_SIZE;
            line = line.substring(2);
        }

        // checksum
        if ((checksum + hexToByte(line.substring(0, 2))) % PAGE_SIZE != 0) {
            System.err.println("checksum error at address:" + address);
        }
    }

    public int hexToByte(String hex) {
        int result = 0;
        if ((hex != null) && (hex.length() == 2)) {
            char upper = hex.charAt(0);
            char lower = hex.charAt(1);
            int up = Character.digit(upper, 16);
            up *= 16;
            int low =  Character.digit(lower, 16);
            result = up + low;
        }
        return result;
    }

    public static String byteToHex(int value) {
        value = value % PAGE_SIZE;
        int up = value / 16;
        int low = value % 16;
        char upper = Character.forDigit(up, 16);
        char lower = Character.forDigit(low, 16);
        return (String.valueOf(upper) + lower).toUpperCase();
    }

    /**
     * move data in buffer
     * @param offsetStr how much to move
     * @param direction up (true) or down
     */
    public void moveData(String offsetStr, boolean direction, String fillStr) {
        if ((offsetStr == null) || (offsetStr.length() != 4)) {
            System.err.println("bad offset");
            return;
        }
        int offset = hexToByte(offsetStr.substring(0, 2)) * PAGE_SIZE + hexToByte(offsetStr.substring(2, 4));
        int fill = hexToByte(fillStr);
        if (direction) {
            for (int i=BUFFER_SIZE-offset; i<BUFFER_SIZE; i++) {
                newArray[i] = fill;
            }
        } else {
            for (int i=0; i<offset; i++) {
                newArray[i] = fill;
            }
        }
        for (int i=0; i<BUFFER_SIZE; i++) {
            int targetAddress = direction ? i - offset : i + offset;
            if ((targetAddress >= 0) && (targetAddress < BUFFER_SIZE)) {
                newArray[targetAddress] = buffer[i];
            }
        }
        System.arraycopy(newArray, 0, buffer, 0, BUFFER_SIZE);
    }

	public static String dumpBytes(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		String result = "";
		String byteStr = "";
		String asciiStr = DELIMITER;
		for (int i=0; i<bytes.length; i++) {
			int unsignedByteValue = Byte.toUnsignedInt(bytes[i]);
			byteStr += byteToHex(unsignedByteValue) + " ";
			char p = (char)unsignedByteValue;
			if (p >= 32 && p < 127) {
				asciiStr += p;
			} else {
				asciiStr += ".";
			}
			if (((i+1) % 16) == 0) {
				result += byteStr + asciiStr + "\n";
				byteStr = "";
				asciiStr = DELIMITER;
			}
		}
		return result;
	}
}
