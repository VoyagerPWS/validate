// Copyright © 2019, California Institute of Technology ("Caltech").
// U.S. Government sponsorship acknowledged.
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// • Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
// • Redistributions must reproduce the above copyright notice, this list of
// conditions and the following disclaimer in the documentation and/or other
// materials provided with the distribution.
// • Neither the name of Caltech nor its operating division, the Jet Propulsion
// Laboratory, nor the names of its contributors may be used to endorse or
// promote products derived from this software without specific prior written
// permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package gov.nasa.pds.validate.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.nasa.pds.tools.label.ExceptionType;
import gov.nasa.pds.tools.util.Utility;
import gov.nasa.pds.tools.validate.ProblemCategory;
import gov.nasa.pds.tools.validate.ValidationProblem;
import gov.nasa.pds.validate.status.Status;

/**
 * Abstract class that represents a Report for the Vtool command line API. This class handles basic
 * utilities for reporting and calling customized portions of reports.
 *
 * @author pramirez
 *
 */
public abstract class Report {
  private static final Logger LOG = LoggerFactory.getLogger(Report.class);
  private static String DEPRECATED_FLAG_WARNING_MSG =
      ("NOTE: The following flags have been deprecated. These options will no longer be supported after December 20, 2021.\n"
          + "      Update execution as soon as possible to avoid issues. Contact pds_operator@jpl.nasa.gov if you have issues.\n\n"
          + "      --force (-f)--model-version (-m) flags have been deprecated. \n"
          + "      The default behavior of the Validate Tool validates against the schemas and \n"
          + "      schematrons specified in a label.  \n\n"
          + "      Please use -x and/or -S flag(s) to validate with the core PDS or user-specified \n"
          + "      schema and schematron.\n\n"
          + "      --no-check-data flag has been deprecated and replaced by --skip-content-validation to avoid confusion in naming "
          + "      of other flags. Please change software execution to use the new flag to avoid any issues.");

  private boolean deprecatedFlagWarning;
  protected boolean integrityCheckFlag;
  private int totalWarnings;
  private int totalErrors;
  private int totalInfos;
  private int numSkipped;
  private int numFailed;
  private int numPassed;
  private int numPassedProds;
  private int numFailedProds;
  private int numSkippedProds;
  protected int totalProducts;
  protected int numProducts;
  private int numPassedIntegrityChecks;
  private int numFailedIntegrityChecks;
  private int numSkippedIntegrityChecks;
  protected int totalIntegrityChecks;
  protected final List<String> parameters;
  protected final List<String> configurations;
  protected PrintWriter writer;
  private ExceptionType level;
  protected Map<String, Long> messageSummary;

  /**
   * Default constructor to initialize report variables. Initializes default output to System.out if
   * you wish to write the report to a different sourse use the appropriate setOutput method.
   */
  public Report() {
    this.totalWarnings = 0;
    this.totalErrors = 0;
    this.totalInfos = 0;
    this.numFailed = 0;
    this.numFailedProds = 0;
    this.numPassed = 0;
    this.numPassedProds = 0;
    this.numSkipped = 0;
    this.numSkippedProds = 0;
    this.numFailedIntegrityChecks = 0;
    this.numPassedIntegrityChecks = 0;
    this.numSkippedIntegrityChecks = 0;
    this.integrityCheckFlag = false;
    this.deprecatedFlagWarning = false;
    this.parameters = new ArrayList<>();
    this.configurations = new ArrayList<>();
    this.writer = new PrintWriter(new OutputStreamWriter(System.out));
    this.messageSummary = new HashMap<>();
    this.level = ExceptionType.WARNING;
  }

  /**
   * Handles writing a Report to the writer interface. This is is useful if someone would like to
   * put the contents of the Report to something such as {@link java.io.StringWriter}.
   *
   * @param writer which the report will be written to
   */
  public void setOutput(Writer writer) {
    this.writer = new PrintWriter(writer);
  }

  /**
   * Handle writing a Report to an {@link java.io.OutputStream}. This is useful to get the report to
   * print to something such as System.out
   *
   * @param os stream which the report will be written to
   */
  public void setOutput(OutputStream os) {
    this.setOutput(new OutputStreamWriter(os));
  }

  /**
   * Handles writing a Report to a {@link java.io.File}.
   *
   * @param file which the report will output to
   * @throws IOException if there is an issue in writing the report to the file
   */
  public void setOutput(File file) throws IOException {
    this.setOutput(new FileWriter(file));
  }

