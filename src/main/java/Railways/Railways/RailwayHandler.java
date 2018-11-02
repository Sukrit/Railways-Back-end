package Railways.Railways;

import org.o7planning.generateentities.Station;
import org.o7planning.generateentities.Train;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;

@Path("/trainStatus")
public class RailwayHandler {

	/*
	 * This function should perform the following tasks 1) Fetch new trains from the
	 * DB - with their designated paths 2) Move the already fetched trains - update
	 * their status
	 */
	private static boolean STATION_CHECK = false;
	private static boolean TRACK_CHECK = false;

	private static QueryBuilder queryBuilder = new QueryBuilder();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response trainStatus(@QueryParam("startTime") String startTime, @QueryParam("endTime") String endTime,
			@QueryParam("startDay") String startDay, @QueryParam("stationCheck") String stationCheck,
			@QueryParam("trackCheck") String trackCheck, TrainResponse trainResponse) throws JSONException {

		TRACK_CHECK = Boolean.valueOf(trackCheck);
		STATION_CHECK = Boolean.valueOf(stationCheck);
		// System.out.println("Receiving request " +
		// JsonMapper.mapObjectToJson(trainResponse));
		// Start new trains
		startTrains(startTime, endTime, trainResponse, startDay);

		// populate path for started trains
		populatePath(trainResponse);

		// Populate direction of progress of each train
		populateCoordinateSlopes(trainResponse);

		// move all trains
		moveAll(startTime, endTime, trainResponse);

		// terminate trains
		terminateTrains(trainResponse);

		// System.out.println("Sending response " +
		// JsonMapper.mapObjectToJson(trainResponse));
		// queryBuilder.close();

		return Response.status(200).entity(JsonMapper.mapObjectToJson(trainResponse)).build();
	}

	private void populateCoordinateSlopes(TrainResponse trainResponse) {
		double deltaLat = 0;
		double deltaLong = 0;
		for (TrainDetail trainDetail : trainResponse.getTrainDetail()) {
			if (trainDetail.getDistanceCovered() == "0") {
				StationDetail last = getLastCovered(trainDetail.getStationsCovered());

				for (StationDetail station : trainDetail.getStationsPending()) {
					if (station.getLat() != 0 && station.getLongitude() != 0 && last.getLat() != 0
							&& last.getLongitude() != 0) {

						deltaLat = station.getLat() - last.getLat();
						deltaLong = station.getLongitude() - last.getLongitude();

					} else {
						deltaLat = 0;
						deltaLong = 0;
					}
					station.setDeltaLat(deltaLat);
					station.setDeltaLong(deltaLong);
					last = station;
				}
			}
		}

	}

	private StationDetail getLastCovered(List<StationDetail> stationsCovered) {
		double max = -1;
		StationDetail last = null;
		for (StationDetail station : stationsCovered) {
			if (station.getDistance() > max) {
				last = station;
				max = station.getDistance();
			}
		}
		return last;
	}

	private void terminateTrains(TrainResponse trainResponse) {
		List<TrainDetail> newdetails = new ArrayList<TrainDetail>();
		for (TrainDetail trainDetail : trainResponse.getTrainDetail()) {
			if (!trainDetail.getCurrentLocation().equals("Terminated")) {

				newdetails.add(trainDetail);
			}
		}
		trainResponse.setTrainDetail(newdetails);
	}

