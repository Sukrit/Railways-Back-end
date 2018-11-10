package Railways.Railways;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StationDetail {

	private QueryBuilder queryBuilder;
	private String code;
	private Double Distance;
	private String speed; // represented in m/sec
	private Double deltaLat;
	private Double deltaLong;
	public Double getDeltaLong() {
		return deltaLong;
	}

	public void setDeltaLong(Double deltaLong) {
		this.deltaLong = deltaLong;
	}

	private long stopTime; // Stoppage time in second (0 if not a stop)
	private double raminingStopTime;
	private double lat;
	private double longitude;

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public StationDetail() {

	}

	public double getraminingStopTime() {
		return raminingStopTime;
	}

	public void setraminingStopTime(double d) {
		this.raminingStopTime = d;
	}

	public StationDetail(String code, Double distance, String speed2, long stopTime, QueryBuilder queryBuilder) {
		super();
		this.code = code;
		Distance = distance;
		this.speed = speed2;
		this.stopTime = stopTime;
		this.queryBuilder = queryBuilder;

			String sql = "SELECT lat, `long` from station WHERE Code='" + code + "';";
			ResultSet rs = this.queryBuilder.selectCustomQuery(sql);
			try {
				rs.next();
			
			if (rs!=null) {
				this.lat = rs.getDouble("lat");
				this.longitude = rs.getDouble("long");
			}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		

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

	public Double getDeltaLat() {
		return deltaLat;
	}

	public void setDeltaLat(Double deltaLat) {
		this.deltaLat = deltaLat;
	}

}
