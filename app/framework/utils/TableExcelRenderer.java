/*! LICENSE
 *
 * Copyright (c) 2015, The Agile Factory SA and/or its affiliates. All rights
 * reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package framework.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import framework.utils.Table.ColumnDef;
import framework.utils.Table.FormattedRow;
import framework.utils.Table.NotFormattedRow;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;
import play.Logger;

/**
 * This class takes a {@link Table} as a parameter and renders an Excel file.
 * <br/>
 * There are two options:
 * <ul>
 * <li>formatted : the column formatters are applied but all the cell will be
 * filled with Strings</li>
 * <li>not formatted : the column formatters are NOT applied but the cells might
 * be String, Boolean or Date</li>
 * </ul>
 * 
 * <b>WARNING</b>The excel file generation is done "in memory". Be carefull with
 * very large files.
 * 
 * @author Pierre-Yves Cloux
 */
public class TableExcelRenderer {
    public static final String DEFAULT_EXCEL_DATE_FORMAT = "m/d/yy h:mm";
    private static final char INVISIBLE_BULLET_CHAR = 0xFFFA;

    static {
        // See Jericho documentation
        Renderer.setDefaultTopMargin(HTMLElementName.UL, 0);
        Renderer.setDefaultTopMargin(HTMLElementName.LI, 0);
        Renderer.setDefaultTopMargin(HTMLElementName.DIV, 0);
        Renderer.setDefaultBottomMargin(HTMLElementName.UL, 0);
        Renderer.setDefaultBottomMargin(HTMLElementName.LI, 0);
        Renderer.setDefaultBottomMargin(HTMLElementName.DIV, 0);
    }

    private static Logger.ALogger log = Logger.of(TableExcelRenderer.class);

    /**
     * Generate an Excel representation of the specified {@link Table}.<br/>
     * Formatting is applied.
     * 
     * @param table
     * @return a byte array (Excel file)
     */
    public static byte[] renderFormatted(Table<?> table) {
        return render(table, true);
    }

    /**
     * Generate an Excel representation of the specified {@link Table}.<br/>
     * No formatting is applied.
     * 
     * @param table
     * @return a byte array (Excel file)
     */
    public static byte[] renderNotFormatted(Table<?> table) {
        return render(table, false);
    }

    /**
     * Render the specified table
     * 
     * @param table
     * @param formatted
     *            true if a formatting must be applied
     * @return a byte array (Excel file)
     */
    private static byte[] render(Table<?> table, boolean formatted) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("export");

        // Default date format
        XSSFCellStyle dateCellStyle = wb.createCellStyle();
        XSSFDataFormat df = wb.createDataFormat();
        dateCellStyle.setDataFormat(df.getFormat(DEFAULT_EXCEL_DATE_FORMAT));

