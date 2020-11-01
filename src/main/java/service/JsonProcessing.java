package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import model.ActuatorLink;
import model.LoggerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.CommonOutput;
import tools.GlobalData;
import tools.Tools;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


public class JsonProcessing {
    private final Logger log = LoggerFactory.getLogger(JsonProcessing.class);
    /**
     * The input string is one JSON elements containin all links - NOT as an array:
     *
     * {
     *     "_links": {
     *         "self": {
     *             "href": "http://localhost:9999/actuator",
     *             "templated": false
     *         },
     *         "beans": {
     *             "href": "http://localhost:9999/actuator/beans",
     *             "templated": false
     *         },
     * @param jsonInput
     * @return
     */
    public CommonOutput GetAllActuatorLinks(String jsonInput) {
        CommonOutput out = new CommonOutput();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(jsonInput);
            log.info("Main element: size = " + actualObj.size());


            Iterator<Map.Entry<String, JsonNode>> root = actualObj.fields();
            while(root.hasNext()) {
                Map.Entry<String, JsonNode> entry = root.next();
                String fieldName = entry.getKey();
                log.info("  - Field: " + fieldName);
                if(fieldName.contains("links")) ProcessList(entry.getValue(), out);
            }
            return out;
        }
        catch(Exception ex) {
            out.getErrorMsg().add("Exception: ");
            out.getErrorMsg().add(ex.getMessage());
            return out;
        }
    }

    /**
     *
     * @param root
     * @param out
     */
    private void ProcessList(JsonNode root, CommonOutput out) {
        ArrayList<ActuatorLink> alRes = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> list = root.fields();
        while(list.hasNext()) {
            ActuatorLink acl = new ActuatorLink();
            Map.Entry<String, JsonNode> entry = list.next();
            String fieldName = entry.getKey();
            log.info("  - Link: " + fieldName);
            acl.setName(fieldName);
            JsonNode node = entry.getValue();
            Iterator<Map.Entry<String, JsonNode>> link = node.fields();
            while(link.hasNext()) {
                Map.Entry<String, JsonNode> detail = link.next();
                log.info("      - " + detail.getKey() + " : " + detail.getValue().asText());
                if(detail.getKey().contains("href")) acl.setHref(detail.getValue().asText());
                if(detail.getKey().contains("templated")) {
                    if(detail.getValue().asText().contains("false")) acl.setTemplated(false);
                    else acl.setTemplated(true);
                }
            }
            alRes.add(acl);
        }
        out.setResult(alRes);
    }

    /**
     * Typical input:
     * {
     *     "levels": [
     *         "OFF",
     *         "ERROR",
     *         "WARN",
     *         "INFO",
     *         "DEBUG",
     *         "TRACE"
     *     ],
     *     "loggers": {
     *         "ROOT": {
     *             "configuredLevel": "INFO",
     *             "effectiveLevel": "INFO"
     *         },
     *         "cz": {
     *             "configuredLevel": null,
     *             "effectiveLevel": "INFO"
     *         },
     *
     * @param jsonInput
     * @return
     */
    public CommonOutput GetAllLoggers(String jsonInput) {
        CommonOutput out = new CommonOutput();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(jsonInput);
            log.info("Main element: size = " + actualObj.size());

            Iterator<Map.Entry<String, JsonNode>> root = actualObj.fields();
            while(root.hasNext()) {
                Map.Entry<String, JsonNode> entry = root.next();
                String fieldName = entry.getKey();
                log.info("  - Field: " + fieldName);
                if(fieldName.contains("loggers")) ProcessLoggerList(entry.getValue(), out);
            }
            return out;
        }
        catch(Exception ex) {
            out.getErrorMsg().add("Exception: ");
            out.getErrorMsg().add(ex.getMessage());
            return out;
        }
    }

    /**
     *
     * @param root
     * @param out
     */
    private void ProcessLoggerList(JsonNode root, CommonOutput out) {
        ArrayList<LoggerRecord> alRes = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> list = root.fields();
        while(list.hasNext()) {
            LoggerRecord lr = new LoggerRecord();
            Map.Entry<String, JsonNode> entry = list.next();
            String fieldName = entry.getKey();
            log.info("  - Logger: " + fieldName);
            lr.setName(fieldName);
            JsonNode node = entry.getValue();
            Iterator<Map.Entry<String, JsonNode>> link = node.fields();
            while(link.hasNext()) {
                Map.Entry<String, JsonNode> detail = link.next();
                log.info("      - " + detail.getKey() + " : " + detail.getValue().asText());
                if(detail.getKey().contains("configured")) lr.setConfiguredLevel(detail.getValue().asText());
                if(detail.getKey().contains("effective")) lr.setEffectiveLevel(detail.getValue().asText());
            }
            alRes.add(lr);
        }
        out.setResult(alRes);
    }

    public void ParseJsonToLog(String jsonString, DefaultListModel dlm) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(jsonString);
            log.info("Main element: size = " + actualObj.size());
            ParseOneLevel(actualObj, dlm, 0);
        }
        catch(Exception ex) {
            log.error(ex.getMessage());
        }
    }

    private void ParseOneLevel(JsonNode node, DefaultListModel dlm, int level ) {
        Iterator<Map.Entry<String, JsonNode>> root = node.fields();
        while(root.hasNext()) {
            Map.Entry<String, JsonNode> entry = root.next();
            String fieldName = entry.getKey();
            log.info("  - Field: " + fieldName);

            JsonNode child = entry.getValue();
                switch (child.getNodeType()) {
                    case BOOLEAN:
                    case NUMBER:
                    case STRING:
                        dlm.addElement(Tools.getPadding(level) + fieldName + " : " + child.asText());
                        break;
                    case ARRAY:
                        dlm.addElement(Tools.getPadding(level) + fieldName);
                        ProcessJsonArray((ArrayNode) child, dlm, level + 1);
                        break;
                    default:
                        dlm.addElement(Tools.getPadding(level) + fieldName);
                        ParseOneLevel(child, dlm, level + 1);
                        break;
                }
            }
    }

    private void ProcessJsonArray(ArrayNode node, DefaultListModel dlm, int level) {
        dlm.addElement(Tools.getPadding(level-1) + "[");
        for(int i=0; i<node.size(); i++) {
            JsonNode child = node.get(i);
            if(child.isTextual()) {
                dlm.addElement(Tools.getPadding(level) + child.asText());
            }
            else {
                dlm.addElement(Tools.getPadding(level) + "{");
                ParseOneLevel(node.get(i), dlm, level+1);
                dlm.addElement(Tools.getPadding(level) + "}");
            }
        }
        dlm.addElement(Tools.getPadding(level-1) + "]");
    }

    public CommonOutput GetMetricsList(String jsonString) {
        CommonOutput co = new CommonOutput();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(jsonString);
            JsonNode names = actualObj.get("names");
            ArrayNode n = (ArrayNode) names;
            ArrayList<String> metrics = new ArrayList<>();
            for(int i=0; i<n.size(); i++) {
                metrics.add(n.get(i).asText());
            }
            co.setResult(metrics);
            return(co);
        }
        catch(Exception ex) {
            log.error(ex.getMessage());
            ArrayList<String> al = new ArrayList<>();
            al.add(ex.getMessage());
            co.setErrorMsg(al);
            return(co);
        }
    }
}
