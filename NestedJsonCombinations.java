package createParameterSets;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NestedJsonCombinations {
	public static void generateCombinations(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
        if (index == parameters.size() || parameters.isEmpty()) {
            if (!current.entrySet().isEmpty()) {
                result.add(current.deepCopy());
            }
            return;
        }

        String type = null;
        String isRequired = null;
        try {
            Map<String, Object> parameter = parameters.get(index);
            type = (String) parameter.get("type");
            isRequired = (String) parameter.get("required");
            type = type.toLowerCase();

        }
        catch (NullPointerException e) {
        	System.out.println(String.format("Incorrect parameter: %s", parameters.toString()));
        	throw new IllegalArgumentException("The parameter is missing a part (key, type, possibleValues, etc.)");
        	
        }
        
        switch (type) {
	        case "date":
	            handleDateType(parameters, current, index, result);
	        	break;
	        case "multivalue":
	        	handleArrayType(parameters, current, index, result);
	            break;
	        case "nestedjson":
		        handleNestedJsonType(parameters, current, index, result);
		        break;
	           
	        case "singlevalue":
	            handleSingleValueType(parameters, current, index, result);
	            break;
        }
        

        if (!"true".equalsIgnoreCase(isRequired)) {
            generateCombinations(parameters, current, index + 1, result);
        }
    }

	private static void handleSingleValueType(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
    	Map<String, Object> parameter = parameters.get(index);
    	List<String> possibleValues = (List<String>) parameter.get("possibleValues");
    	String key = (String) parameter.get("key");

        String type = (String) parameter.get("type");
    	for (Object value : possibleValues) {
            JsonObject valueObject = new JsonObject();
            if (Utilities.isDouble(value.toString())) {
                valueObject.addProperty("value", ((Double) value).intValue());
                type = "number";
            } else {
                valueObject.addProperty("value", (String) value);
                type = "string";
            }
            valueObject.addProperty("type", type);

            current.add(key, valueObject);
            Combinations.generateCombinations(parameters, current, index + 1, result);
            current.remove(key);
        }
    }
    
	public static void handleNestedJsonType(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
        
    	Map<String, Object> parameter = parameters.get(index);
		String key = (String) parameter.get("key");
        List<Map<String, Object>> possibleValues = (List<Map<String, Object>>) parameter.get("possibleValues");
        List<JsonObject> nestedJsonCombinations = new ArrayList<>();
        
        // Generate combinations among the NestedJson "possibleValues"
        generateCombinations(possibleValues, new JsonObject(), 0, nestedJsonCombinations);
        

        for (JsonObject nestedJsonCombination : nestedJsonCombinations) {
            current.addProperty(key, nestedJsonCombination.toString());
            Combinations.generateCombinations(parameters, current, index + 1, result); 
            current.remove(key);
        }
    }
	

    
    private static void handleDateType(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
        Set<String> singleValueOperations = new HashSet<>(Arrays.asList("eq", "lt", "gt", "lte", "gte"));
    	
        Map<String, Object> parameter = parameters.get(index);
    	List<String> possibleValues = (List<String>) parameter.get("possibleValues");
    	String key = (String) parameter.get("key");
        String operation = (String) parameter.get("operation");
        String type = (String) parameter.get("type");

        
        for (String dateFrom : possibleValues) {
            JsonObject valueObject = new JsonObject();
            if ("range".equalsIgnoreCase(operation)) {
                for (String dateTo : possibleValues) {
                    Date startDate = Configurations.DATE_PARSER.parse(dateFrom);
                    Date endDate = Configurations.DATE_PARSER.parse(dateTo);

                    if (startDate.before(endDate)) {
                        JsonArray dateValues = new JsonArray();
                        dateValues.add(dateFrom);
                        dateValues.add(dateTo);

                        valueObject.add("value", dateValues);
                        valueObject.addProperty("operation", operation);
                        
                        
           
                        current.add(key, valueObject);
                        generateCombinations(parameters, current, index + 1, result);
                        current.remove(key);
                    }
                }
            } else if (singleValueOperations.contains(operation)) {
            	

                valueObject.addProperty("value", dateFrom);
                valueObject.addProperty("type", type);
                valueObject.addProperty("operation", operation);

                if (current.has(key)) {	
                	short counter = 0;
                	String newkey = key;
                	while (current.has(newkey)) {
                		counter++;
                		newkey = key + Configurations.KEY_SUFFIX + counter;
                	}

                	String existingOperation = current.getAsJsonObject(key).get("operation").getAsString();

                	boolean hasCorrectRangeType = (existingOperation.equals("gt") || existingOperation.equals("gte")) 
                										&& (operation.equals("lt") || operation.equals("lte"));
                	if (hasCorrectRangeType) {
                        current.add(newkey, valueObject);
                        generateCombinations(parameters, current, index + 1, result);
                        current.remove(newkey);                		
                	}
                
                }
                else {
                    current.add(key, valueObject);
                    generateCombinations(parameters, current, index + 1, result);
                    current.remove(key);                	
                }

            }
        }
    }
    
    private static void handleArrayType(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
        // Generates combinations for multi-valued parameters
    	Map<String, Object> parameter = parameters.get(index);
    	List<Object> possibleValues = (List<Object>) parameter.get("possibleValues");
    	String key  = (String) parameter.get("key");
        String type = null;
        String operation = (String) parameter.get("operation");
        
        for (int i = 0; i < (1 << possibleValues.size()); i++) {
            JsonArray combination = new JsonArray();
            JsonObject valueObject = new JsonObject();
            for (int j = 0; j < possibleValues.size(); j++) {
                if ((i & (1 << j)) > 0) {
                	Object value = possibleValues.get(j);
                	

                	if (value instanceof Double) {
                		type = "number";
                		combination.add(((Double) value).intValue());
                	}
                	else {
                		type = "string";
                		combination.add((String) value);
                	}
                	
                	valueObject.add("value", combination);
                	valueObject.addProperty("type", type);
                	
                	if (operation!=null) {
                    	valueObject.addProperty("operation", operation);
                	}

                }
            	
            }
            if (combination.size() > 0) {
        		current.add(key, valueObject);
                generateCombinations(parameters, current, index + 1, result);                
        		current.remove(key);
            }
        }
    }

}