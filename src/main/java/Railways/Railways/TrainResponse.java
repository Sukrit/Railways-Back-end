package Railways.Railways;

import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;

@Produces("application/json")
@XmlRootElement 
@Entity
public class TrainResponse {

	private List<TrainDetail> Trains;
	private Map<String, Integer> stationMap;
	private Map<String, String> trackMap;


	public List<TrainDetail> getTrainDetail() {
		return Trains;
	}

	public void setTrainDetail(List<TrainDetail> trainDetail) {
		this.Trains = trainDetail;
	}

	public Map<String, Integer> getStationMap() {
		return stationMap;
	}

	public void setStationMap(Map<String, Integer> stationMap) {
		this.stationMap = stationMap;
	}

	public Map<String, String> getTrackMap() {
		return trackMap;
	}

	public void setTrackMap(Map<String, String> trackMap) {
		this.trackMap = trackMap;
	} 
}
