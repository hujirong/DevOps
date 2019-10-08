package com.devops.test
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import com.beust.jcommander.*

class CmdManager {
    public static void main(String[] args) {
        // Options available before commands
        CmdManager cm = new CmdManager();
        JCommander jc = new JCommander(cm);

        // Command: add
        CommandAdd add = new CommandAdd();
        jc.addCommand("add", add);

        // Command: commit
        CommandCommit commit = new CommandCommit();
        jc.addCommand("commit", commit);

        jc.parse("commit", "--amend", "--author=cbeust", "A.java", "B.java");
                
        assert jc.getParsedCommand() == "commit"
        assert commit.amend == true
        assert commit.author == "cbeust"
        assert commit.files == Arrays.asList("A.java", "B.java")
    }
}