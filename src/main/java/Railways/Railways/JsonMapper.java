package Railways.Railways;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapper {
	
	public static String mapObjectToJson(Object object) {
		ObjectMapper mapper = new ObjectMapper();
		
		String jsonInString = null;
		//Object to JSON in String
		try {
			jsonInString = mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonInString;
	}
	
	public String mapObjectToJson(List<Object> objects) {
		ObjectMapper mapper = new ObjectMapper();
		
			
		
		String jsonInString = null;
		//Object to JSON in String
		try{
			jsonInString = mapper.writeValueAsString(objects);
			
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonInString;
	}


}
