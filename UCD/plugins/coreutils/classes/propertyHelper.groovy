/*
* Licensed Materials - Property of IBM Corp.
* IBM UrbanCode Deploy
* (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
*
* U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
* GSA ADP Schedule Contract with IBM Corp.
*/
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


public static void outputPropertyChanges(Properties properties, JSONArray oldPropertiesArray, Boolean deleteExtraProps) {

    def oldProperties = new Properties();
    def deleted = new ArrayList<String>();
    def updated = new ArrayList<String>();
    def created = new ArrayList<String>();
    def allPropNames = new ArrayList<String>();
    properties.propertyNames().each() { prop ->
        allPropNames.add(prop);
    }
    int index = 0;
    while ( index < oldPropertiesArray.length() ) {
        JSONObject entry = oldPropertiesArray.get(index);
        String name = entry.get("name");
        oldProperties.setProperty(name, entry.get("value"));
        if (!allPropNames.contains(name)) {
            allPropNames.add(name);
        }
        index++;
    }

    Collections.sort(allPropNames);
    allPropNames.each() { prop ->
        if (properties.getProperty(prop) != null && oldProperties.getProperty(prop) != null) {
            updated.add(prop);
        }
        else {
            if (properties.getProperty(prop) != null) {
               created.add(prop);
            }
            else if(deleteExtraProps) {
                deleted.add(prop);
            }
        }
        
    }
    if (updated.size() > 0) {
        println "Updating the following properties: " + updated.toString().substring(1, updated.toString().length() -1);
    }
    if (created.size() > 0) {
        println "Creating the following properties: " + created.toString().substring(1, created.toString().length() -1);
    }
    if (deleteExtraProps && deleted.size() > 0) {
        println "Deleting the following properties: " + deleted.toString().substring(1, deleted.toString().length() -1);
    }

}