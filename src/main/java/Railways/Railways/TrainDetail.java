package Railways.Railways;

import java.util.List;

public class TrainDetail {
	
	private String Number;
	private String Name;
	private String CurrentLocation;
	private double currentLat;
	private double currentLong;
	private String departureTime;
	private String finalDestination;
	private String Delayed;
	private String PercentCompleted;
	private String DistanceCovered;
	private List<StationDetail> stationsCovered;
	private List<StationDetail> stationsPending;
	private boolean justStarted;
	private String startLocation;
	private double timeStarted;
	
	double getTimeStarted() {
		return timeStarted;
	}
	public void setTimeStarted(double d) {
		this.timeStarted = d;
	}
	
	public String getNumber() {
		return Number;
	}
	public void setNumber(String number) {
		Number = number;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getCurrentLocation() {
		return CurrentLocation;
	}
	public void setCurrentLocation(String station) {
		CurrentLocation = station;
	}
	public String getDelayed() {
		return Delayed;
	}
	public void setDelayed(String delayed) {
		Delayed = delayed;
	}
	public String getPercentCompleted() {
		return PercentCompleted;
	}
	public void setPercentCompleted(String percentCompleted) {
		PercentCompleted = percentCompleted;
	}
	public String getDistanceCovered() {
		return DistanceCovered;
	}
	public void setDistanceCovered(String distanceCovered) {
		DistanceCovered = distanceCovered;
	}
	public List<StationDetail> getStationsCovered() {
		return stationsCovered;
	}
	public void setStationsCovered(List<StationDetail> stationsCovered) {
		this.stationsCovered = stationsCovered;
	}
	public List<StationDetail> getStationsPending() {
		return stationsPending;
	}
	public void setStationsPending(List<StationDetail> stationsPending) {
		this.stationsPending = stationsPending;
	}
	public String getFinalDestination() {
		return finalDestination;
	}
	public void setFinalDestination(String finalDestination) {
		this.finalDestination = finalDestination;
	}
	public String getDepartureTime() {
		return departureTime;
	}
	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}
	public boolean isJustStarted() {
		return justStarted;
	}
	public void setJustStarted(boolean justStarted) {
		this.justStarted = justStarted;
	}
	public String getStartLocation() {
		return startLocation;
	}
	public void setStartLocation(String startLocation) {
		this.startLocation = startLocation;
	}
	public double getCurrentLong() {
		return currentLong;
	}
	public void setCurrentLong(double currentLong) {
		this.currentLong = currentLong;
	}
	public double getCurrentLat() {
		return currentLat;
	}
	public void setCurrentLat(double currentLat) {
		this.currentLat = currentLat;
	}

}
