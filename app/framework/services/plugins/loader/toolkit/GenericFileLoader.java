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
package framework.services.plugins.loader.toolkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import play.Logger;

import com.avaje.ebean.Ebean;

import framework.utils.Utilities;

/**
 * A class which loads a CSV file into the database.<br/>
 * Each row is mapped to a {@link ILoadableObject}.<br/>
 * <p>
 * Here are the parameters for the class:
 * <ul>
 * <li>genericFileLoaderMapper : a mapper class which defines what must be
 * loaded</li>
 * <li>csvFormatType : a CSV file format</li>
 * <li>allowedCharSet : the charSet of the input file or stream</li>
 * <li>log : the logger to be user to log the errors</li>
 * <li>isTestMode : true if the loader is in test mode (the file or stream
 * content is parsed but not loaded into the database)</li>
 * <li>ignoreInvalidRows : if true, only the valid rows are saved, if false the
 * process will stop if an invalid row is found</li>
 * </ul>
 * </p>
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <K>
 */
public class GenericFileLoader<K extends ILoadableObject> {
    private Logger.ALogger log;
    private CSVFormatType csvFormatType;
    private AllowedCharSet allowedCharSet;
    private IGenericFileLoaderMapper<K> genericFileLoaderMapper;
    private boolean ignoreInvalidRows;
    private boolean testMode;

    /**
     * Constructor.
     * 
     * @param genericFileLoaderMapper
     *            the generic file loader mapper
     * @param csvFormatType
     *            the CSV form type
     * @param allowedCharSet
     *            the charset
     * @param log
     *            the logger (play)
     * @param isTestMode
     *            set to true if test mode
     * @param ignoreInvalidRows
     *            set to true to ignore the invalid rows, meaning to process the
     *            valid ones
     */
    public GenericFileLoader(IGenericFileLoaderMapper<K> genericFileLoaderMapper, CSVFormatType csvFormatType, AllowedCharSet allowedCharSet,
            Logger.ALogger log, boolean isTestMode, boolean ignoreInvalidRows) {
        this.genericFileLoaderMapper = genericFileLoaderMapper;
        this.csvFormatType = csvFormatType;
        this.allowedCharSet = allowedCharSet;
        this.log = log;
        this.testMode = isTestMode;
        this.ignoreInvalidRows = ignoreInvalidRows;
    }

    /**
     * Load the specified input stream.
     * 
     * @param inputFileStream
     *            an input stream to a CSV source
     * @throws IOException
     */
    public String performLoad(InputStream inputFileStream) throws IOException {
        int rowCount = 0;
        List<K> loadableObjects = new ArrayList<K>();
        Map<Long, String> invalidRows = null;
        SaveToDatabaseReports saveToDatabaseReports = null;
        boolean failed = false;
        String failureExceptionMessage = "";

        try {
            getGenericFileLoaderMapper().init();
            // Parse the objects
            iterateAndParse(loadableObjects, inputFileStream);
            rowCount = loadableObjects.size();
            // Perform some validation
            invalidRows = getGenericFileLoaderMapper().validate(loadableObjects);
            failed = isIgnoreInvalidRows() ? false : invalidRows.size() > 0;
        } catch (Exception e) {
            failed = true;
            failureExceptionMessage = "Error during the file parsing :\n" + Utilities.getExceptionAsString(e);
            log.error("Error while parsing the input file", e);
        }

        if (!failed && !isTestMode()) {
            try {
                // Filter the invalid rows
                Iterator<K> itr = loadableObjects.iterator();
                while (itr.hasNext()) {
                    K obj = itr.next();
                    if (invalidRows.containsKey(obj.getSourceRowNumber())) {
                        itr.remove();
                    }
                }
                saveToDatabaseReports = saveToDatabase(loadableObjects);
            } catch (Exception e) {
                failed = true;
                failureExceptionMessage = "Error during the database uploading :\n" + Utilities.getExceptionAsString(e);
                log.error("Error during the database uploading", e);
            }
        }

        // Write or send a report
        String report = createReport(rowCount, loadableObjects, invalidRows, saveToDatabaseReports, failed, failureExceptionMessage);

        loadableObjects.clear();
        getGenericFileLoaderMapper().close();

        return report;
    }

