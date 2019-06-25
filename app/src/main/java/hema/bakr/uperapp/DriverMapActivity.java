package hema.bakr.uperapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DriverMapActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {
    private DatabaseReference mHistoryRideInfoDb;
    //SwitchButton for working status
    private Switch mWorkingSwitch;

    //The class tag is used for Logging the errors
    private static final String TAG = "MapActivity";

    //Default zoom for the driver location
    private static final float DEFAULT_ZOOM = 16f;

    //This is the  top priority variable that holds the map
    private GoogleMap mMap;

    //This variable is needed to get the driver location
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //String variable holding the Customer Id
    private String mCustomerId = "";


    //Holds the last valid location for the driver
    Location mLastLocation;
    //This variable is for making updating the location of the driver by requesting as needed
    LocationRequest mLocationRequest;

    // this boolen used in Onstoped fuction
    private Boolean IsLogedOut = false;

    private LinearLayout mCustomerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;

    //the ride status button on the driver map activity
    private Button mRideStatus;

    private int status = 0;
    private String destination;
    private LatLng destiantionLatLng;
    private float rideDitance;

    Toolbar toolbar;
    public static final String MY_PREFS_NAME = "MyPrefsFile";
    private TextView workingStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        workingStatus = toolbar.findViewById(R.id.working_status);

        initializeNavigationDrawer();
        //this arrayList for draw the route
        mPolylines = new ArrayList<>();

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        //this variables for Display customer's info on driver's screen
        mCustomerInfo = findViewById(R.id.customerInfo);
        mCustomerName = findViewById(R.id.customerName);
        mCustomerPhone = findViewById(R.id.customerPhone);
        mCustomerProfileImage = findViewById(R.id.customerProfileImage);
        mCustomerDestination = findViewById(R.id.customerDestination);


        mWorkingSwitch = findViewById(R.id.workingSwitch);
        //this function used to check the switch button if it checked so make the driver available for customers
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //used to make the driver available for customers
                    workingStatus.setText(getResources().getText(R.string.stop_working));
                    workingStatus.setTextColor(getResources().getColor(R.color.colorAccent));
                    connectDriver();
                } else {
                    //used to make the driver Unavailable for customers
                    workingStatus.setText(getResources().getText(R.string.start_working));
                    workingStatus.setTextColor(getResources().getColor(R.color.white));
                    disConnectedDriver();
                }
            }
        });

        mRideStatus = findViewById(R.id.ride_status);
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    //Means the driver is on its way to pick up the cutomer
                    case 1:
                        status = 2;
                        erasePolylines();
                        if (destiantionLatLng.latitude != 0.0 && destiantionLatLng.longitude != 0.0) {
                            getRouteToMarker(destiantionLatLng);
                        }
                        mRideStatus.setText("Drive Completed");
                        break;
                    //the driver is with the customer and heading to the destiantion
                    case 2:
                        recordRide();
                        String x = String.valueOf(price);
                        endRide();
                        String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverWorking");

                        GeoFire geoFire = new GeoFire(ref);
                        //the listener must be added to prevent the app from crashing
                        geoFire.removeLocation(userid, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Log.i("Success", "Driver removed Successfully");
                            }
                        });
                        disConnectedDriver();
                        ///////Cash////////////////
                        Intent intent = new Intent(DriverMapActivity.this, PaymentActivity.class);
                        intent.putExtra("price", x);
                        intent.putExtra("rideid", requestID);
                        intent.putExtra("driverOrCustomer", "driver");
                        startActivity(intent);
                        //////////////////////
                        break;
                }
            }
        });

        //This function gets the driver the assigned customer to him
        getAssignedCustomer();
    }

    //Creating a map holdnig info needed to be recorded about the ride
    HashMap<String, Object> map = new HashMap<>();
    //This function keeps records of all the rides
    String requestID;

    private void recordRide() {

        //Getting the driver ID
        String userId = FirebaseAuth.getInstance().getUid();
        //Adding history child to the driver
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(userId).child("history");
        //Adding history child to the customer
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(mCustomerId).child("history");
        //creating "History" child in the main tree
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");
        //Creating a unique id for the ride
        requestID = historyRef.push().getKey();
        //Adding the id to the driver history child
        driverRef.child(requestID).setValue(true);
        //Adding the id to the customer history child
        customerRef.child(requestID).setValue(true);

        map.put("driver", userId);
        map.put("customer", mCustomerId);
        map.put("rating", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", customerLatLong.latitude);
        map.put("location/from/lng", customerLatLong.longitude);
        map.put("location/to/lat", destiantionLatLng.latitude);
        map.put("location/to/lng", destiantionLatLng.longitude);

        //updating the "History" child with the data
        historyRef.child(requestID).updateChildren(map);

    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis() / 1000;
        return timestamp;
    }

    //This function check if there is assigned customer to the driver and gets it
    private void getAssignedCustomer() {
        // Get the current driver ID
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //Get the custom ride id assigned to the driver
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverId).child("CustomerRequest").child("CustomerRideId");
        //A listener that Keep checking if a user got assigned to the customer
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Getting the assigned customer ID
                if (dataSnapshot.exists()) {
                    status = 1;
                    //Get the customer ride Id assigned to the driver
                    mCustomerId = dataSnapshot.getValue().toString();

                    //This method get the customer Location which is the pick up location for the driver
                    getAssignedCustomerLocation();
                    //This function gets the destination of the customer
                    getAssignedCustomerDestination();
                    //for get Customer Information
                    getAssignedCustomerInfo();

                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Not needed for now
            }
        });
    }

    //For Removing marker of pickup
    Marker mPickupMarker;
    //For Removing data of firebase and make the driver available again
    private DatabaseReference mAssignedCustomerPickupLocation;

    private ValueEventListener mAssignedCustomerPickupLocationListener;
    //The LatLng of the customer
    LatLng customerLatLong;

    //This method get the assigned customer Location for the driver
    private void getAssignedCustomerLocation() {
        //Get the customer reference so to be able to get the assigned customer pickup location
        mAssignedCustomerPickupLocation = FirebaseDatabase.getInstance().getReference().child("CustomerRequest").child(mCustomerId).child("l");
        //This listener keeps tracking the customer location
        mAssignedCustomerPickupLocationListener = mAssignedCustomerPickupLocation.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !mCustomerId.equals("")) {
                    //getting the data of the snapshot into list of Objects
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;
                    //Check if the list has the Latitude and Longitude and assign them to the above variables
                    if (map != null) {
                        //check if Latitude exists and assign it
                        if (map.get(0) != null) {
                            LocationLat = Double.parseDouble(map.get(0).toString());
                        }
                        //check if Longitude exists and assign it
                        if (map.get(1) != null) {
                            LocationLng = Double.parseDouble(map.get(1).toString());
                        }
                    }

                    //The LatLng of the customer
                    customerLatLong = new LatLng(LocationLat, LocationLng);

                    //Add the customer marker on the map with a title "Your Customer" that will be changed later
                    mMap.addMarker(new MarkerOptions().position(customerLatLong).title("Pickup Location"));
                    //get the route between customer locaton and driver locaton
                    getRouteToMarker(customerLatLong);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Not needed for now
            }
        });
    }

    //For get Route between driver and customer
    private void getRouteToMarker(LatLng customerLatLong) {
        if (customerLatLong != null && mLastLocation != null) {
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), customerLatLong)
                    .key(getString(R.string.google_api_key))
                    .build();
            routing.execute();
        }
    }


    //This function check if there is assigned customer to the driver and gets it
    private void getAssignedCustomerDestination() {
        // Get the current driver ID
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //Get the custom ride id assigned to the driver
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverId).child("CustomerRequest");
        //A listener that Keep checking if a user got assigned to the customer
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Getting the assigned customer ID
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("destination") != null) {
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination:" + destination);
                    } else {
                        mCustomerDestination.setText("Destination : -- ");
                    }

                    double destiantionLat = 0.0, destiantionLng = 0.0;

                    if (map.get("destinationLat") != null) {
                        destiantionLat = Double.parseDouble(map.get("destinationLat").toString());
                    }
                    if (map.get("destinationLng") != null) {
                        destiantionLng = Double.valueOf(map.get("destinationLng").toString());
                    }

                    destiantionLatLng = new LatLng(destiantionLat, destiantionLng);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Not needed for now
            }
        });
    }


    // for get customer's info from database and Display it on driver's screen
    private void getAssignedCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(mCustomerId);

        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map != null) {
                        if (map.get("name") != null) {
                            mCustomerName.setText(map.get("name").toString());
                        }
                        if (map.get("phone") != null) {
                            mCustomerPhone.setText(map.get("phone").toString());
                        }
                        if (map.get("profileImageUrl") != null) {
                            Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready");
        //Constructing the map
        mMap = googleMap;

        //Specifying the location request options
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // 1 second interval
        mLocationRequest.setFastestInterval(1000); //the least time to take if the request is ready
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Getting accurate location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permision message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    //Getting permissions .. second part
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    //This callback is similar to onChanged function .. here the request updates happen
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());

                    // calculate distance part 35

                    if (!mCustomerId.equals("")) {
                        // divied to 1000 for km
                        rideDitance += mLastLocation.distanceTo(location) / 1000;
                    }

                    mLastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());


                    //Get the user id to check later if it is assigned to the driver
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriverWorking");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    switch (mCustomerId) {
                        //This case when no user is assigned to the customer
                        case "":
                            geoFireWorking.removeLocation(userId, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    //This listener prevent the app from crashing
                                }
                            });
                            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    //This listener prevent the app from crashing
                                }
                            });
                            Log.i(TAG, "Everything is okay");
                            break;
                        default:
                            //This case when a user is assigned to the customer
                            geoFireAvailable.removeLocation(userId, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    //This listener prevent the app from crashing
                                }
                            });
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    //This listener prevent the app from crashing
                                }
                            });
                            break;
                    }
                }
            }
        }

    };

    private void connectDriver() {
        //take the permission to use GPS
        checkLocationPermission();

        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Log.e("Thread Exception", "Check the thread");
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), DEFAULT_ZOOM));
                    }
                });
            }
        };
        thread.start();
    }

    private void disConnectedDriver() {

        if (mFusedLocationProviderClient != null) {
            //Stopping the callback from getting the location
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }

        String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");

        GeoFire geoFire = new GeoFire(ref);
        //the listener must be added to prevent the app from crashing
        geoFire.removeLocation(userid, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Log.i("Success", "Driver removed Successfully");
            }
        });

    }

    int price;
    //all this part of Overrided methods for draw the route between customer and driver
    private List<Polyline> mPolylines;
    private static final int[] COLORS = new int[]{R.color.polylines, android.R.color.holo_blue_dark, android.R.color.holo_orange_dark};

    @Override
    public void onRoutingFailure(RouteException e) {
        // The Routing request failed
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        //Draw the route between if routing success between the customer and the driver
        if (mPolylines.size() > 0) {
            for (Polyline poly : mPolylines) {
                poly.remove();
            }
        }
        mPolylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            mPolylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + (double) route.get(i).getDistanceValue() / 1000 + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
            //save distance to databse
            map.put("distance", (double) route.get(i).getDistanceValue() / 1000);
            price = (int) (((double) route.get(i).getDistanceValue() / 1000) * .5 * 15);
        }
    }

    @Override
    public void onRoutingCancelled() {
    }

    //this method for clear the route from map
    private void erasePolylines() {
        for (Polyline line : mPolylines) {
            line.remove();
        }
        mPolylines.clear();
    }

    //This function ends the trip and remove the data from the database
    private void endRide() {

        mRideStatus.setText(getResources().getText(R.string.picked));

        erasePolylines();

        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(userId).child("CustomerRequest");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(mCustomerId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                //This listener prevent the app from crashing
            }
        });
        mCustomerId = "";
        // make distance zero for new trip
        rideDitance = 0;

        //Remove Pickup marker
        if (mPickupMarker != null) {
            mPickupMarker.remove();
        }

        if (mAssignedCustomerPickupLocationListener != null) {
            mAssignedCustomerPickupLocation.removeEventListener(mAssignedCustomerPickupLocationListener);
        }

        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText(getResources().getText(R.string.destination));
        mCustomerProfileImage.setImageResource(R.drawable.delivery);

    }

    private void initializeNavigationDrawer() {

        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String retrieve = prefs.getString("name", null);
        String name = "", phone = "", image = "";
        if (retrieve != null) {
            name = prefs.getString("name", "No name defined");//"No name defined" is the default value.
            phone = prefs.getString("phone", "No phone"); //0 is the default value.
            image = prefs.getString("image", "No image");
        }

        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_item_home);
        SecondaryDrawerItem item2 = new SecondaryDrawerItem().withIdentifier(2).withName(R.string.drawer_item_settings);
        SecondaryDrawerItem item3 = new SecondaryDrawerItem().withIdentifier(3).withName(R.string.drawer_item_history);
        SecondaryDrawerItem item4 = new SecondaryDrawerItem().withIdentifier(4).withName(R.string.drawer_item_Logout);
        SecondaryDrawerItem item5 = new SecondaryDrawerItem().withIdentifier(5).withName(R.string.drawer_item_report);

        // Create the AccountHeader
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.gradient_background)
                .addProfiles(
                        new ProfileDrawerItem().withName(name).withEmail(phone).withIcon(Uri.parse(image))
                )
                .build();

        //Now create your drawer and pass the AccountHeader.Result
        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        item1,
                        new DividerDrawerItem(),
                        item2,
                        new DividerDrawerItem(),
                        item3,
                        new DividerDrawerItem(),
                        item4,
                        new DividerDrawerItem(),
                        item5,
                        new DividerDrawerItem()
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {

                        if (drawerItem != null) {
                            Intent intent;
                            int identifier = (int) drawerItem.getIdentifier();
                            switch (identifier) {
                                case 2:
                                    disConnectedDriver();
                                    intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                                    startActivity(intent);
                                    break;
                                case 3:
                                    intent = new Intent(DriverMapActivity.this, HistoryActivity.class);
                                    intent.putExtra("CustomerOrDriver", "Riders");
                                    startActivity(intent);
                                    break;
                                case 4:
                                    IsLogedOut = true;
                                    //Disconnect the driver
                                    disConnectedDriver();
                                    FirebaseAuth.getInstance().signOut();
                                    intent = new Intent(DriverMapActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                    break;
                                case 5:
                                    Toast.makeText(DriverMapActivity.this, "Report activity under construction", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                        return false;
                    }
                }).withAccountHeader(headerResult).build();

        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);


    }

    @Override
    public void onBackPressed() {

        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        String userid = "";
        try {
            userid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser(), "User signed out already").getUid();
        } catch (Exception e) {
            Log.e("User logged ", e.getMessage());
        }

        if (!userid.equals("")) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");

            GeoFire geoFire = new GeoFire(ref);
            //the listener must be added to prevent the app from crashing
            geoFire.removeLocation(userid, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    Log.i("Success", "Driver removed Successfully");
                }
            });
        }
    }
}