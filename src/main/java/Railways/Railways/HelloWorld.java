package Railways.Railways;

import org.o7planning.generateentities.Station;
import org.o7planning.generateentities.Train;


import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONException;
 
@Path("/trainStatus")
public class HelloWorld {
	
	/*
	 * This function should perform the following tasks
	 * 1) Fetch new trains from the DB - with their designated paths
	 * 2) Move the already fetched trains - update their status
	 */
	
	
	  @PUT
	  @Consumes(MediaType.APPLICATION_JSON)
	  public Response trainStatus(@QueryParam("startTime") String startTime, @QueryParam("endTime") String endTime, TrainResponse trainResponse) throws JSONException {
 
		System.out.println("Receiving request " + JsonMapper.mapObjectToJson(trainResponse));
		  
		//Start new trains
		startTrains(startTime, endTime, trainResponse);
		
		//populate path for started trains
		populatePath(trainResponse);
		
		//move all trains
		moveAll(startTime, endTime, trainResponse);
		
		return Response.status(200).entity(JsonMapper.mapObjectToJson(trainResponse)).build();
	  }

	private void moveAll(String startTime, String endTime, TrainResponse trainResponse) {
		
		double range = Double.valueOf(endTime) - Double.valueOf(startTime);
		for(TrainDetail trainDetail: trainResponse.getTrainDetail()) {
			if(trainDetail.isJustStarted()) {
				range = trainDetail.getTimeStarted();
				trainDetail.setJustStarted(false);
			}
			
			while(range>0) {
				
				if(trainDetail.getCurrentLocation().equals("Moving")) {
				StationDetail station = getNextStation(trainDetail.getStationsPending(), trainDetail.getDistanceCovered());
				double maxDistance = getMaxDistance(trainDetail.getStationsPending());
				double distancePending = station.getDistance() - Double.valueOf(trainDetail.getDistanceCovered());
				double tempDistance = (Double.valueOf(station.getSpeed())*range)/1000.0;
				
				if(tempDistance < distancePending) {
					trainDetail.setDistanceCovered(String.valueOf(Double.valueOf(trainDetail.getDistanceCovered())+tempDistance));
					trainDetail.setPercentCompleted(String.valueOf(Double.valueOf(trainDetail.getDistanceCovered())*100.0/maxDistance));
					range=0;
				} else {
					//go till station
					double time = distancePending*1000.0/Double.valueOf(station.getSpeed());
					trainDetail.setDistanceCovered(String.valueOf(Double.valueOf(trainDetail.getDistanceCovered())+distancePending));
					trainDetail.setPercentCompleted(String.valueOf(Double.valueOf(trainDetail.getDistanceCovered())*100.0/maxDistance));
					
					range = range - time;
					trainDetail.setCurrentLocation(station.getCode());
				}
	
			} else {//Train is at a station
				StationDetail stationDetail = getStationByCode(trainDetail.getCurrentLocation(), trainDetail);
				if(stationDetail.getStopTime()==0) {
					
					//move this to done
					moveToVisited(trainDetail, stationDetail.getCode());
					//carry on moving
					trainDetail.setCurrentLocation("Moving");
				}
				else {
					
					if(stationDetail.getraminingStopTime() > 0) {
						if(range < stationDetail.getraminingStopTime()) {
							stationDetail.setraminingStopTime(stationDetail.getraminingStopTime()-range);
							range=0;
						} else {
							range = range - stationDetail.getraminingStopTime();
							stationDetail.setraminingStopTime(0.0);
							//move this to done
							moveToVisited(trainDetail, stationDetail.getCode());
							
							//carry on moving
							trainDetail.setCurrentLocation("Moving");
						}
					}
					else {
						if(range < stationDetail.getStopTime()) {
							stationDetail.setraminingStopTime(stationDetail.getraminingStopTime()-range);
							range=0;
						} else {
							range = range - stationDetail.getStopTime();
							//move this to done
							moveToVisited(trainDetail, stationDetail.getCode());
							
							//carry on moving
							trainDetail.setCurrentLocation("Moving");
						}
					}
					
				}
			}
			
			}
		}
	}

	private void moveToVisited(TrainDetail trainDetail, String code) {
		
		StationDetail tempStation =null;
		for(StationDetail station:trainDetail.getStationsPending()){
			if(station.getCode().equals(code)) {
				trainDetail.getStationsCovered().add(station);
				tempStation = station;
			}
		}
		trainDetail.getStationsPending().remove(tempStation);
		
	}

	private StationDetail getStationByCode(String currentLocation, TrainDetail trainDetail) {
		for(StationDetail station:trainDetail.getStationsPending()) {
			if(station.getCode().equals(currentLocation)) {
				return station;
			}
		}
		return null;
	}

	private double getMaxDistance(List<StationDetail> stationsPending) {
		double max = 0.0;
		for (StationDetail station: stationsPending) {
			if(station.getDistance()>max) {
				max = station.getDistance();
			}
		}
		
		return max;
	}

