/**
 * table model for hex buffer
 */
package com.archeocomp.silentdrive.gui.hexer;

import java.io.BufferedWriter;
import java.io.IOException;
import javax.swing.table.AbstractTableModel;

/**
 * represents memory from 0000 to FFFF, organized as a table with 16 bytes per
 * row
 */
public class HexModel extends AbstractTableModel {

    public static final int MEM_64KB = 65536;
    public static final int PAGE_256 = 256;
    private int data[] = new int[MEM_64KB];
    private static HexModel instance = null;

    private HexModel() {
    }

    public static HexModel getInstance() {
        if (instance == null) {
            instance = new HexModel();
        }
        return instance;
    }

    public int getRowCount() {
        return MEM_64KB / 16;
    }

    public int getColumnCount() {
        return 1 + 16;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        String result = null;
        if (columnIndex == 0) {
            int address = rowIndex * 16;
            result = byteToHex(address / PAGE_256) + byteToHex(address % PAGE_256);
        } else {
            columnIndex--;
            result = byteToHex(data[rowIndex * 16 + columnIndex]);
        }
        return result;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int columnIndex) {
        String name = "";
        if (columnIndex > 0) {
            name = String.valueOf(Character.forDigit(columnIndex - 1, 16)).toUpperCase();
        }
        return name;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        columnIndex--;
        int value = hexToByte((String) aValue);
        if ((value < 0) || (value > 255)) {
            value = 0;
        }
        data[rowIndex * 16 + columnIndex] = value;
        this.fireTableCellUpdated(rowIndex, columnIndex);
    }

    /**
     * @return the data
     */
    public int[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(int[] data) {
        this.data = data;
    }

    public void fill(String valueStr) {
        int value = hexToByte(valueStr);
        if ((value >= 0) && (value < PAGE_256)) {
            for (int i = 0; i < MEM_64KB; i++) {
                data[i] = value;
            }
            this.fireTableDataChanged();
        }
    }

    /**
     * move data in buffer
     *
     * @param offsetStr how much to move
     * @param direction up (true) or down
     */
    public void moveData(String offsetStr, boolean direction, String fillStr) {
        if ((offsetStr == null) || (offsetStr.length() != 4)) {
            System.err.println("bad offset");
            return;
        }
        int offset = hexToByte(offsetStr.substring(0, 2)) * PAGE_256 + hexToByte(offsetStr.substring(2, 4));
        int newArray[] = new int[MEM_64KB];
        int fill = hexToByte(fillStr);
        if (direction) {
            for (int i = MEM_64KB - offset; i < MEM_64KB; i++) {
                newArray[i] = fill;
            }
        } else {
            for (int i = 0; i < offset; i++) {
                newArray[i] = fill;
            }
        }
        for (int i = 0; i < MEM_64KB; i++) {
            int targetAddress = direction ? i - offset : i + offset;
            if ((targetAddress >= 0) && (targetAddress < MEM_64KB)) {
                newArray[targetAddress] = data[i];
            }
        }
        System.arraycopy(newArray, 0, data, 0, MEM_64KB);
        this.fireTableDataChanged();
    }

    /**
     * exports data as intel hex. each line consists of following fields : len
     * address type data checksum
     *
     * @param startAddress
     * @param endAddress
     */
    public void exportIntelHex(BufferedWriter bw, String startAddressStr, String endAddressStr, int bytesPerLine) throws IOException {
        int startAddress = hexToByte(startAddressStr.substring(0, 2)) * PAGE_256 + hexToByte(startAddressStr.substring(2, 4));
        if ((startAddress < 0) || (startAddress > MEM_64KB)) {
            startAddress = 0;
        }
        int endAddress = hexToByte(endAddressStr.substring(0, 2)) * PAGE_256 + hexToByte(endAddressStr.substring(2, 4)) + 1;
        if ((endAddress < 0) || (endAddress > MEM_64KB)) {
            endAddress = MEM_64KB;
        }
        String line = null;
        int checksum = 0;
        boolean firstLine = true;
        for (int i = startAddress; i < endAddress; i++) {
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
                line += byteToHex(i / PAGE_256) + byteToHex(i % PAGE_256);
                checksum += i / PAGE_256;
                checksum += i % PAGE_256;
                // record type
                line += "00";
                checksum += 0;
            }
            // data
            for (int j = 0; j < recordLength; j++) {
                line += byteToHex(data[i + j]);
                checksum += data[i + j];
                checksum %= PAGE_256;
            }
            i += recordLength - 1;
            // close last line
            if ((line != null) && (line.length() > 0)) {
                line += byteToHex(PAGE_256 - checksum) + "\n";
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
        int address = hexToByte(line.substring(0, 2)) * PAGE_256 + hexToByte(line.substring(2, 4));
        checksum += hexToByte(line.substring(0, 2)) + hexToByte(line.substring(2, 4));
        checksum %= PAGE_256;
        line = line.substring(4);

        // record type
        if (hexToByte(line.substring(0, 2)) != 0) {
            return;
        }
        checksum += hexToByte(line.substring(0, 2));
        checksum %= PAGE_256;
        line = line.substring(2);

        // data
        for (int i = 0; i < length; i++) {
            data[address + i] = hexToByte(line.substring(0, 2));
            checksum += hexToByte(line.substring(0, 2));
            checksum %= PAGE_256;
            line = line.substring(2);
        }

        // checksum
        if ((checksum + hexToByte(line.substring(0, 2))) % PAGE_256 != 0) {
            System.err.println("checksum error at address:" + address);
        }
        this.fireTableRowsUpdated(address / 16, address / 16);
    }

    public int hexToByte(String hex) {
        int result = 0;
        if ((hex != null) && (hex.length() == 2)) {
            char upper = hex.charAt(0);
            char lower = hex.charAt(1);
            int up = Character.digit(upper, 16);
            up *= 16;
            int low = Character.digit(lower, 16);
            result = up + low;
        }
        return result;
    }

    public String byteToHex(int value) {
        value = value % PAGE_256;
        int up = value / 16;
        int low = value % 16;
        char upper = Character.forDigit(up, 16);
        char lower = Character.forDigit(low, 16);
        return (String.valueOf(upper) + lower).toUpperCase();
    }

}
