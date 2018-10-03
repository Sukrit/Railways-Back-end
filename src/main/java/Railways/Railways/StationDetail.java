package Railways.Railways;

public class StationDetail {
	
	private String code;
	private Double Distance;
	private String speed; //represented in m/sec
	private long stopTime; //Stoppage time in second (0 if not a stop)
	private double raminingStopTime;
	
	public StationDetail() {
		
	}
	
	public double getraminingStopTime() {
		return raminingStopTime;
	}

	public void setraminingStopTime(double d) {
		this.raminingStopTime = d;
	}

	public StationDetail(String code, Double distance, String speed2, long stopTime) {
		super();
		this.code = code;
		Distance = distance;
		this.speed = speed2;
		this.stopTime = stopTime;
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public Double getDistance() {
		return Distance;
	}
	public void setDistance(Double distance) {
		Distance = distance;
	}
	public String getSpeed() {
		return speed;
	}
	public void setSpeed(String speed) {
		this.speed = speed;
	}
	public long getStopTime() {
		return stopTime;
	}
	public void setStopTime(long stopTime) {
		this.stopTime = stopTime;
	}

}