	private StationDetail getNextStation(List<StationDetail> stationsPending, String distanceCovered) {
		
		double min = 100000.0;
		StationDetail stationMin = null;
		for (StationDetail station: stationsPending) {
			if((station.getDistance()-Double.valueOf(distanceCovered))<min) {
				min = station.getDistance()-Double.valueOf(distanceCovered);
				stationMin = station;
			}
		}
		
		return stationMin;
		
	}

	private void populatePath(TrainResponse trainResponse) {
		for(TrainDetail trainDetail: trainResponse.getTrainDetail()) {
			if(trainDetail.getDistanceCovered()=="0") {//Recently added trains
				
				try {
					Class forName = Class.forName("com.mysql.jdbc.Driver");
					
			        Connection con = null;
			        con = DriverManager.getConnection("jdbc:mysql://localhost/railways", "root", "");
			        con.setAutoCommit(false);
			        PreparedStatement pstm = null;
			        PreparedStatement pstm2 = null;
			        
			        //fetch all stops
			        String sql = "SELECT * FROM STOP WHERE `TRAIN NUMBER` ="+trainDetail.getNumber()+" ORDER BY `SERIAL NUMBER` ASC";
			        pstm = (PreparedStatement) con.prepareStatement(sql);
			        ResultSet rs = pstm.executeQuery();
			        con.commit();
			        
			        long prevDepart = Time.valueOf(trainDetail.getDepartureTime()).getTime();
			        String prevStation = trainDetail.getStartLocation();
			        String prevDistance = "0";
				
			        while (rs.next()) {
			        	//fetch distance for each stop
			        	if(!(rs.getString("Station")).equals(prevStation)){
			        		
			        	
			        	String newSql = "SELECT DISTANCE FROM ROUTE WHERE `FROM` = '"+prevStation+"' AND `TO` = '"+rs.getString("Station") + "' ORDER BY DISTANCE DESC LIMIT 1";
			        	pstm2 = (PreparedStatement) con.prepareStatement(newSql);
			        	ResultSet rs2 = pstm2.executeQuery();
			        	rs2.next();
				        con.commit();
				        long waitingTime = ((rs.getTime("Departure Time")).getTime() - (rs.getTime("Arrival Time")).getTime())/1000;;
				        String distance = String.valueOf(rs2.getInt("Distance"));
				        long tempTravelTime = ((rs.getTime("Arrival Time")).getTime() - prevDepart);
				        
				        //Change of day
				        if(tempTravelTime<0) {
				        	tempTravelTime = tempTravelTime + 86400000;
				        }
				        double speed = (Double.valueOf(distance)*1000000.0/tempTravelTime);
				        System.out.println("Speed is "+speed);
				        StationDetail station = new StationDetail(rs.getString("Station"),Double.valueOf(distance)+Double.valueOf(prevDistance) , String.valueOf(speed), waitingTime );
			        	
				
				        //fetch train number/serial number of train covering max stations and almost equal to the distance
				        newSql = "select max(k.`serial number`-s.`serial number`) as boo"
				        		+ " from stop s, stop k where s.station='"+ prevStation+ "' and k.station= '" + rs.getString("Station")+ "' "
				        				+ "and s.`train number`=k.`train number` order by boo desc limit 1";
				        pstm2 = (PreparedStatement) con.prepareStatement(newSql);
			        	ResultSet rs3 = pstm2.executeQuery();
				        con.commit();
				        rs3.next();
				        int maxCount = rs3.getInt("boo");
				        
				        //call a method to fetch sub-stops and distance (FUCK!!)
				        List<StationDetail> stationList= fetchAllStations(prevStation, rs.getString("Station"), maxCount, distance, station, speed, Integer.parseInt(prevDistance), con, waitingTime);
				        
				        
				
				        //populate sub-stops, distance and speed
				        trainDetail.getStationsPending().addAll(stationList);
					
				        prevDistance = String.valueOf(Integer.parseInt(prevDistance)+Integer.parseInt(distance));
				        prevStation = rs.getString("Station");
						prevDepart = rs.getTime("Departure Time").getTime();
			        	}
			        	      	
			}
			con.close();
		}
				catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			}
		}
	}

