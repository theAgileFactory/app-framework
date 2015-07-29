package framework.patcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * The patch manager is responsible for running some patches for a named system.<br/>
 * The patches are to be implemented as Java classes implementing the
 * {@link IPatch} interface. The patch manager is associated with two
 * properties:
 * <ul>
 * <li>patchPackage : the java package which contains the {@link IPatch} classes
 * </li>
 * <li>patchStatusFilePath : the file which contains the list of patch which
 * have already run. This file contains one row per "patch" with the following
 * structure:<br/>
 * 
 * <pre>
 * {@code
 * <the patch class name>|<date when it was run if successfull>
 * }
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 * The {@link IPatch} must be named according to the following pattern:<br>
 * 
 * <pre>
 * {@code
 * Patch_ddMMyyyyHHmmssSS_sometext
 * }
 * </pre>
 * 
 * This means that each class is named according to a timestamp. This one will
 * allow the application to sort the patches (in order to run them in the right
 * order).
 * 
 * @author Pierre-Yves Cloux
 */
public class PatchManager {
    /**
     * The patch class name pattern
     */
    private static final String PATCH_CLASSNAME_PATTERN = "Patch_(\\d*)_(\\.*)";

    /**
     * The pattern for the timestamp part of the class name
     */
    private static final String PATCH_CLASSNAME_TIMESTAMP_PATTERN = "ddMMyyyyHHmmss";

    /**
     * The String sequence which is used to separate the name of the patch class
     * from its execution time into the status file
     */
    private static final String PATCH_STATUS_LOG_SEPARATOR = " | ";

    private List<String> patchesNames;
    private String lastRunPatch;
    private IPatchLog log;
    private File patchStatusFile;

    /**
     * Creates a new patch manager
     * 
     * @param patchPackageName
     *            the java package which contains the patch classes
     * @param patchStatusFilePath
     *            the path to the status file
     * @param log
     *            a log
     * @throws PatcherException
     */
    public PatchManager(String patchPackageName, String patchStatusFilePath, IPatchLog log) throws PatcherException {
        super();
        this.log = log;
        // Find the patches
        this.patchesNames = getPatchesList(patchPackageName, log);
        // Find the last file run in the status file
        this.lastRunPatch = getLastRunPatch(patchStatusFilePath, log);
        this.patchStatusFile = new File(patchStatusFilePath);
    }

    /**
     * Execute the required scripts
     * 
     * @throws PatcherException
     */
    public void execute() throws PatcherException {
        // Find the patches to be run
        List<String> patchesToBeRun = getPatchesToRun();
        if (patchesToBeRun.size() == 0) {
            getLog().warn("No patch to run");
        } else {
            getLog().info("Running the scripts in sequence " + patchesToBeRun);
        }
        for (String patchName : patchesToBeRun) {
            try {
                getLog().info("Running : " + patchName);
                IPatch patch = IPatch.class.cast(Class.forName(patchName).newInstance());
                patch.perform(getLog());
                getLog().info("Success : " + patchName);
                FileUtils.write(getPatchStatusFile(), patchName + PATCH_STATUS_LOG_SEPARATOR + (new Date()) + "\n", true);
            } catch (Exception e) {
                throw new PatcherException("Patch execution [" + patchName + "] failed, stopping", e);
            }
        }
    }

    /**
     * Return the list of patches to be run.<br/>
     * This is done by looking at the current list and the last run script (if
     * any)
     * 
     * @return
     */
    private List<String> getPatchesToRun() {
        List<String> patchesToBeRun = new ArrayList<String>();
        if (getLastRunPatch() != null) {
            boolean isBeforeLastRunPatch = true;
            for (String patch : getPatchesNames()) {
                if (!isBeforeLastRunPatch) {
                    patchesToBeRun.add(patch);
                }
                if (patch.startsWith(getLastRunPatch())) {
                    isBeforeLastRunPatch = false;
                }
            }
        } else {
            // All the patch must be run (since no previous run detected)
            patchesToBeRun = getPatchesNames();
        }
        return patchesToBeRun;
    }

