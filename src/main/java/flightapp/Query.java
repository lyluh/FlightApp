package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  private static final String CLEAR_SQL = "DELETE FROM Users_lyledeng; DELETE FROM Reservations_lyledeng;";
  PreparedStatement clear;

  private static final String CREATE_USER_SQL = "INSERT INTO Users_lyledeng VALUES(?, ?, ?);";
  private PreparedStatement createCustomerStmt;

  private static final String CHECK_USERNAME_SQL = "SELECT password FROM Users_lyledeng WHERE username = ?";
  private PreparedStatement checkUsernameStmt;

  private static final String FIND_DIRECT_FLIGHT_SQL = "SELECT TOP (?) fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price "
  + "FROM Flights WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 ORDER BY actual_time ASC;";
  private PreparedStatement findDirectFlightsStmt;

  private static final String FIND_INDIRECT_FLIGHT_SQL = "SELECT TOP (?) F1.fid AS fid1, F2.fid AS fid2, F1.actual_time + F2.actual_time as actual_time "
  + "FROM Flights AS F1, Flights AS F2 WHERE F1.origin_city = ? AND F2.dest_city = ? AND F1.dest_city = F2.origin_city "
  + "AND F1.day_of_month = ? AND F2.day_of_month = ? AND F1.canceled = 0 AND F2.canceled = 0 ORDER BY actual_time, F1.fid, F2.fid ASC;";
  private PreparedStatement findIndirectFlightsStmt;

  private static final String GET_FLIGHT = "SELECT fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price "
  + "FROM Flights WHERE fid = ?;";
  private PreparedStatement getFlightStmt;
  //
  // Instance variables
  //
  private boolean inUse;
  private String currentUser;
  private List<Itinerary> itineraries;


  protected Query() throws SQLException, IOException {
    prepareStatements();
    this.inUse = false;
    this.currentUser = "";
    this.itineraries = new ArrayList<>();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      clear = conn.prepareStatement(CLEAR_SQL);
      clear.execute();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    createCustomerStmt = conn.prepareStatement(CREATE_USER_SQL);
    checkUsernameStmt = conn.prepareStatement(CHECK_USERNAME_SQL);
    findDirectFlightsStmt = conn.prepareStatement(FIND_DIRECT_FLIGHT_SQL);
    findIndirectFlightsStmt = conn.prepareStatement(FIND_INDIRECT_FLIGHT_SQL);
    getFlightStmt = conn.prepareStatement(GET_FLIGHT);
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    try {
      if (this.inUse) {
        return "User already logged in\n";
      }
      
      username = username.toLowerCase();
      checkUsernameStmt.setString(1, username);
      ResultSet result = checkUsernameStmt.executeQuery();
      result.next();
      byte[] result_password = result.getBytes("password");
      if (!PasswordUtils.plaintextMatchesSaltedHash(password, result_password)) {
        return "Login failed\n";
      } else {
        this.inUse = true;
        this.currentUser = username;
        return "Logged in as " + username + "\n";
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      return "Login failed\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    try {
      if (initAmount < 0) {
        return "Failed to create user\n";
      }

      username = username.toLowerCase();
      byte[] hashedPassword = PasswordUtils.saltAndHashPassword(password);
      createCustomerStmt.setString(1, username);
      createCustomerStmt.setBytes(2, hashedPassword);
      createCustomerStmt.setInt(3, initAmount);
      createCustomerStmt.execute();
      return "Created user " + username + "\n";
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      return "Failed to create user\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE             
    StringBuffer sb = new StringBuffer();
    try {
      if (dayOfMonth < 1 || dayOfMonth > 31) {
        return "Invalid date\n";
      }        
      if (numberOfItineraries < 0) {
        return "num itineraries must be positive\n";
      }

      itineraries.clear();
      findDirectFlightsStmt.setInt(1, numberOfItineraries);
      findDirectFlightsStmt.setString(2, originCity);
      findDirectFlightsStmt.setString(3, destinationCity);
      findDirectFlightsStmt.setInt(4, dayOfMonth);
      ResultSet oneHopResults = findDirectFlightsStmt.executeQuery();

      while (oneHopResults.next()) {
        int result_fid = oneHopResults.getInt("fid");
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_carrierId = oneHopResults.getString("carrier_id");
        String result_flightNum = oneHopResults.getString("flight_num");
        String result_originCity = oneHopResults.getString("origin_city");
        String result_destCity = oneHopResults.getString("dest_city");
        int result_time = oneHopResults.getInt("actual_time");
        int result_capacity = oneHopResults.getInt("capacity");
        int result_price = oneHopResults.getInt("price");
        // sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
        //       + result_flightNum + " Origin: " + result_originCity + " Destination: "
        //       + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
        //       + " Price: " + result_price + "\n");
        Flight f = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity, result_destCity, result_time, result_capacity, result_price);
        Itinerary i = new Itinerary(0, f);
        this.itineraries.add(i);
      }
      oneHopResults.close();

      if (!directFlight && itineraries.size() < numberOfItineraries) {
        numberOfItineraries = numberOfItineraries - itineraries.size();
        findIndirectFlightsStmt.setInt(1, numberOfItineraries);
        findIndirectFlightsStmt.setString(2, originCity);
        findIndirectFlightsStmt.setString(3, destinationCity);
        findIndirectFlightsStmt.setInt(4, dayOfMonth);
        findIndirectFlightsStmt.setInt(5, dayOfMonth);
  
        ResultSet indirectResults = findIndirectFlightsStmt.executeQuery();
  
        while (indirectResults.next()) {
          int result_fid1 = indirectResults.getInt("fid1");
          int result_fid2 = indirectResults.getInt("fid2");
          // Find detail of first flight
          getFlightStmt.setInt(1, result_fid1);
          ResultSet firstFlightResults = getFlightStmt.executeQuery();
  
          firstFlightResults.next();
          int result_fid = firstFlightResults.getInt("fid");
          int result_dayOfMonth = firstFlightResults.getInt("day_of_month");
          String result_carrierId = firstFlightResults.getString("carrier_id");
          String result_flightNum = firstFlightResults.getString("flight_num");
          String result_originCity = firstFlightResults.getString("origin_city");
          String result_destCity = firstFlightResults.getString("dest_city");
          int result_time = firstFlightResults.getInt("actual_time");
          int result_capacity = firstFlightResults.getInt("capacity");
          int result_price = firstFlightResults.getInt("price");
          Flight f1 = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity, result_destCity, result_time, result_capacity, result_price);
          firstFlightResults.close();
  
          // Find detail of second flight
          getFlightStmt.setInt(1, result_fid2);
          ResultSet secondFlightResults = getFlightStmt.executeQuery();
  
          secondFlightResults.next();
          int result2_fid = secondFlightResults.getInt("fid");
          int result2_dayOfMonth = secondFlightResults.getInt("day_of_month");
          String result2_carrierId = secondFlightResults.getString("carrier_id");
          String result2_flightNum = secondFlightResults.getString("flight_num");
          String result2_originCity = secondFlightResults.getString("origin_city");
          String result2_destCity = secondFlightResults.getString("dest_city");
          int result2_time = secondFlightResults.getInt("actual_time");
          int result2_capacity = secondFlightResults.getInt("capacity");
          int result2_price = secondFlightResults.getInt("price");
          Flight f2 = new Flight(result2_fid, result2_dayOfMonth, result2_carrierId, result2_flightNum, result2_originCity, result2_destCity, result2_time, result2_capacity, result2_price);
          secondFlightResults.close();

          Itinerary i = new Itinerary(0, f1, f2);
          this.itineraries.add(i);
        }
        indirectResults.close();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("error:" + e);
    }
    Collections.sort(this.itineraries);
    for (int i = 0; i < this.itineraries.size(); i++) {
      this.itineraries.get(i).id = i;
      sb.append(this.itineraries.get(i));
    }
    if (this.itineraries.size() == 0) {
      sb.append("No flights found");
    }
    return sb.toString();
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    return "Booking failed\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price + "\n";
    }
  }

  class Itinerary implements Comparable<Itinerary>{
    public int id;
    public Flight f1;
    public Flight f2;
    public int time;

    public Itinerary(int id, Flight f1, Flight f2) {
      this.id = id;
      this.f1 = f1;
      this.f2 = f2;
      this.time = f1.time + f2.time;
    }

    public Itinerary(int id, Flight f1) {
      this.id = id;
      this.f1 = f1;
      this.time = f1.time;
    }

    public String toString() {
      int numFlights = 1;
      if (this.f2 != null) {
        numFlights += 1;
      }
      String result = "Itinerary " + this.id + ": " + numFlights + " flight(s), " + this.time + " minutes\n"
           + this.f1;
      if (this.f2 != null) {
        result += this.f2;
      }
      return result;
    }

    public int compareTo(Itinerary o) {
      if (this.time == o.time) {
        if (this.f1.fid == o.f1.fid) {
          return this.f2.fid - o.f2.fid;
        }
        return this.f1.fid - o.f1.fid;
      }
      return this.time - o.time;
    }

  }
}


// try {
//   // one hop itineraries
//   String unsafeSearchSQL = "SELECT TOP (" + numberOfItineraries
//     + ") day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
//     + "FROM Flights " + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'"
//     + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
//     + "ORDER BY actual_time ASC";

//   Statement searchStatement = conn.createStatement();
//   ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);

//   while (oneHopResults.next()) {
//     int result_dayOfMonth = oneHopResults.getInt("day_of_month");
//     String result_carrierId = oneHopResults.getString("carrier_id");
//     String result_flightNum = oneHopResults.getString("flight_num");
//     String result_originCity = oneHopResults.getString("origin_city");
//     String result_destCity = oneHopResults.getString("dest_city");
//     int result_time = oneHopResults.getInt("actual_time");
//     int result_capacity = oneHopResults.getInt("capacity");
//     int result_price = oneHopResults.getInt("price");

//     sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
//               + result_flightNum + " Origin: " + result_originCity + " Destination: "
//               + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
//               + " Price: " + result_price + "\n");
//   }
//   oneHopResults.close();
// } catch (SQLException e) {
//   e.printStackTrace();
// }