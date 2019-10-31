// Search all .json files, including sub-folders
// Merge the contents into one file ibm-ucd-resources.json
package com.devops.test

File file = new File("ibm-ucd-resources.json");
if (file.isDirectory()) {
    throw new Exception("Directory exists in the way");
}
if (file.isFile()) {
   System.out.println("File already created");
}
else {
    file.append("[\n", "utf-8");
    File dir = new File(".");
    boolean first = true; 
    dir.eachFileRecurse { curfile ->
        if (curfile.getName().endsWith(".json") && !curfile.getName().equals("ibm-ucd-resources.json")) { 
            if (first) {
                first = false;
            }
            else {
                file.append(",\n", "utf-8");
            }
            curfile.eachLine { line -> 
                file.append("\n", "utf-8");
                file.append(line);  /* Input JSON files are already UTF-8 encoded */
            }
        }
    }
    file.append("\n]", "utf-8");
}
