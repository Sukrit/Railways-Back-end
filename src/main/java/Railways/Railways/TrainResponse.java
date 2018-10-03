package Railways.Railways;

import java.util.List;

import javax.persistence.Entity;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;

@Produces("application/json")
@XmlRootElement 
@Entity
public class TrainResponse {

	private List<TrainDetail> Trains;

	public List<TrainDetail> getTrainDetail() {
		return Trains;
	}

	public void setTrainDetail(List<TrainDetail> trainDetail) {
		this.Trains = trainDetail;
	} 
}