	private void moveAll(String startTime, String endTime, TrainResponse trainResponse) {

		for (TrainDetail trainDetail : trainResponse.getTrainDetail()) {

			double range = 0.0;
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			try {
				Date dateEnd = sdf.parse(endTime);
				Date dateStart = sdf.parse(startTime);

				range = (dateEnd.getTime() - dateStart.getTime()) / 1000;
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (trainDetail.isJustStarted()) {
				range = trainDetail.getTimeStarted();
				trainDetail.setJustStarted(false);
			}

			while (range > 0) {

				StationDetail prevStation = getPrevStation(trainDetail.getStationsCovered());

				if (trainDetail.getCurrentLocation().equals("Moving")) {

					if (trainDetail.getStationsPending().isEmpty()) {
						trainDetail.setCurrentLocation("Terminated");
						break;
					}
					StationDetail station = getNextStation(trainDetail.getStationsPending(),
							trainDetail.getDistanceCovered());
					double maxDistance = getMaxDistance(trainDetail.getStationsPending());
					if (station == null) {
						System.out.println("Empty station for " + JsonMapper.mapObjectToJson(trainDetail));
					}
					double distancePending = station.getDistance() - Double.valueOf(trainDetail.getDistanceCovered()); // Distance
																														// to
																														// travel
					double tempDistance = (Double.valueOf(station.getSpeed()) * range) / 1000.0; // potential to travel

					if (tempDistance < distancePending) { // Continue moving on the tracks
						trainDetail.setDistanceCovered(
								String.valueOf(Double.valueOf(trainDetail.getDistanceCovered()) + tempDistance));
						trainDetail.setPercentCompleted(
								String.valueOf(Double.valueOf(trainDetail.getDistanceCovered()) * 100.0 / maxDistance));
						if (station.getDeltaLat() != 0 || station.getDeltaLong() != 0) { // Train can move horizontally
																							// or vertically
							trainDetail.setCurrentLat(
									trainDetail.getCurrentLat() + (tempDistance * station.getDeltaLat()) / 111.0);
							trainDetail.setCurrentLong(trainDetail.getCurrentLong()
									+ (tempDistance * station.getDeltaLong()) / getLongDifference(station.getLat()));

						}
						range = 0;
					} else {
						// go till station
						double time = distancePending * 1000.0 / Double.valueOf(station.getSpeed());
						trainDetail.setDistanceCovered(
								String.valueOf(Double.valueOf(trainDetail.getDistanceCovered()) + distancePending));
						trainDetail.setPercentCompleted(
								String.valueOf(Double.valueOf(trainDetail.getDistanceCovered()) * 100.0 / maxDistance));

						range = range - time;
						trainDetail.setCurrentLocation(station.getCode());
						if (station.getLat() != 0 && station.getLongitude() != 0) {
							trainDetail.setCurrentLat(station.getLat());
							trainDetail.setCurrentLong(station.getLongitude());
						}

					}

				} else {// Train is at a station
					StationDetail stationDetail = getStationByCode(trainDetail.getCurrentLocation(), trainDetail);
					if (stationDetail.getStopTime() == 0) { // Train is passing through a station

						if (isTrackAvailable(stationDetail,
								getNextStation(trainDetail.getStationsPending(), trainDetail.getDistanceCovered()))) {
							// move this to done
							moveToVisited(trainDetail, stationDetail.getCode());
							// carry on moving
							trainDetail.setCurrentLocation("Moving");

							updateTrackStatus(stationDetail,
									getNextStation(trainDetail.getStationsPending(), trainDetail.getDistanceCovered()),
									"Occupied");
						} else {
							trainDetail.setDelayed("true");
							range = 0;
							trainDetail.setLateBy(trainDetail.getLateBy() + range);
							if (isPlatformAvailable(trainResponse, stationDetail)) {
								updateStationStatus(trainResponse, stationDetail, "Occupy");
							}
						}
					} else {// Train has stopped at a station

						if (stationDetail.getraminingStopTime() > 0) {// Stopped in a previous cycle
							if (range < stationDetail.getraminingStopTime()) { // Continue to wait at the station
								stationDetail.setraminingStopTime(stationDetail.getraminingStopTime() - range);
								range = 0;
							} else {

								range = range - stationDetail.getraminingStopTime();
								stationDetail.setraminingStopTime(0.0);

								if (isTrackAvailable(stationDetail, getNextStation(trainDetail.getStationsPending(),
										trainDetail.getDistanceCovered()))) {
									// move this to done
									moveToVisited(trainDetail, stationDetail.getCode());
									// carry on moving
									trainDetail.setCurrentLocation("Moving");

									updateTrackStatus(stationDetail, getNextStation(trainDetail.getStationsPending(),
											trainDetail.getDistanceCovered()), "Occupied");

									updateStationStatus(trainResponse, stationDetail, "Release");

								} else {
									setTrainAsLate(trainDetail, range, stationDetail);
									range = 0;
								}

							}
						} else {// New scheduled stoppage

							if (isPlatformAvailable(trainResponse, stationDetail)) {
								updateStationStatus(trainResponse, stationDetail, "Occupy");
								updateTrackStatus(prevStation, stationDetail, "Available");

								if (range < stationDetail.getStopTime()) {
									stationDetail.setraminingStopTime(stationDetail.getStopTime() - range);
									range = 0;
								} else {

									range = range - stationDetail.getStopTime();

									if (isTrackAvailable(stationDetail, getNextStation(trainDetail.getStationsPending(),
											trainDetail.getDistanceCovered()))) {
										// move this to done
										moveToVisited(trainDetail, stationDetail.getCode());
										// carry on moving
										trainDetail.setCurrentLocation("Moving");

										updateStationStatus(trainResponse, stationDetail, "Release");

										updateTrackStatus(stationDetail,
												getNextStation(trainDetail.getStationsPending(),
														trainDetail.getDistanceCovered()),
												"Occupied");
									} else {
										setTrainAsLate(trainDetail, range, stationDetail);
										range = 0;

									}

								}
							}

							else { // can't find a platform
								trainDetail.setCurrentLocation("Moving");

								updateTrackStatus(prevStation, stationDetail, "Occupied");
								setTrainAsLate(trainDetail, range, stationDetail);
								range = 0;

							}
						}

					}

				}
			}

		}
	}

	private void setTrainAsLate(TrainDetail trainDetail, double range, StationDetail stationDetail) {
		trainDetail.setLateBy(trainDetail.getLateBy() + range);
		trainDetail.setDelayed("true");

		System.out.println("Train delayed at station : " + JsonMapper.mapObjectToJson(stationDetail.getCode()));
	}

	private void updateStationStatus(TrainResponse trainResponse, StationDetail stationDetail, String string) {
		if (!STATION_CHECK) {
			return;
		}

		if (string.equals("Occupy")) {
			trainResponse.getStationMap().put(stationDetail.getCode(),
					trainResponse.getStationMap().get(stationDetail.getCode()) - 1);
		} else {
			trainResponse.getStationMap().put(stationDetail.getCode(),
					trainResponse.getStationMap().get(stationDetail.getCode()) + 1);
		}

	}

	private boolean isPlatformAvailable(TrainResponse trainResponse, StationDetail stationDetail) {
		if (!STATION_CHECK) {
			return true;
		}
		if (trainResponse.getStationMap().get(stationDetail.getCode()) > 0) {
			return true;
		}
		return false;
	}

	private StationDetail getPrevStation(List<StationDetail> stationsCovered) {
		double max = -50.0;
		StationDetail stationMax = null;
		for (StationDetail station : stationsCovered) {
			if (station.getDistance() > max) {
				max = station.getDistance();
				stationMax = station;
			}
		}

		return stationMax;
	}

	private void updateTrackStatus(StationDetail currentStation, StationDetail nextStation, String status) {
		if (!TRACK_CHECK) {
			return;
		}
		
		if(nextStation==null) {
			return;
		}

		String sql = "SELECT TYPE FROM NODE WHERE `FROM`='" + currentStation.getCode() + "' AND `TO`='"
				+ nextStation.getCode() + "';";

		ResultSet rs = queryBuilder.selectCustomQuery(sql);
		try {
			if (rs == null) {
				return;
			}
			while (rs.next()) {

				String type = rs.getString("TYPE");

				if (isDoubleLine(type)) {
					// mark one way as occupied
					sql = "UPDATE NODE SET STATUS ='" + status + "' WHERE `from` = '" + currentStation.getCode()
							+ "' and `to` = '" + nextStation.getCode() + "';";
					queryBuilder.updateCustomQuery(sql);

				} else if (isSingleLine(type)) {
					// mark both ways as occupied
					sql = "UPDATE NODE SET STATUS ='" + status + "' WHERE `from` = '" + currentStation.getCode()
							+ "' and `to` = '" + nextStation.getCode() + "';";
					queryBuilder.updateCustomQuery(sql);

					sql = "UPDATE NODE SET STATUS ='" + status + "' WHERE `from` = '" + nextStation.getCode()
							+ "' and `to` = '" + currentStation.getCode() + "';";
					queryBuilder.updateCustomQuery(sql);
				} else {
					return;
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private boolean isSingleLine(String trackType) {

		if (trackType == null) {
			return false;
		}
		TrackType type = TrackType.get(trackType.trim());
		if (type == null) {
			System.out.println("Returning null for " + trackType);
			return false;
		}
		switch (type) {

		case SINGLE_ELECTRIC:
			return true;
		case CONST_SINGLE_LINE:
			return true;
		case SINGLE_DIESEL:
			return true;
		case METRE_GAUGE:
			return true;
		case NARROW_GAUGE:
			return true;
		case CONST_NEW:
			return true;
		default:
			return false;

		}
	}

	private boolean isDoubleLine(String trackType) {

		if (trackType == null) {
			return false;
		}
		TrackType type = TrackType.get(trackType.trim());
		if (type == null) {
			System.out.println("Returning null for " + trackType);
			return false;
		}
		switch (type) {

		case DOUBLE_ELECTRIC:
			return true;
		case DOUBLE_DIESEL:
			return true;
		case CONST_DOUBLE_DIESEL:
			return true;
		case CONST_DOUBLE_ELECTRIC:
			return true;
		case CONST_DOUBLING_ELECTRIC:
			return true;
		case CONST_ELECTRIC_DOUBLE:
			return true;
		default:
			return false;

		}
	}

	private boolean isTrackAvailable(StationDetail currentStation, StationDetail nextStation) {
		if (!TRACK_CHECK) {
			return true;
		}

		String sql = "SELECT STATUS FROM NODE WHERE `FROM`='" + currentStation.getCode() + "' AND `TO`='"
				+ nextStation.getCode() + "';";
		ResultSet rs = queryBuilder.selectCustomQuery(sql);
		try {
			while (rs.next()) {
				if (rs.getString("STATUS").equals("Occupied")) {
					return false;
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	private double getLongDifference(double lat) {
		return 109.87 - ((lat - 8.08) * 4.23);
	}

	private void moveToVisited(TrainDetail trainDetail, String code) {

		StationDetail tempStation = null;
		for (StationDetail station : trainDetail.getStationsPending()) {
			if (station.getCode().equals(code)) {
				trainDetail.getStationsCovered().add(station);
				tempStation = station;
			}
		}
		trainDetail.getStationsPending().remove(tempStation);

	}

	private StationDetail getStationByCode(String currentLocation, TrainDetail trainDetail) {

		for (StationDetail station : trainDetail.getStationsPending()) {
			if (station.getCode().equals(currentLocation)) {
				return station;
			}
		}

		System.out.println("Returning null for " + currentLocation + " and " + JsonMapper.mapObjectToJson(trainDetail));
		return null;
	}

	private double getMaxDistance(List<StationDetail> stationsPending) {
		double max = 0.0;
		for (StationDetail station : stationsPending) {
			if (station.getDistance() > max) {
				max = station.getDistance();
			}
		}

		return max;
	}

	private StationDetail getNextStation(List<StationDetail> stationsPending, String distanceCovered) {

		double min = 100000.0;
		StationDetail stationMin = null;
		for (StationDetail station : stationsPending) {
			if ((station.getDistance() - Double.valueOf(distanceCovered)) < min) {
				min = station.getDistance() - Double.valueOf(distanceCovered);
				stationMin = station;
			}
		}

		return stationMin;

	}

	private void populatePath(TrainResponse trainResponse) {
		for (TrainDetail trainDetail : trainResponse.getTrainDetail()) {
			if (trainDetail.getDistanceCovered() == "0") {// Recently added trains

				try {

					ResultSet rs = queryBuilder.selectStopsQuery(trainDetail.getNumber());

					long prevDepart = Time.valueOf(trainDetail.getDepartureTime()).getTime();
					String prevStation = trainDetail.getStartLocation();
					String prevDistance = "0";

					while (rs.next()) {
						// fetch distance for each stop
						if (!(rs.getString("Station")).equals(prevStation)) {

							ResultSet rs2 = queryBuilder.selectRouteQuery(prevStation, rs.getString("Station"));
							rs2.next();
							long waitingTime = ((rs.getTime("Departure Time")).getTime()
									- (rs.getTime("Arrival Time")).getTime()) / 1000;
							;
							String distance = String.valueOf(rs2.getInt("Distance"));
							long tempTravelTime = ((rs.getTime("Arrival Time")).getTime() - prevDepart);

							// Change of day
							if (tempTravelTime < 0) {
								tempTravelTime = tempTravelTime + 86400000;
							}
							double speed = (Double.valueOf(distance) * 1000000.0 / tempTravelTime);
							StationDetail station = new StationDetail(rs.getString("Station"),
									Double.valueOf(distance) + Double.valueOf(prevDistance), String.valueOf(speed),
									waitingTime, queryBuilder);

							// fetch train number/serial number of train covering max stations and almost
							// equal to the distance
							String newSql = "select max(k.`serial number`-s.`serial number`) as boo"
									+ " from stop s, stop k where s.station='" + prevStation + "' and k.station= '"
									+ rs.getString("Station") + "' "
									+ "and s.`train number`=k.`train number` order by boo desc limit 1";
							ResultSet rs3 = queryBuilder.selectCustomQuery(newSql);
							rs3.next();
							int maxCount = rs3.getInt("boo");

							// call a method to fetch sub-stops and distance (FUCK!!)
							List<StationDetail> stationList = fetchAllStations(prevStation, rs.getString("Station"),
									maxCount, distance, station, speed, Integer.parseInt(prevDistance), waitingTime);

							// populate sub-stops, distance and speed
							trainDetail.getStationsPending().addAll(stationList);

							prevDistance = String.valueOf(Integer.parseInt(prevDistance) + Integer.parseInt(distance));
							prevStation = rs.getString("Station");
							prevDepart = rs.getTime("Departure Time").getTime();
						}

					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				StationDetail stationDetail = getStationByCode(trainDetail.getFinalDestination(), trainDetail);
				stationDetail.setStopTime(900);
			}
		}
	}

	private List<StationDetail> fetchAllStations(String prevStation, String toStation, int maxCount, String distance,
			StationDetail station, double speed, int prevDistance, long waitingTime) {

		try {
			int count = maxCount;
			while (count != 1) {
				String sql = "select s.`train number`,s.`serial number` from stop s, stop k where s.station='"
						+ prevStation + "' and k.station='" + toStation
						+ "' and s.`train number`= k.`train number` and (k.`serial number`- s.`serial number`) ="
						+ count;

				ResultSet rs = queryBuilder.selectCustomQuery(sql);

				while (rs.next()) {
					if (getDistance(rs.getInt("Train Number"), rs.getInt("Serial Number"),
							count) < (Integer.parseInt(distance) + 5)) {

						return populate(rs.getInt("Train Number"), rs.getInt("Serial Number"), count, speed,
								prevDistance, waitingTime);
					}

				}

				count--;
			}

			List<StationDetail> stn = new ArrayList<StationDetail>();
			stn.add(station);
			return stn;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	private List<StationDetail> populate(int trainNumber, int serialNumber, int count, double speed, int prevDistance,
			long waitingTime) {
		try {
			List<StationDetail> stationDetail = new ArrayList<StationDetail>();
			int cumDistance = 0;
			for (int i = serialNumber; i < (serialNumber + count); i++) {

				String sql = "(select station from stop where `train number`= " + trainNumber
						+ " and `serial number` = " + (i + 1) + ")";
				ResultSet rs = queryBuilder.selectCustomQuery(sql);
				rs.next();
				String stationCode = rs.getString("Station");

				sql = "select distance from route where `from` = " + "(select station from stop where `train number`= "
						+ trainNumber + " and `serial number` = " + i + ") and `to`= '" + stationCode + "'";
				rs = queryBuilder.selectCustomQuery(sql);
				rs.next();
				int PartDistance = rs.getInt("Distance");
				cumDistance = cumDistance + PartDistance;
				long stopTime = 0;
				if ((i + 1) == (serialNumber + count)) {
					stopTime = waitingTime;
				}
				StationDetail station = new StationDetail(stationCode, Double.valueOf(cumDistance + prevDistance),
						String.valueOf(speed), stopTime, queryBuilder);
				stationDetail.add(station);

			}

			return stationDetail;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<StationDetail>();

	}

	private int getDistance(int trainNumber, int serialNumber, int count) {
		int distance = 0;

		try {

			for (int i = serialNumber; i <= (serialNumber + count - 1); i++) {
				String sql = "select distance from route where `from` = "
						+ "(select station from stop where `train number`= " + trainNumber + " and `serial number` = "
						+ i + ") and `to`= " + "(select station from stop where `train number`= " + trainNumber
						+ " and `serial number` = " + (i + 1) + ")";
				ResultSet rs = queryBuilder.selectCustomQuery(sql);

				rs.next();
				distance = distance + rs.getInt("Distance");

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return distance;
	}

	private void startTrains(String startTime, String endTime, TrainResponse trainResponse, String startDay) {
		if (trainResponse.getStationMap() == null) {
			trainResponse.setStationMap(new HashMap<String, Integer>());
		}
		List<Train> trainList = fetchTrains(startTime, endTime, startDay, trainResponse);
		List<TrainDetail> trainDetailList = new ArrayList<TrainDetail>();
		for (Train train : trainList) {
			TrainDetail trainDetail = new TrainDetail();
			trainDetail.setCurrentLocation("Moving");
			trainDetail.setStartLocation(train.getStationByFrom().getCode());
			trainDetail.setFinalDestination(train.getStationByTo().getCode());
			trainDetail.setDelayed("false");
			trainDetail.setDistanceCovered("0");
			trainDetail.setName(train.getName());
			trainDetail.setNumber(new Integer(train.getNumber()).toString());
			trainDetail.setPercentCompleted("0");
			trainDetail.setDepartureTime(String.valueOf(train.getDepartTime()));
			trainDetail.setJustStarted(true);
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			try {
				Date date = sdf.parse(endTime);

				trainDetail.setTimeStarted(date.getTime() / 1000 - (train.getDepartTime().getTime() / 1000));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// add current train, stoppage time of dep time - start time
			StationDetail station = new StationDetail(trainDetail.getStartLocation(), 0.0, "0.0", 0, queryBuilder);
			trainDetail.setCurrentLat(station.getLat());
			trainDetail.setCurrentLong(station.getLongitude());
			List<StationDetail> stationList = new ArrayList<StationDetail>();
			stationList.add(station);
			trainDetail.setStationsCovered(stationList);

			trainDetail.setStationsPending(new ArrayList<StationDetail>()); // To be done
			trainDetailList.add(trainDetail);
		}

		if (trainResponse.getTrainDetail() == null) {
			trainResponse.setTrainDetail(new ArrayList<TrainDetail>());
		}

		trainResponse.getTrainDetail().addAll(trainDetailList);
	}

	private List<Train> fetchTrains(String startTime, String endTime, String startDay, TrainResponse trainResponse) {

		List<Train> trains = new ArrayList<Train>();
		try {
			Class.forName("com.mysql.jdbc.Driver");

			Connection con = null;
			con = DriverManager.getConnection("jdbc:mysql://localhost/railways", "root", "");
			con.setAutoCommit(false);
			PreparedStatement pstm = null;
			String sql = "SELECT * FROM TRAIN WHERE `DEPART TIME` between '" + startTime + "' AND '" + endTime + "'";
			// System.out.println(sql);
			pstm = (PreparedStatement) con.prepareStatement(sql);
			ResultSet rs = pstm.executeQuery();
			con.commit();
			while (rs.next()) {

				if (checkStart(rs, startDay)) {
					Train train = new Train();
					train.setArrivalTime(rs.getTime("Arrival Time"));
					train.setDepartTime(rs.getTime("Depart Time"));
					train.setName(rs.getString("Name"));
					train.setNumber(rs.getInt("Number"));
					train.setStationByFrom(new Station(rs.getString("From"), ""));
					train.setStationByTo(new Station(rs.getString("To"), ""));
					trains.add(train);

					// System.out.println("Starting train : " + train.getNumber());
				}
			}

			if (STATION_CHECK && trainResponse.getStationMap().isEmpty()) {
				sql = "SELECT * FROM STATION";
				pstm = (PreparedStatement) con.prepareStatement(sql);
				rs = pstm.executeQuery();
				con.commit();
				while (rs.next()) {
					trainResponse.getStationMap().put(rs.getString("Code"), rs.getInt("Platforms"));
				}
			}

			con.close();

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return trains;
	}

	private boolean checkStart(ResultSet rs, String startDay) {

		try {
			String runningDays = rs.getString("RunningDays");
			String day = mapDay(startDay);
			if (runningDays.isEmpty()) {
				return true;
			}
			if (runningDays.contains(day) || runningDays.equals("Daily")) {
				return true;
			} else {
				return false;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	private String mapDay(String startDay) {
		switch (Integer.parseInt(startDay)) {
		case 0:
			return "SUN";
		case 1:
			return "MON";
		case 2:
			return "TUE";
		case 3:
			return "WED";
		case 4:
			return "THU";
		case 5:
			return "FRI";
		case 6:
			return "SAT";
		default:
			return "SUN";
		}
	}

}