    /**
     * Check if the status file exists and get the last run script (if it does
     * not exists created it)
     * 
     * @param patchStatusFilePath
     *            the path of the status file
     * @param log
     *            a log file
     * @return the name of the last file which was run or null if the status
     *         file is new (nothing ever run)
     * @throws PatcherException
     */
    private static String getLastRunPatch(String patchStatusFilePath, IPatchLog log) throws PatcherException {
        File statusFile = new File(patchStatusFilePath);
        if (statusFile.exists() && statusFile.isFile()) {
            FileReader fReader = null;
            try {
                log.info("Status file found " + patchStatusFilePath);
                fReader = new FileReader(patchStatusFilePath);
                List<String> statusFileLogRecords = IOUtils.readLines(fReader);
                if (statusFileLogRecords == null || statusFileLogRecords.size() == 0) {
                    log.info("Status file is empty, starting from the first patch");
                    return null;
                }
                // Return the lastFile run
                String statusFileLogRecord = statusFileLogRecords.get(statusFileLogRecords.size() - 1);
                int indexOfSeparator = statusFileLogRecord.indexOf(PATCH_STATUS_LOG_SEPARATOR);
                if (indexOfSeparator == -1) {
                    throw new PatcherException("Invalid status file record " + statusFileLogRecord);
                }
                return statusFileLogRecord.substring(0, indexOfSeparator);
            } catch (FileNotFoundException e) {
                throw new PatcherException("Unable to read the status file " + patchStatusFilePath, e);
            } catch (IOException e) {
                throw new PatcherException("Unable to read the status file " + patchStatusFilePath, e);
            } finally {
                IOUtils.closeQuietly(fReader);
            }
        } else {
            log.warn("Status file does not exists, creating a new one " + patchStatusFilePath);
            try {
                if (statusFile.createNewFile()) {
                    return null;
                }
                throw new PatcherException("Unable to create a new status file " + patchStatusFilePath);
            } catch (IOException e) {
                throw new PatcherException("Unable to create a new status file " + patchStatusFilePath, e);
            }
        }
    }

    /**
     * Look for the patches class names in the specified package
     * 
     * @param patchPackageName
     *            the name of the package which contains the {@link IPatch}
     *            instances
     * @return an ordered list of class names (according to the time stamp)
     * @throws PatcherException
     */
    private static List<String> getPatchesList(String patchPackageName, IPatchLog log) throws PatcherException {
        List<String> patchesNames = new ArrayList<String>();
        Map<Date, String> patchesDictionary = new HashMap<Date, String>();

        // Creates the pattern matching
        Pattern pattern = Pattern.compile(PATCH_CLASSNAME_PATTERN);
        SimpleDateFormat sdf = new SimpleDateFormat(PATCH_CLASSNAME_TIMESTAMP_PATTERN);

        // Look for the IPatch classes in the specified package
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        final Set<BeanDefinition> classes = provider.findCandidateComponents(patchPackageName);
        for (BeanDefinition bean : classes) {
            Class<?> objectClass = null;
            try {
                objectClass = Class.forName(bean.getBeanClassName());
            } catch (ClassNotFoundException e) {
            }
            if (objectClass != null && IPatch.class.isAssignableFrom(objectClass)) {
                log.info("Found patch " + bean.getBeanClassName());
                Matcher matcher = pattern.matcher(objectClass.getSimpleName());
                if (matcher.find()) {
                    String timeStampAsString = matcher.group(1);
                    try {
                        Date timeStamp = sdf.parse(timeStampAsString);
                        if (patchesDictionary.containsKey(timeStamp)) {
                            throw new PatcherException("Two patches with the same timestamps have been found [" + bean.getBeanClassName() + "] and ["
                                    + patchesDictionary.get(timeStamp) + "]");
                        }
                        patchesDictionary.put(timeStamp, objectClass.getName());
                    } catch (ParseException e) {
                        throw new PatcherException("Found patch class " + objectClass.getName() + " with an invalid timestamp");
                    }
                }
            }
        }

        // Sort the classes according to their name
        List<Date> patchTimeStamps = new ArrayList<Date>(patchesDictionary.keySet());
        Collections.sort(patchTimeStamps);

        // Fill the patches list
        for (Date patchTimeStamp : patchTimeStamps) {
            patchesNames.add(patchesDictionary.get(patchTimeStamp));
        }

        return patchesNames;
    }

    public List<String> getPatchesNames() {
        return patchesNames;
    }

    public String getLastRunPatch() {
        return lastRunPatch;
    }

    private IPatchLog getLog() {
        return log;
    }

    private File getPatchStatusFile() {
        return patchStatusFile;
    }
}
