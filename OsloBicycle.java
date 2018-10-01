// made by: lisa tomren kjørli
// enkel java-app laget for jobbsøknad i oslo kommune 2018
// gjør kall mot APIet til oslo bysykler og viser sted, antall ledige sykler og ledige låser.


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import com.google.gson.*;


public class OsloBicycle {
    
    private String CLIENTTOKEN = "";
    private String BASEURL = "https://oslobysykkel.no/api/v1/";
    
    public static void main(String[] args) {
        
        new OsloBicycle().RunApp();
    }
    
    
    private void RunApp()
    {
        if( AllStationsAreClosed() )
        {
            System.out.println("Sorry, all stations are closed");
        }
        else{
            //get all stations
            AllStations allStations = GetAllStations();
            
            
            //print info about oslo bicycles
            if( allStations.stations == null )
                System.out.println("Sorry, can't show any bicycles in Oslo");
            else
                PrintAllStationsTable(allStations);
        }
    }
    
    
    private Boolean AllStationsAreClosed()
    {
        try {
            String StatusResponse = GetResponseString("status");
            Status status = new Gson().fromJson(StatusResponse, Status.class );//parse json to object
            
            if( status != null)
                return Boolean.parseBoolean( status.all_stations_closed ) ; //TODO: should be done in Status-object
        }
        catch(Exception ex)
        {
            System.out.println( "Something went wrong in AllStationsAreClosed-method");
        }
        
        return false; //maybe something is wrong with this call, but calling stations could still be working..
    }
    
    
    private AllStations GetAllStations()
    {
        try
        {
            // get all stations with location-name
            String StationListResponse = GetResponseString("stations");
            AllStations allStations = new Gson().fromJson(StationListResponse, AllStations.class );//parse json to object
            
            
            if(allStations == null)
                return new AllStations();
            
            // get all stations with available locks and bikes
            String StationAvailabilityResponse = GetResponseString("stations/availability");
            AllStations allStationsWithAvailability = new Gson().fromJson(StationAvailabilityResponse, AllStations.class );//parse json to object
            
            
            //set availability to all stations
            for( Station station : allStations.stations)
            {
                Station matchingStation = allStationsWithAvailability.stations
                .stream()
                .filter(ms -> ms.id.equals( station.id ))
                .findFirst()
                .orElse(new Station());
                
                station.availability = matchingStation.availability;
            }
            
            allStations.stations.sort(Comparator.comparing( e -> e.title)); //order by title
            return allStations;
            
        }
        catch( Exception ex)
        {
            System.out.println( "Something went wrong in GetAllStaions-method");
        }
        
        return new AllStations();
    }
    
    
    private String GetResponseString( String requestUrl )
    {
        
        try
        {
            URL url = new URL(BASEURL + requestUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Client-Identifier", CLIENTTOKEN);
            int responseCode = con.getResponseCode();
            
            
            if( responseCode == 200 )
            {
                //get responseString
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                return response.toString();
            }
            else if(responseCode == 401)
                System.out.println( "Unauthorized user, please change clienttoken");
            
        }
        catch( Exception ex)
        {
            System.out.println( "Something went wrong in GetResponseString-method");
        }
        
        return "";
        
    }
    
    
    private void PrintAllStationsTable( AllStations allStations)
    {
        //heading
        System.out.println("BYSYKLER I OSLO");
        
        //table header
        System.out.format("%-30s %2s %30s %n",
                          "STED:",
                          "LEDIGE SYKLER:",
                          "LEDIGE LÅSER:");
        
        //table rows
        for(Station station : allStations.stations) {
            
            System.out.format("%-30s %2s %32s %n",
                              station.title,
                              station.availability.bikes ,
                              station.availability.locks );
        }
    }
    
    
    //----------OBJECTS----------//
    
    class Station
    {
        String id;
        String title;
        Availability availability = new Availability();
    }
    
    class Availability{
        String bikes = "0";
        String locks = "0";
    }
    
    
    // dont want to have this class!
    //Station[] allStations = gson.fromJson(r2, Station[].class ); NOT WORKING!!
    class AllStations
    {
        List<Station> stations;
    }
    
    class Status
    {
        String all_stations_closed;
    }
    
}