        // Write the header
        Row headerRow = sheet.createRow(0);
        int columnIndex = 0;
        CellStyle headerCellStyle = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerCellStyle.setFont(f);
        for (ColumnDef header : table.getHeaders()) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(Msg.get(header.getLabel()));
            cell.setCellStyle(headerCellStyle);
            columnIndex++;
        }

        // Write the cells
        if (formatted) {
            writeFormattedRows(table, sheet, dateCellStyle);
        } else {
            writeNotFormattedRows(table, sheet, dateCellStyle);
        }

        ByteArrayOutputStream outBuffer = null;
        try {
            outBuffer = new ByteArrayOutputStream();
            wb.write(outBuffer);
            outBuffer.close();
        } catch (Exception e) {
            log.error("Error while generating an Excel file from a Table", e);
            throw new RuntimeException("Error while generating an Excel file from a Table", e);
        }
        return outBuffer.toByteArray();
    }

    /**
     * Write a NOT formatted row (see the concept of NOT formatted row in
     * {@link Table})
     * 
     * @param table
     * @param sheet
     * @param dateCellStyle
     *            the style to be applied for {@link Date} cell values
     */
    private static void writeNotFormattedRows(Table<?> table, Sheet sheet, XSSFCellStyle dateCellStyle) {
        int rowIndex = 1;
        int columnIndex;
        for (NotFormattedRow row : table.getNotFormattedRows()) {
            Row dataRow = sheet.createRow(rowIndex);
            rowIndex++;
            columnIndex = 0;
            for (Object cellValue : row.getValues()) {
                Cell cell = dataRow.createCell(columnIndex);
                if (cellValue == null) {
                    cell.setCellValue("");
                } else {
                    if (cellValue.getClass().equals(Date.class)) {
                        cell.setCellValue((Date) cellValue);
                        cell.setCellStyle(dateCellStyle);
                    } else {
                        if (cellValue.getClass().equals(Boolean.class) || cellValue.getClass().equals(boolean.class)) {
                            cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
                            cell.setCellValue((Boolean) cellValue);
                        } else {
                            if (cellValue.getClass().equals(BigDecimal.class)) {
                                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                                cell.setCellValue(((BigDecimal) cellValue).doubleValue());
                            } else {
                                if (cellValue instanceof Iterable<?>) {
                                    StringBuffer sb = new StringBuffer();
                                    for (Object item : (Iterable<?>) cellValue) {
                                        sb.append(item).append(',');
                                    }
                                    if (sb.length() != 0 && sb.charAt(sb.length() - 1) == ',') {
                                        sb.delete(sb.length() - 1, sb.length());
                                    }
                                    cell.setCellValue(sb.toString());
                                } else {
                                    cell.setCellValue(cellValue.toString());
                                }
                            }
                        }
                    }
                }
                columnIndex++;
            }
        }
    }

    /**
     * Write a formatted row (see the concept of formatted row in {@link Table})
     * 
     * @param table
     * @param sheet
     */
    private static void writeFormattedRows(Table<?> table, Sheet sheet, XSSFCellStyle dateCellStyle) {
        int rowIndex = 1;
        int columnIndex;
        Iterator<NotFormattedRow> notFormattedRows = table.getNotFormattedRows().iterator();
        for (FormattedRow row : table.getFormattedRows()) {
            NotFormattedRow notFormattedRow = notFormattedRows.next();
            Row dataRow = sheet.createRow(rowIndex);
            rowIndex++;
            columnIndex = 0;
            for (String cellValue : row.getValues()) {
                Object cellvalueAsObject = notFormattedRow.getValues().get(columnIndex);
                Cell cell = dataRow.createCell(columnIndex);
                if (cellvalueAsObject == null) {
                    cell.setCellValue("");
                } else {
                    // If boolean use native value
                    if (cellvalueAsObject.getClass().equals(Boolean.class) || cellvalueAsObject.getClass().equals(boolean.class)) {
                        cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
                        cell.setCellValue((Boolean) cellvalueAsObject);
                    } else {
                        if (cellvalueAsObject.getClass().equals(BigDecimal.class)) {
                            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                            cell.setCellValue(((BigDecimal) cellvalueAsObject).doubleValue());
                        } else {
                            // If date use native value
                            if (cellvalueAsObject.getClass().equals(Date.class)) {
                                cell.setCellValue((Date) cellvalueAsObject);
                                cell.setCellStyle(dateCellStyle);
                            } else {
                                // Any other type of column is "rendered"
                                Source htmlSource = new Source(cellValue);
                                Renderer renderer = htmlSource.getRenderer();
                                renderer.setListBullets(new char[] { INVISIBLE_BULLET_CHAR, '-', '#', '*' });
                                renderer.setBlockIndentSize(0);
                                renderer.setListIndentSize(0);
                                renderer.setIncludeFirstElementTopMargin(false);
                                String stringRepresentation = renderer.toString();
                                if (stringRepresentation.indexOf(INVISIBLE_BULLET_CHAR) != -1) {
                                    // Special action of the content is a
                                    // <UL></UL>
                                    StringBuffer sb = new StringBuffer();
                                    String[] lines = stringRepresentation.split("" + INVISIBLE_BULLET_CHAR);
                                    for (String line : lines) {
                                        if (!StringUtils.isBlank(line)) {
                                            sb.append(line.trim());
                                            sb.append(';');
                                        }
                                    }
                                    if (sb.length() > 1) {
                                        sb.deleteCharAt(sb.length() - 1);
                                    }
                                    cell.setCellValue(sb.toString());
                                } else {
                                    cell.setCellValue(stringRepresentation.trim());
                                }
                            }
                        }
                    }
                }
                columnIndex++;
            }
        }
    }
}