    /**
     * Save the provided loadable objects into the database.
     * 
     * @param loadableObjects
     *            an array of loadable objects
     * 
     * @return a set of reports
     * @throws IOException
     */
    private SaveToDatabaseReports saveToDatabase(List<K> loadableObjects) throws IOException {
        SaveToDatabaseReports saveToDatabaseReports = new SaveToDatabaseReports();
        saveToDatabaseReports.newlyCreated = new ArrayList<Pair<Long, String>>();
        // Save in the database
        Ebean.beginTransaction();
        try {
            saveToDatabaseReports.beforeSaveReport = getGenericFileLoaderMapper().beforeSave(loadableObjects);
            for (K loadableObject : loadableObjects) {
                Pair<Long, String> pair = loadableObject.updateOrCreate();
                if (pair != null) {
                    saveToDatabaseReports.newlyCreated.add(pair);
                }
            }
            saveToDatabaseReports.afterSaveReport = getGenericFileLoaderMapper().afterSave(loadableObjects);
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            throw new IOException(e);
        } finally {
            Ebean.endTransaction();
        }
        return saveToDatabaseReports;
    }

    /**
     * Iterate over the CSV source, parse its content and create loadable
     * objects.
     * 
     * @param loadableObjects
     *            an array to be filled with Loadable objects
     * @param inputStream
     *            an input stream which points to a CSV source
     * @throws IOException
     */
    private void iterateAndParse(List<K> loadableObjects, InputStream inputStream) throws IOException {
        InputStreamReader reader = null;
        CSVParser parser = null;
        long rowCount = 0;
        try {
            // Iterate over the CSV records
            reader = new InputStreamReader(new BOMInputStream(inputStream), getAllowedCharSet().getCharSet());
            parser = new CSVParser(reader, getCsvFormatType().getCsvFormat());
            for (final CSVRecord record : parser) {
                rowCount = record.getRecordNumber();
                K loadableObject = getGenericFileLoaderMapper().createNewLoadableObject();
                loadableObject.setSourceRowNumber(rowCount);
                boolean ignore = getGenericFileLoaderMapper().convert(record, loadableObject);
                if (!ignore) {
                    loadableObjects.add(loadableObject);
                }
            }
        } catch (Exception e) {
            String message = "Error while loading the CSV file at row " + rowCount;
            getLog().error(message, e);
            throw new IOException(message, e);
        } finally {
            IOUtils.closeQuietly(reader);
            try {
                parser.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Create a text report.
     * 
     * @param rowCount
     *            the number of rows which were processes
     * @param validLoadableObjects
     *            the valid loadableObjects
     * @param invalidRows
     *            the invalid rows
     * @param saveToDatabaseReports
     *            the report content
     * @param failed
     *            true if the load failed
     * @param failureExceptionMessage
     *            the exception which occurred during the load
     * @throws IOException
     * @return a String
     */
    private String createReport(int rowCount, List<K> validLoadableObjects, Map<Long, String> invalidRows, SaveToDatabaseReports saveToDatabaseReports,
            boolean failed, String failureExceptionMessage) throws IOException {

        // Generate the report
        StringBuffer reportAsString = new StringBuffer();
        reportAsString.append(">>> Run at : ").append(new Date());
        reportAsString.append('\n');
        reportAsString.append(">>> Load report : " + (failed ? "FAILED" : "SUCCESS"));
        reportAsString.append('\n');
        reportAsString.append(">>> Number of rows parsed : " + rowCount);
        reportAsString.append('\n');

        if (failed) {
            if (!StringUtils.isBlank(failureExceptionMessage)) {
                reportAsString.append(">>> Error message : " + failureExceptionMessage);
                reportAsString.append('\n');
            }
            if (invalidRows != null) {
                reportAsString.append(">>> Invalid rows :").append(invalidRows.size()).append('\n');
                reportAsString.append('\n');
                for (Long rowId : invalidRows.keySet()) {
                    reportAsString.append("Row [").append(rowId).append("] = ").append(invalidRows.get(rowId)).append('\n');
                }
            }
        } else {
            if (saveToDatabaseReports != null) {
                if (saveToDatabaseReports.beforeSaveReport != null && saveToDatabaseReports.beforeSaveReport.getRight() != null
                        && saveToDatabaseReports.beforeSaveReport.getRight().size() != 0) {
                    reportAsString.append('\n');
                    reportAsString.append('\n');
                    reportAsString.append(">>> Reporting " + saveToDatabaseReports.beforeSaveReport.getLeft() + " :");
                    reportAsString.append('\n');
                    for (String messageToReport : saveToDatabaseReports.beforeSaveReport.getRight()) {
                        reportAsString.append(messageToReport);
                        reportAsString.append('\n');
                    }
                }
                if (saveToDatabaseReports.newlyCreated != null && saveToDatabaseReports.newlyCreated.size() != 0) {
                    reportAsString.append('\n');
                    reportAsString.append('\n');
                    reportAsString.append(">>> Newly created " + getGenericFileLoaderMapper().getLoadedObjectName() + ":");
                    reportAsString.append('\n');
                    for (Pair<Long, String> newObjectLog : saveToDatabaseReports.newlyCreated) {
                        reportAsString.append("New " + getGenericFileLoaderMapper().getLoadedObjectName() + " refId=" + newObjectLog.getRight()
                                + " created with id=" + newObjectLog.getLeft());
                        reportAsString.append('\n');
                    }
                }
                if (saveToDatabaseReports.afterSaveReport != null && saveToDatabaseReports.afterSaveReport.getRight() != null
                        && saveToDatabaseReports.afterSaveReport.getRight().size() != 0) {
                    reportAsString.append('\n');
                    reportAsString.append('\n');
                    reportAsString.append(">>> Reporting " + saveToDatabaseReports.afterSaveReport.getLeft() + " :");
                    reportAsString.append('\n');
                    for (String messageToReport : saveToDatabaseReports.afterSaveReport.getRight()) {
                        reportAsString.append(messageToReport);
                        reportAsString.append('\n');
                    }
                }
            }
        }

        if (isTestMode()) {
            reportAsString.append('\n');
            reportAsString.append("\n>>> TEST MODE ACTIVATED\nThe following rows would have been saved into the database\n\n");
            if (validLoadableObjects != null) {
                for (K loadableObject : validLoadableObjects) {
                    reportAsString.append(loadableObject.toString()).append('\n');
                }
            }
        }

        return reportAsString.toString();
    }

    /**
     * Get the plan logger.
     * 
     * @return
     */
    private Logger.ALogger getLog() {
        return log;
    }

    /**
     * Get the CSV format type (EXCEL...).
     */
    private CSVFormatType getCsvFormatType() {
        return csvFormatType;
    }

    /**
     * Get the charset.
     */
    private AllowedCharSet getAllowedCharSet() {
        return allowedCharSet;
    }

    /**
     * Get the generic file loader mapper.
     */
    private IGenericFileLoaderMapper<K> getGenericFileLoaderMapper() {
        return genericFileLoaderMapper;
    }

    /**
     * Return true if test mode is enabled.
     */
    private boolean isTestMode() {
        return testMode;
    }

    /**
     * Return true to ignore the invalid rows.
     */
    private boolean isIgnoreInvalidRows() {
        return ignoreInvalidRows;
    }

    /**
     * The type of Excel formats which are allowed.
     * 
     * @author Pierre-Yves Cloux
     */
    public static enum CSVFormatType {
        EXCEL(CSVFormat.EXCEL.withDelimiter(';').withHeader()), MYSQL(CSVFormat.MYSQL), RFC4180(CSVFormat.RFC4180);

        private CSVFormat csvFormat;

        /**
         * Set the CSV format type.
         * 
         * @param csvFormat
         *            the CSV format
         */
        private CSVFormatType(CSVFormat csvFormat) {
            this.csvFormat = csvFormat;
        }

        /**
         * Get the CSV format type.
         */
        public CSVFormat getCsvFormat() {
            return this.csvFormat;
        }
    }

    /**
     * The char sets which are allowed for the input file.
     * 
     * @author Pierre-Yves Cloux
     */
    public static enum AllowedCharSet {
        ISO_8859_1("ISO8859-1"), UTF8("UTF-8"), ASCII("US-ASCII");

        private String charSet;

        /**
         * Default constructor.
         * 
         * @param charSet
         *            the charset
         */
        private AllowedCharSet(String charSet) {
            this.charSet = charSet;
        }

        /**
         * Get the charset.
         */
        public String getCharSet() {
            return charSet;
        }

        /**
         * Get the charset object from a charset string.
         * 
         * @param charSet
         *            the charset
         */
        public static AllowedCharSet getFromCharset(String charSet) {
            for (AllowedCharSet allowedCharSet : AllowedCharSet.values()) {
                if (allowedCharSet.getCharSet().equals(charSet)) {
                    return allowedCharSet;
                }
            }
            throw new IllegalArgumentException("Invalid AllowedCharSet value " + charSet);
        }
    }

    /**
     * A structure to hold the reports generated during the database saving
     * operations.<br/>
     * <ul>
     * <li>newlyCreated : a list of Pair([id of the newly created object],[refId
     * of the newly created object])</li>
     * <li>beforeSaveReport : Pair([Report title],[List of items to be displayed
     * in the final report])</li>
     * <li>afterSaveReport : Pair([Report title],[List of items to be displayed
     * in the final report])</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    private static class SaveToDatabaseReports {
        public List<Pair<Long, String>> newlyCreated;
        public Pair<String, List<String>> beforeSaveReport;
        public Pair<String, List<String>> afterSaveReport;
    }
}
