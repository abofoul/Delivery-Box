package hema.bakr.uperapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback , RoutingListener {

    private String mRideId,mCurrentUserId,mCustomerId,mDriverId,mUserDriverOrCustomer;
    private TextView mRideLocation,mRideDistance,mRideDate,mUserName,mUserPhone,mPrice,mRatingText;
    private ImageView mUserImage;

    private RatingBar mRatingBar;

    private Button mPay;

    private DatabaseReference mHistoryRideInfoDb;

    private LatLng mdestinationLatLng, mpickupLatlng;

    private String distance;
    private String mProfileImageUrl;
    private double mRideprice;

    private Boolean customerPaid = false;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        //Initialize the PayPal Service
        Intent intent = new Intent(this,PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,config);
        startService(intent);

        //used for draw the route of ride
        mpolylines = new ArrayList<>();

        //save the selected ride Id to use It after
        mRideId = getIntent().getExtras().getString("rideId");

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        mRideLocation= findViewById(R.id.rideLocation);
        mRideDistance = findViewById(R.id.rideDistance);
        mRatingText = findViewById(R.id.ratingText);
        mPrice = findViewById(R.id.price);
        mRideDate = findViewById(R.id.rideDate);
        mUserName = findViewById(R.id.userName);
        mUserPhone = findViewById(R.id.userPhone);
        mUserImage = findViewById(R.id.userImage);

        mRatingBar = findViewById(R.id.rating_bar);

        mPay = findViewById(R.id.pay);

        //used to get current user ID from Firebase;
        mCurrentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //reference for selected history ride ID
        mHistoryRideInfoDb = FirebaseDatabase.getInstance().getReference().child("History").child(mRideId);

        getRideInformation();


    }

    private void getRideInformation() {

        mHistoryRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    //used to get all information about selected ride (all children of selected ride ID)
                    for(DataSnapshot child:dataSnapshot.getChildren()){
                        if(child.getKey().equals("customer")){
                            mCustomerId = child.getValue().toString();
                            //check if this Id is customer or driver Because this activity used for both driver and the customer
                            if(!mCustomerId.equals((mCurrentUserId))){
                                mUserDriverOrCustomer = "Riders";
                                getUserInformation("Customers",mCustomerId);
                            }
                        }
                        if(child.getKey().equals("driver")){
                            mDriverId = child.getValue().toString();
                            //check if this Id is customer or driver Because this activity used for both driver and the customer
                            if(!mDriverId.equals((mCurrentUserId))){
                                mUserDriverOrCustomer = "Customers";
                                getUserInformation("Riders",mDriverId);
                                displayCustomerRelatedOjects();
                            }
                        }
                        if(child.getKey().equals("timestamp")){
                            //Display Date and time
                           mRideDate.setText("Time : "+getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if(child.getKey().equals("rating")){
                            //Display the rating
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                        if(child.getKey().equals("customerPaid")){
                            customerPaid = true;
                        }

                        if(child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                            mRideDistance.setText("Distance : "+distance+ " Km");
                            mRideprice = Double.valueOf(distance)* .5 ;
                            mPrice.setText("Price : "+ Integer.valueOf((int) (mRideprice*15))+ " EGP");
                        }

                        if(child.getKey().equals("destination")){
                            //Display the location of ride
                            mRideLocation.setText("Destination : "+child.getValue().toString());
                        }
                        if(child.getKey().equals("location")){
                            mpickupLatlng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            mdestinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if(!mdestinationLatLng.equals(new LatLng(0, 0))){
                                getRouteToMarker();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayCustomerRelatedOjects() {
        //Display the rating bar
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingText.setVisibility(View.VISIBLE);
        //make pay button visible to customer
        mPay.setVisibility(View.VISIBLE);

        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                //set the rating on the main History child
                mHistoryRideInfoDb.child("rating").setValue(rating);

                //getting reference of the driver rating child
                DatabaseReference driverRating = FirebaseDatabase.getInstance().getReference().child("Users/Riders").child(mDriverId).child("rating");
                //setting the rating of the ride at the driver child
                driverRating.child(mRideId).setValue(rating);
            }
        });

        //used to make the button of PayPal =bled or not
        if(customerPaid){
           mPay.setEnabled(false);
        }else{
            mPay.setEnabled(true);
        }
        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payPalPayment();
            }
        });
    }

    private int PAYPAL_REQUEST_CODE = 1;
    //used when testing app if it ready to publish or not so we use (SandBox)
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);
    //Making a call for a PayPal Intent
    private void payPalPayment() {
        PayPalPayment payment = new PayPalPayment(new BigDecimal(mRideprice),"USD","Delivery Ride",
                PayPalPayment.PAYMENT_INTENT_SALE);
        //PaymentActivity is created by PayPAl API
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }

    //Getting Retrieved PayPal Status and check about if the payment is successful or not
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PAYPAL_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                //getting payment data
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if(confirm != null){
                    try {
                        JSONObject jsonObj = new JSONObject(confirm.toJSONObject().toString());

                        String paymentResponse = jsonObj.getJSONObject("response").getString("state");

                        if(paymentResponse.equals("approved")){
                            Toast.makeText(getBaseContext(),"Payment successful",Toast.LENGTH_SHORT).show();
                           //Recording Transaction in Database
                            mHistoryRideInfoDb.child("customerPaid").setValue(true);
                            mPay.setEnabled(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                Toast.makeText(getBaseContext(),"Payment unsuccessful",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        //used to stop PayPalService on destroy the app
        stopService(new Intent(this,PayPalService.class));
        super.onDestroy();
    }

    private void getUserInformation(String otherUserDriverOrCustomer, String otherUserID) {
        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserID);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Displaying Information of existing user
                if(dataSnapshot.exists()){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name") !=null){
                        mUserName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") !=null){
                        mUserPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl") !=null){

                        mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(mUserImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long timestamp) {

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);

        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();

        return date;
    }


    //For get Route between start and end location of the ride
    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(mpickupLatlng,mdestinationLatLng)
                .key(getString(R.string.google_api_key))
                .build();
        routing.execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }


    //all this part of Overrided methods for draw the route between start and end location of the ride
    private List<Polyline> mpolylines;
    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark};

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


        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        //Pickup Latlng
        builder.include(mpickupLatlng);
        //Destination Latlng
        builder.include(mdestinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);
        //animating the camera and zooming in selected history ride
        CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngBounds(bounds,padding);
        mMap.animateCamera(cameraupdate);
        //Add marker for Pickup Location
        mMap.addMarker(new MarkerOptions().position(mpickupLatlng).title("Pickup location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        //Add marker for Destination Location
        mMap.addMarker(new MarkerOptions().position(mdestinationLatLng).title("Destination location"));


        //Draw the route between if routing success between the customer and the driver
        if (mpolylines.size() > 0) {
            for (Polyline poly : mpolylines) {
                poly.remove();
            }
        }
        mpolylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            mpolylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + ((double)route.get(i).getDistanceValue()/1000) + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {
    }

    //this method for clear the route from map
    private void erasePolylines() {
        for (Polyline line : mpolylines) {
            line.remove();
        }
        mpolylines.clear();
    }
}