  /**
   * This method will display the default header for the Vtool command line library reports. This is
   * the standard header across all reports.
   */
  public void printHeader() {
    writer.println();
    writer.println("PDS Validate Tool Report");
    writer.println();
    writer.println("Configuration:");
    for (String configuration : configurations) {
      writer.println(configuration);
    }
    writer.println();
    writer.println("Parameters:");
    for (String parameter : parameters) {
      writer.println(parameter);
    }
    writer.println();
    printHeader(this.writer, "Product Level Validation Results");
  }

  /**
   * Adds the string supplied to the parameter section in the heading of the report.
   *
   * @param parameter in a string form that represents something that was passed in when the tool
   *        was run
   */
  public void addParameter(String parameter) {
    this.parameters.add(parameter);
  }

  /**
   * Adds the string supplied to the configuration section in the heading of the report.
   *
   * @param configuration in a string form that represents a configuration that was used during
   *        parsing and validation
   */
  public void addConfiguration(String configuration) {
    this.configurations.add(configuration);
  }

  public void printHeader(String title) {
    printHeader(this.writer, title);
  }

  /**
   * Allows a Report to customize the header portion of the Report if necessary.
   *
   * @param writer passed down to write header contents to
   */
  protected abstract void printHeader(PrintWriter writer, String title);

  public Status record(URI sourceUri, final ValidationProblem problem) {
    List<ValidationProblem> problems = new ArrayList<>();
    problems.add(problem);
    LOG.debug("record:RECORDING_PROBLEM:sourceUri {}", sourceUri);
    return record(sourceUri, problems);
  }

  /**
   * Allows a report to change how they manage reporting on a given file that has been parsed and
   * validated. Also handles generating a status for a file and generating some summary statistics.
   *
   * @param sourceUri reference to the file that is being reported on
   * @param problems the set of issues found with the file. to be reported on
   * @return status of the file (i.e. PASS, FAIL, or SKIP)
   */
  public Status record(URI sourceUri, final List<ValidationProblem> problems) {
    int numErrors = 0;
    int numWarnings = 0;
    int numInfos = 0;
    Status status = Status.PASS;
    LOG.debug("record:RECORDING_PROBLEM:sourceUri,problems.size {},{}", sourceUri, problems.size());

    // TODO: Handle null problems
    int ignoreFromProductCounts = 0;
    for (ValidationProblem problem : problems) {
      if (problem.getProblem().getSeverity() == ExceptionType.ERROR
          || problem.getProblem().getSeverity() == ExceptionType.FATAL) {
        if (ExceptionType.ERROR.getValue() <= this.level.getValue()) {
          numErrors++;
          addToMessageSummary(problem.getProblem().getType().getKey());
        }
      } else if (problem.getProblem().getSeverity() == ExceptionType.WARNING) {
        if (ExceptionType.WARNING.getValue() <= this.level.getValue()) {
          numWarnings++;
          addToMessageSummary(problem.getProblem().getType().getKey());
        }
      } else if (problem.getProblem().getSeverity() == ExceptionType.INFO) {
        if (ExceptionType.INFO.getValue() <= this.level.getValue()) {
          numInfos++;
          addToMessageSummary(problem.getProblem().getType().getKey());
        }
      } else if (problem.getProblem().getSeverity() == ExceptionType.DEBUG) {
        if (ExceptionType.DEBUG.getValue() <= this.level.getValue()) {
          addToMessageSummary(problem.getProblem().getType().getKey());
        }
      }

      // Check ProblemCategory to remove from product counts
      ProblemCategory category = problem.getProblem().getType().getProblemCategory();
      if (category.equals(ProblemCategory.GENERAL) || category.equals(ProblemCategory.EXECUTION)) {
        ignoreFromProductCounts++;
      }
    }
    this.totalErrors += numErrors;
    this.totalInfos += numInfos;
    this.totalWarnings += numWarnings;

    if (numErrors > 0) {
      this.numFailed++;
      status = Status.FAIL;

      LOG.debug("record:sourceUri {}", sourceUri);

      // Note: If the value of sourceUri is null, there will be a
      // java.lang.NullPointerException below.
      // Therefore, sourceUri must be checked against null-ness before calling
      // toString() function.
      if (sourceUri == null) {
        LOG.error(
            "record:sourceUri is null.  A NullPointerException exception will occur when sourceUri.toString() is called.  Cannot continue.  Must return with status {}",
            status);
        return status;
      }
      LOG.debug("record:sourceUri.toString {}", sourceUri.toString());

      if (!Utility.isDir(sourceUri.toString())) {
        if (!this.integrityCheckFlag) {
          this.numFailedProds++;
        } else {
          this.numFailedIntegrityChecks++;
        }
      }
    } else {
      this.numPassed++;
      if (!Utility.isDir(sourceUri.toString())) {
        if (!this.integrityCheckFlag) {
          this.numPassedProds++;
        } else {
          this.numPassedIntegrityChecks++;
        }
      }
    }

    this.numProducts++;

    this.totalProducts = this.numFailedProds + this.numPassedProds - ignoreFromProductCounts;
    this.totalIntegrityChecks = this.numFailedIntegrityChecks + this.numPassedIntegrityChecks
        + this.numSkippedIntegrityChecks;
    printRecordMessages(this.writer, status, sourceUri, problems);
    this.writer.flush();
    return status;
  }

