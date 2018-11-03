package Railways.Railways;

import java.util.HashMap;
import java.util.Map;

public enum TrackType {
	
	DOUBLE_ELECTRIC("Double Electric-Line"),
	SINGLE_ELECTRIC("Single Electric-Line"),
	DOUBLE_DIESEL("Double Diesel-Line"),
	CONST_SINGLE_LINE("Construction - Single-Line Electrification"),
	NULL("NULL"),
	SINGLE_DIESEL("Single Diesel-Line"),
	CONST_DOUBLE_DIESEL("Construction - Diesel-Line Doubling"),
	TRIPLE_ELECTRIC("Triple Electric-Line"),
	CONST_DOUBLE_ELECTRIC("Construction - Double-Line Electrification"),
	CONST_DOUBLING_ELECTRIC("Construction - Doubling Electrification"),
	CONST_NEW("Construction - New Line"),
	METRE_GAUGE("Metre Gauge"),
	CONST_ELECTRIC_DOUBLE("Construction - Electric-Line Doubling"),
	CONST_GAUGE("Construction - Gauge Conversion"),
	NARROW_GAUGE("Narrow Gauge"),
	QUAD_ELECTRIC("Quadruple Electric-Line");

	private String type;
	
	TrackType(String type) {
		this.setType(type);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	 //****** Reverse Lookup Implementation************//
	 
    //Lookup table
    private static final Map<String, TrackType> lookup = new HashMap<String, TrackType>();
  
    //Populate the lookup table on loading time
    static
    {
        for(TrackType track : TrackType.values())
        {
            lookup.put(track.getType(), track);
        }
    }
  
    //This method can be used for reverse lookup purpose
    public static TrackType get(String url)
    {
        return lookup.get(url);
    }
  
}
