package com.devops.test

import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import com.beust.jcommander.*

@Slf4j
@Parameters(separators = "=")
public class CommandCommit {
 
  @Parameter(description = "Record changes to the repository")
  public List<String> files;
 
  @Parameter(names = "--amend", description = "Amend")
  public Boolean amend = false;
 
  @Parameter(names = "--author")
  public String author;
}
