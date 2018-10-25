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
}
