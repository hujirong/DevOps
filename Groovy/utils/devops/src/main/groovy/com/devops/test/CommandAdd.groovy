package com.devops.test
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import com.beust.jcommander.*

public class CommandAdd {
 
  @Parameter(description = "Add file contents to the index")
  public List<String> patterns;
 
  @Parameter(names = "-i")
  public Boolean interactive = false;
}
