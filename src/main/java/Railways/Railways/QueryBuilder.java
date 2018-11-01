package Railways.Railways;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryBuilder {
	
	Connection con = null;
	PreparedStatement pstm = null;
	

	QueryBuilder(){
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost/railways", "root", "");
			con.setAutoCommit(false);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	public ResultSet selectStopsQuery(String trainNumber ) {
		try {
			// fetch all stops
			String sql = "SELECT * FROM STOP WHERE `TRAIN NUMBER` =" + trainNumber
					+ " ORDER BY `SERIAL NUMBER` ASC";
			pstm = (PreparedStatement) con.prepareStatement(sql);
			ResultSet rs = pstm.executeQuery();
			con.commit();
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return null;
		
	}
	
	public ResultSet selectRouteQuery(String from, String to ) {
		try {
			// fetch all stops
			String sql = "SELECT DISTANCE FROM ROUTE WHERE `FROM` = '" + from
					+ "' AND `TO` = '" + to + "' ORDER BY DISTANCE DESC LIMIT 1";
			pstm = (PreparedStatement) con.prepareStatement(sql);
			ResultSet rs = pstm.executeQuery();
			con.commit();
			return rs;
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return null;
		
	}
	
	public ResultSet selectCustomQuery(String sql) {
		try {
			pstm = (PreparedStatement) con.prepareStatement(sql);
			ResultSet rs = pstm.executeQuery();
			con.commit();
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return null;
		
	}
	
	public void updateCustomQuery(String sql) {
		try {
			pstm = (PreparedStatement) con.prepareStatement(sql);
			pstm.executeUpdate();
			con.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}

	public void close() {
		try {
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
