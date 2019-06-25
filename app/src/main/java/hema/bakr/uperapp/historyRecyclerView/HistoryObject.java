package hema.bakr.uperapp.historyRecyclerView;


// This class represents a history object
public class HistoryObject {

    //holds the unique id for the ride
    private String rideId;

    // holds the time of the ride request
    private String time;

    //holds the destination
    private String destination;

    public HistoryObject(String rideId, String time, String destination) {
        this.rideId = rideId;
        this.time = time;
        this.destination = destination;
    }

    String getRideId() {
        return rideId;
    }

    String getTime() {
        return time;
    }


    public String getDestination() {
        return destination;
    }
}