  private void addToMessageSummary(String type) {
    if (this.messageSummary.containsKey(type)) {
      long count = this.messageSummary.get(type).longValue();
      this.messageSummary.put(type, new Long(count + 1));
    } else {
      this.messageSummary.put(type, new Long(1));
    }
  }

  protected Map<String, Long> sortMessageSummary(Map<String, Long> messageSummary) {
    List<Map.Entry<String, Long>> entries = new ArrayList<>(messageSummary.entrySet());
    Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
      @Override
      public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
        int result = 0;
        ExceptionType aKey = getExceptionType(a.getKey());
        ExceptionType bKey = getExceptionType(b.getKey());
        if (aKey.getValue() > bKey.getValue()) {
          result = 1;
        } else if (aKey.getValue() < bKey.getValue()) {
          result = -1;
        } else {
          // keys are the same severity level
          int valueResult = a.getValue().compareTo(b.getValue());
          if (valueResult > 0) {
            result = -1;
          } else if (valueResult < 0) {
            result = 1;
          } else {
            result = 0;
          }
        }
        return result;
      }
    });
    Map<String, Long> sortedMap = new LinkedHashMap<>();
    for (Map.Entry<String, Long> entry : entries) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return sortedMap;
  }

  private ExceptionType getExceptionType(String type) {
    if (type.startsWith("error")) {
      return ExceptionType.ERROR;
    }
    if (type.startsWith("warning")) {
      return ExceptionType.WARNING;
    }
    if (type.startsWith("info")) {
      return ExceptionType.INFO;
    } else {
      return ExceptionType.DEBUG;
    }
  }

  public Status recordSkip(final URI sourceUri, final ValidationProblem problem) {
    this.numSkipped++;
    LOG.debug("recordSkip:sourceUri,numSkipped {},{}", sourceUri, this.numSkipped);
    LOG.debug(
        "recordSkip:sourceUri,problem.getProblem().getSeverity().getValue(),this.level.getValue() {},{},{},{}",
        sourceUri, problem.getProblem().getSeverity().getValue(), this.level.getValue(),
        (problem.getProblem().getSeverity().getValue() <= this.level.getValue()));
    if (!Utility.isDir(sourceUri.toString())) {
      LOG.debug("recordSkip:sourceUri,integrityCheckFlag {},{}", sourceUri,
          this.integrityCheckFlag);
      if (!this.integrityCheckFlag) {
        this.numSkippedProds++;
      } else {
        this.numSkippedIntegrityChecks++;
      }
    }

    printRecordSkip(this.writer, sourceUri, problem);
    this.numProducts++;
    return Status.SKIP;
  }

  protected void printRecordSkip(PrintWriter writer, final URI sourceUri,
      final ValidationProblem problem) {
    // no op
  }

  /**
   * Allows a report to customize how it handles reporting on a particular label.
   *
   * @param writer passed on to write customized messages to
   * @param sourceUri reference to the file that is being reported on
   * @param problems which to report on for this source
   */
  protected abstract void printRecordMessages(PrintWriter writer, final Status status,
      final URI sourceUri, final List<ValidationProblem> problems);

  /**
   * Prints out the footer or the report and calls the customized footer section.
   */
  public void printFooter() {
    printFooter(writer);
    writer.println();

    writer.println("Summary:");
    writer.println();
    writer.println("  " + totalErrors + " error(s)");
    writer.println("  " + totalWarnings + " warning(s)");
    writer.println();
    // issue_132: summary of passed/failed products
    writer.println("  Product Validation Summary:");
    writer.printf("    %-10d product(s) passed\n", this.numPassedProds);
    writer.printf("    %-10d product(s) failed\n", this.numFailedProds);
    writer.printf("    %-10d product(s) skipped\n", this.numSkippedProds);
    // writer.println(" Total # of Product(s) processed: " + getTotalProducts());
    writer.println();
    writer.println("  Referential Integrity Check Summary:");
    writer.printf("    %-10d check(s) passed\n", this.numPassedIntegrityChecks);
    writer.printf("    %-10d check(s) failed\n", this.numFailedIntegrityChecks);
    writer.printf("    %-10d check(s) skipped\n", this.numSkippedIntegrityChecks);
    // writer.println(" Total # of Integrity Check(s) processed: " + getTotalIntegrityChecks());

    writer.println();

    if (!this.messageSummary.isEmpty()) {
      writer.println("  Message Types:");
      Map<String, Long> sortedMessageSummary = sortMessageSummary(this.messageSummary);
      for (String type : sortedMessageSummary.keySet()) {
        writer.print("    ");
        writer.printf("%-10d", sortedMessageSummary.get(type));
        writer.print("   ");
        writer.println(type);
      }
    }
    writer.println();
    writer.println("End of Report");

    if (this.deprecatedFlagWarning) {
      writer.println();
      writer.println(DEPRECATED_FLAG_WARNING_MSG);
      writer.println();
    }
    this.writer.flush();
  }

  /**
   * Allows customization of the footer section of the report
   *
   * @param writer passed on to writer customized footer contents
   */
  protected abstract void printFooter(PrintWriter writer);

  /**
   *
   * @return number of labels that passed (had no errors)
   */
  public int getNumPassed() {
    return this.numPassed;
  }

  /**
   *
   * @return number of labels that failed (had one or more errors)
   */
  public int getNumFailed() {
    return this.numFailed;
  }

  /**
   *
   * @return number of files that were not recognized as a label
   */
  public int getNumSkipped() {
    return this.numSkipped;
  }

  public int getTotalProducts() {
    return this.totalProducts;
  }

  public int getTotalIntegrityChecks() {
    return this.totalIntegrityChecks;
  }

  /**
   *
   * @return total number of errors that were found across all labels inspected. Will not count
   *         errors generated from files that were considered skipped.
   */
  public int getTotalErrors() {
    return this.totalErrors;
  }

  /**
   *
   * @return total number of warning that were found across all labels inspected. Will not count
   *         warnings generated from files that were considered skipped.
   */
  public int getTotalWarnings() {
    return this.totalWarnings;
  }

  /**
   *
   * @return total number of info messages that were found across all labels inspected. Will not
   *         count info messages from files that were considered skipped.
   */
  public int getTotalInfos() {
    return this.totalInfos;
  }

  /**
   *
   * @return flag indicating if errors were found in the inspected files
   */
  public boolean hasErrors() {
    return (this.totalErrors > 0) ? true : false;
  }

  /**
   *
   * @return flag indicating if warnings were found in the inspected files
   */
  public boolean hasWarnings() {
    return (this.totalWarnings > 0) ? true : false;
  }

  /**
   * Enables deprecation warning message when deprecated CLI flags are used
   */
  public void enableDeprecatedFlagWarning() {
    this.deprecatedFlagWarning = true;
  }

  /**
   * Anything at or above the level will be reported. Default ExceptionType level is info and above
   *
   * @param ExceptionType level on which items will be reported
   */
  public void setLevel(ExceptionType ExceptionType) {
    this.level = ExceptionType;
  }

  /**
   *
   * @return ExceptionType level of items that will be reported on. Anything at or above this level
   *         will be reported on
   */
  public ExceptionType getLevel() {
    return this.level;
  }

  public String getType(String systemId) {
    String type = "Fragment";
    String extension = FilenameUtils.getExtension(systemId);
    if ("xsd".equalsIgnoreCase(extension)) {
      type = "Schema";
    } else if ("sch".equalsIgnoreCase(extension)) {
      type = "Schematron";
    }
    return type;
  }
}
