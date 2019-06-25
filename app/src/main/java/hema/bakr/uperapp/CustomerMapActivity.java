package hema.bakr.uperapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hema.bakr.uperapp.historyRecyclerView.ReportActivity;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback {


    //The class tag is used for Logging the errors
    private static final String TAG = "MapActivity";

    //Checking for canceling the request
    private Boolean mRequestBol = false;

    //Default zoom for the driver location
    private static final float DEFAULT_ZOOM = 15f;

    //This is the  top priority variable that holds the map
    private GoogleMap mMap;

    //This variable is needed to get the driver location
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //Marker indicating the driver location
    Marker mCurrLocationMarker;

    //reference to the pickup request button
    private Button mRequest;

    //The pickup location
    private LatLng mPickupLocation;

    //Holds the last valid location for the customer
    Location mLastLocation;

    //This variable is for making updating the location of the driver by requesting as needed
    LocationRequest mLocationRequest;

    //The LatLng of the destination location
    private LatLng destinationLatLng;
    //For Removing marker when cancel the request
    private Marker mPickupMarker;

    private RatingBar ratingBar;

    private String mDestination;

    private LinearLayout mDriverInfo;
    private ImageView mDriverProfileImage, mProfileImage;
    private TextView mDriverName, mDriverPhone, mDriverCar;

    public static final String CUSTOMER_PREFES = "CusPrefes";
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        toolbar = findViewById(R.id.customerToolbar);
        setSupportActionBar(toolbar);

        initializeNavigationDrawer();

        getLocationPermission();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);


        //Setting default value for the destination location if user didn't set it
        destinationLatLng = new LatLng(0.0, 0.0);

        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverName = findViewById(R.id.driverName);
        mDriverPhone = findViewById(R.id.driverPhone);
        mDriverProfileImage = findViewById(R.id.driverProfileImage);
        mDriverCar = findViewById(R.id.driverCar);

        ratingBar = findViewById(R.id.ratingBar);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        //A reference to the pickup request  button
        mRequest = findViewById(R.id.request);

        // Function to make a request, call a driver and Mark my location and save it in Firebase
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Checking for canceling the request
                if (mRequestBol) {
                    endRide();
                } else {

                    //check if customer didn't choose a destination
                    if (TextUtils.isEmpty(mDestination)) {
                        Toast.makeText(CustomerMapActivity.this, "Please choose your destination", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mRequestBol = true;

                    String userIdd = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userIdd, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Log.e(TAG, "There was an error saving the location to GeoFire: " + error);
                            } else {
                                Log.i(TAG, "Location saved on server successfully!");
                            }
                        }
                    });
                    mPickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    mPickupMarker = mMap.addMarker(new MarkerOptions().position(mPickupLocation).title("Pickup Here"));
                    mRequest.setText("Getting your Drivers...");

                    // moSalah >  this functuin determine close driver for your request the implelmetation under
                    getCloserDriver();
                }
            }
        });

        Places.initialize(getApplicationContext(), getText(R.string.google_api_key).toString());
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                //getting the place name
                mDestination = place.getName();
                //getting the latitude and longitude of the location
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(CustomerMapActivity.this, "Please check you connection and choose your destination again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // moSalah > video num 9
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;

    //For cancelling listener of getCloserDriver function
    GeoQuery geoQuery;

    private void getCloserDriver() {

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(mPickupLocation.latitude, mPickupLocation.longitude), radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && mRequestBol) {
                    driverFound = true;
                    driverFoundID = key;

                    //Assign the customer ID to the driver that has the pick up request so the driver could know who is he picking
                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverFoundID).child("CustomerRequest");
                    // Get the current customer ID
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("CustomerRideId", customerId);
                    map.put("destination", mDestination);
                    map.put("destinationLat", destinationLatLng.latitude);
                    map.put("destinationLng", destinationLatLng.longitude);

                    //map.put(customerId, true);
                    driverRef.updateChildren(map);

                    //A function that gets the driver location .. it is def ined below
                    getDriverLocation();

                    //A function that gets the driver info
                    getDriverInfo();

                    //this function keep checking if the request was cancled by the driver and remove it from the database
                    getHasRideEnded();

                    //Changing the text of the button
                    mRequest.setText("Looking for Driver Location ... ");

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    // we increse th raduis 1 km to find near driver
                    radius += 50;
                    //call it self
                    getCloserDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }


    //The driver marker on the map
    private Marker mDriverMarker;

    //For removing of all Listeners of DatabaseRefrence
    private DatabaseReference mdriverLocationRef;
    private ValueEventListener mdriverLocationRefListener;
    //This method gets the driver Location and Mark it on the map
    private void getDriverLocation() {
        //L is a list for each driver that has the Latitude and Longitude
        mdriverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriverWorking").child(driverFoundID).child("l");
        // driverLocationRef.setValue(true);
        //final GeoFire geoFireDriver = new GeoFire(driverLocationRef);
        //A listener that keeps tracking the location of the driver
        mdriverLocationRefListener = mdriverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //checking if there is a snapshot exists
                if (dataSnapshot.exists() && mRequestBol) {
                    //getting the data of the snapshot into list of Objects
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;
                    mRequest.setText("Driver Found");
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
                    //The LatLng of the driver
                    LatLng driverLatLong = new LatLng(LocationLat, LocationLng);
                    //Check if there is already a marker to remove it and not to be repeated instead it should be changed to the new location
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }
                    //Location of customer
                    Location loc1 = new Location("");
                    loc1.setLatitude(mPickupLocation.latitude);
                    loc1.setLongitude(mPickupLocation.longitude);

                    //Location of Driver
                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLong.latitude);
                    loc2.setLongitude(driverLatLong.longitude);

                    //Give the distance between two locations in meters
                    float distance = loc1.distanceTo(loc2);

                    //Notice when driver Arrives
                    if (distance < 100) {
                        mRequest.setText("Driver is here..");
                    } else {
                        mRequest.setText("Driver found : " + (double)distance/1000+" Km");

                    }
                    //Add the driver marker on the map with a title "Your Driver" that will be changed later
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLong).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.car_icon_launcher)));


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Not needed for now
            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;

    private void getHasRideEnded() {

        //Get the custom ride id assigned to the driver
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverFoundID).child("CustomerRequest").child("CustomerRideId");
        //A listener that Keep checking if a user got assigned to the customer
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Getting the assigned customer ID
                if (!dataSnapshot.exists()) {
                    endRide();
                    /////////////////
                    Intent intent = new Intent(CustomerMapActivity.this, PaymentActivity.class);
                    intent.putExtra("price", "Order Finished");
                    intent.putExtra("rideid", "mm");
                    intent.putExtra("driverOrCustomer", "Customer");
                    startActivity(intent);
                    ////////////////
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Not needed for now
            }
        });
    }

    private void endRide() {

        mRequestBol = false;
        //When cancel the request,this will remove the listeners of getCloserDriver function
        geoQuery.removeAllListeners();
        if (mdriverLocationRefListener != null) {
            // Removing of all Listeners of DatabaseRefrence
            mdriverLocationRef.removeEventListener(mdriverLocationRefListener);
            //Remove request data from database
            //remove the driver listener
            driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
        }


        if (driverFoundID != null) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverFoundID).child("CustomerRequest");
            driverRef.removeValue();
            driverFoundID = null;
        }


        driverFound = false;
        radius = 1;
        String userIdd = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire geoFire = new GeoFire(ref);

        geoFire.removeLocation(userIdd, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                //Handling app crashes
            }
        });

        //Remove Pickup marker
        if (mPickupMarker != null) {
            mPickupMarker.remove();
        }
        //Change the text of button back to "Call Delivery Box.."
        mRequest.setText("Call Delivery Box");

        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_launcher);


    }


    //This function checks if permissions are granted and if not it calls the onRequestPermissionsResult function to grant it
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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

                    }
                } else {
                    Toast.makeText(CustomerMapActivity.this, "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverFoundID);

        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map != null) {
                        if (map.get("name") != null) {
                            mDriverName.setText(map.get("name").toString());
                        }
                        if (map.get("phone") != null) {
                            mDriverPhone.setText(map.get("phone").toString());
                        }
                        if (map.get("car") != null) {
                            mDriverCar.setText(map.get("car").toString());
                        }
                        // dataSnapshot.child("profileImageUrl").getValue()
                        // Get value was added in the video
                        if (map.get("profileImageUrl") != null) {
                            Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                        }

                        /*
                         The following section calculates the average rating of the driver and show it to the customer
                          */
                        int ratingSum = 0;
                        float totalRating = 0;
                        float averageRating = 0;
                        //We loop on all rating then calculate the average by dividing the total rating on total number of rides
                        //then we set the rating bar to the new number
                        for (DataSnapshot child : dataSnapshot.child("rating").getChildren()) {
                            ratingSum += Integer.valueOf(child.getValue().toString());
                            totalRating++;
                        }
                        if (totalRating != 0) {
                            averageRating = ratingSum / totalRating;
                            ratingBar.setRating(averageRating);
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

        getLocationPermission();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try { Thread.sleep(4000); }
                catch (InterruptedException e) {
                    Log.e("Thread Exception" , "Check the thread");
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()), DEFAULT_ZOOM));
                    }
                });
            }
        };
        thread.start();



    }

    //This callback is similar to onChanged function .. here the request updates happen
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                mCurrLocationMarker = mMap.addMarker(markerOptions);



            }
        }
    };



    private void initializeNavigationDrawer() {

        SharedPreferences prefs = getSharedPreferences(CUSTOMER_PREFES, MODE_PRIVATE);
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
        SecondaryDrawerItem item6 = new SecondaryDrawerItem().withIdentifier(6).withName(getResources().getString(R.string.help));

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
                        new DividerDrawerItem(),
                        item6,
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
                                    intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                                    startActivity(intent);
                                    break;
                                case 3:
                                    intent = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                                    intent.putExtra("CustomerOrDriver", "Customers");
                                    startActivity(intent);
                                    break;
                                case 4:

                                    FirebaseAuth.getInstance().signOut();
                                    if (mFusedLocationProviderClient != null) {
                                        //Stopping the callback from getting the location
                                        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                                    }
                                    intent = new Intent(CustomerMapActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                    break;
                                case 5:
                                    intent = new Intent(CustomerMapActivity.this , ReportActivity.class);
                                    startActivity(intent);
                                    break;
                                case 6:
                                    intent = new Intent(CustomerMapActivity.this, HelpActivity.class);
                                    startActivity(intent);
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
        if (mFusedLocationProviderClient != null) {
            //Stopping the callback from getting the location
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }

    }
}