	private List<StationDetail> fetchAllStations(String prevStation, String toStation, int maxCount, String distance, StationDetail station, double speed, int prevDistance, Connection con2, long waitingTime) {
		
		try {
			PreparedStatement pstm = null;
	        int count = maxCount;
	        while (count!=1) {
	        	String sql = "select s.`train number`,s.`serial number` from stop s, stop k where s.station='"+prevStation+"' and k.station='"+ toStation + "' and s.`train number`= k.`train number` and (k.`serial number`- s.`serial number`) =" +count;
		        pstm = (PreparedStatement) con2.prepareStatement(sql);
		        ResultSet rs = pstm.executeQuery();
		        con2.commit();
		        
		        while(rs.next()) {
		        	if(getDistance(rs.getInt("Train Number"), rs.getInt("Serial Number"), count, con2) < (Integer.parseInt(distance)+3)) {
		        		return populate(rs.getInt("Train Number"), rs.getInt("Serial Number"), count, speed, prevDistance, con2, waitingTime);
		        	}
		        	
		        }
		        
		        count--;
	        }
	        
	        List<StationDetail> stn = new ArrayList<StationDetail>();
	        stn.add(station);
	        return stn;  
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

	private List<StationDetail> populate(int trainNumber, int serialNumber, int count, double speed, int prevDistance, Connection con2, long waitingTime) {
		try {
			PreparedStatement pstm = null;
	        List<StationDetail> stationDetail = new ArrayList<StationDetail>();
	        int cumDistance = 0;
	        for(int i=serialNumber; i<(serialNumber+count); i++) {
	        	
	        	String sql = "(select station from stop where `train number`= "+trainNumber+
	        			" and `serial number` = "+ (i+1) +")";
	        	pstm = (PreparedStatement) con2.prepareStatement(sql);
		        ResultSet rs = pstm.executeQuery();
		        con2.commit();
		        rs.next();
		        String stationCode = rs.getString("Station");
		        
	        	sql = "select distance from route where `from` = "
	        			+ "(select station from stop where `train number`= "+trainNumber+
	        			" and `serial number` = "+i+") and `to`= '" +stationCode +"'";
		        pstm = (PreparedStatement) con2.prepareStatement(sql);
		        rs = pstm.executeQuery();
		        con2.commit();
		        rs.next();
		        int PartDistance = rs.getInt("Distance");
		        cumDistance = cumDistance + PartDistance;
		        long stopTime = 0;
		        if((i+1) == (serialNumber+count)) {
		        	stopTime = waitingTime;
		        }
		        StationDetail station = new StationDetail(stationCode,Double.valueOf(cumDistance+prevDistance) , String.valueOf(speed), stopTime);
	        	stationDetail.add(station);
	        	
	        }
	        
	        return stationDetail;
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<StationDetail>();
		
		
	}

	private int getDistance(int trainNumber, int serialNumber, int count, Connection con2) {
		int distance =0;
		
		try {
			
	        PreparedStatement pstm = null;
	        
	        for(int i=serialNumber; i<=(serialNumber+count - 1);i++) {
	        	String sql = "select distance from route where `from` = "
	        			+ "(select station from stop where `train number`= "+trainNumber+
	        			" and `serial number` = "+i+") and `to`= "
	        			+ "(select station from stop where `train number`= "+trainNumber+
	        			" and `serial number` = "+ (i+1) +")"; 
		        pstm = (PreparedStatement) con2.prepareStatement(sql);
		        ResultSet rs = pstm.executeQuery();
		        con2.commit();
		        rs.next();
		        distance  = distance + rs.getInt("Distance");
		        
	        }
	        
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return distance;
	}

	private void startTrains(String startTime, String endTime, TrainResponse trainResponse) {
		List<Train> trainList = fetchTrains(startTime, endTime);
		List<TrainDetail> trainDetailList = new ArrayList<TrainDetail>();
		for(Train train:trainList) {
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
			trainDetail.setTimeStarted(Integer.parseInt(endTime)-(train.getDepartTime().getTime()/1000) -3600);
			//add current train, stoppage time of dep time - start time
			StationDetail station = new StationDetail(trainDetail.getCurrentLocation(), 0.0, "0.0", 0);
			
			List<StationDetail> stationList = new ArrayList<StationDetail>();
			stationList.add(station);
			trainDetail.setStationsCovered(stationList);
			
			trainDetail.setStationsPending(new ArrayList<StationDetail>()); //To be done
			trainDetailList.add(trainDetail);
		}
		trainResponse.setTrainDetail(trainDetailList);
	}

	private List<Train> fetchTrains(String startTime, String endTime) {
		
		List<Train> trains = new ArrayList<Train>();
		try {
		Class forName = Class.forName("com.mysql.jdbc.Driver");
		
        Connection con = null;
        con = DriverManager.getConnection("jdbc:mysql://localhost/railways", "root", "");
        con.setAutoCommit(false);
        PreparedStatement pstm = null;
        String sql = "SELECT * FROM TRAIN WHERE `DEPART TIME` between "+ startTime+" AND " + endTime;
        pstm = (PreparedStatement) con.prepareStatement(sql);
        ResultSet rs = pstm.executeQuery();
        con.commit();
        while (rs.next()) {
            Train train=new Train();
            train.setArrivalTime(rs.getTime("Arrival Time"));
            train.setDepartTime(rs.getTime("Depart Time"));
            train.setName(rs.getString("Name"));
            train.setNumber(rs.getInt("Number"));
            train.setStationByFrom(new Station(rs.getString("From"), ""));
            train.setStationByTo(new Station(rs.getString("To"), ""));
            trains.add(train);
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
}