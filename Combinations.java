package createParameterSets;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Combinations {


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
        	
        type = type.toLowerCase();
        switch (type) {
            case "date":
     
                handleDateType(parameters, current, index, result);
                break;
            case "multivalue":
                handleArrayType(parameters, current, index, result);
                break;
            case "nestedjson":
                NestedJsonCombinations.handleNestedJsonType(parameters, current, index, result);
                break;
            case "singlevalue":
                handleSingleValueType(parameters, current, index, result);
                break;
        }

        if (!"true".equalsIgnoreCase(isRequired)) {
            generateCombinations(parameters, current, index + 1, result);
        }
    }

    private static void handleDateType(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
    	Map<String, Object> parameter = parameters.get(index);
    	List<String> possibleValues = (List<String>) parameter.get("possibleValues");
        
        String startDateKey = null;
        String endDateKey = null;
        
        if (parameter.get("key") instanceof List) {
        	List<String> keys = (List<String>) parameter.get("key");
        	startDateKey = keys.get(0);
            endDateKey = keys.get(1);
            
            for (String dateFrom : possibleValues) {
                for (String dateTo : possibleValues) {
                    Date startDate = Configurations.DATE_PARSER.parse(dateFrom);
                    Date endDate = Configurations.DATE_PARSER.parse(dateTo);

                    if (startDate.before(endDate)) {

    		            	current.addProperty(startDateKey, dateFrom);
    		                current.addProperty(endDateKey, dateTo);
    		                generateCombinations(parameters, current, index + 1, result);
    		                current.remove(startDateKey);
    		                current.remove(endDateKey);
                        }
                    }
                }
        }
        else {
        	handleSingleValueType(parameters, current, index, result);
        }
        
    }
    

    private static void handleArrayType(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
        // Generates combinations for multi-valued parameters
    	Map<String, Object> parameter = parameters.get(index);
    	List<Object> possibleValues = (List<Object>) parameter.get("possibleValues");
        String key = (String) parameter.get("key");
        for (int i = 0; i < (1 << possibleValues.size()); i++) {
            JsonArray combination = new JsonArray();
            for (int j = 0; j < possibleValues.size(); j++) {
                if ((i & (1 << j)) > 0) {
                	Object value = possibleValues.get(j);
                	if (value instanceof Double) {
                        combination.add(((Double) value).intValue());
                	}
                	else {
                        combination.add((String) value);

                	}
                }
            }
            if (combination.size() > 0) {
            	if (combination.size() <= Configurations.NUMBER_OF_COMBINATIONS_FOR_ARRAYS) {
            		current.add(key, combination);
            	}
                generateCombinations(parameters, current, index + 1, result);
                
            	if (combination.size() <= Configurations.NUMBER_OF_COMBINATIONS_FOR_ARRAYS) {
            		current.remove(key);
            	}
            }
        }
    }
    
    private static void handleSingleValueType(List<Map<String, Object>> parameters, JsonObject current, int index, List<JsonObject> result) throws ParseException {
        // Generates combinations for single-valued parameters
    	Map<String, Object> parameter = parameters.get(index);
    	List<Object> possibleValues = (List<Object>) parameter.get("possibleValues");
        String key = (String) parameter.get("key");

        for (Object value : possibleValues) {
            if (Utilities.isDouble(value.toString())) {
                current.addProperty(key, Integer.parseInt((String) value));
            } else {
                current.addProperty(key, value.toString());
            }
            generateCombinations(parameters, current, index + 1, result);
            current.remove(key);
        }
    }

    

}