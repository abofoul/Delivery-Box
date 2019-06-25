package hema.bakr.uperapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hema.bakr.uperapp.historyRecyclerView.HistoryAdapter;
import hema.bakr.uperapp.historyRecyclerView.HistoryObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView mHistoryRecyclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private TextView mBalance;
    private TextView emptyView;
    private double balance = 0.0;

    private Button mPayout;

    private EditText mPayoutEmail;

    //the list that populates the recyclerview adapter
    private List<HistoryObject> resultHistory = new ArrayList<>();

    //holds the string extra to differentiate between customer and driver
    private String customerOrDriver;
    //holds the current user id
    private String userId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mBalance = findViewById(R.id.balance);
        mPayout = findViewById(R.id.payout);
        mPayoutEmail = findViewById(R.id.payoutEmail);
        emptyView = findViewById(R.id.empty_view);

        //getting the string extra
        customerOrDriver = getIntent().getStringExtra("CustomerOrDriver");
        //getting current user id
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //This function gets all the rides history from the customer's "history" tree child
        getUserHistory();

        if (customerOrDriver.equals("Riders")) {
            mBalance.setVisibility(View.VISIBLE);
            mPayout.setVisibility(View.VISIBLE);
            mPayoutEmail.setVisibility(View.VISIBLE);
        }

        mHistoryRecyclerView = findViewById(R.id.historyRecyclerView);
        //allowing scrolling the recyclerview
        mHistoryRecyclerView.setNestedScrollingEnabled(true);
        //setting the size to fixed as long as user doesn't get realtime updates of the history
        mHistoryRecyclerView.setHasFixedSize(true);

        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);

        //Initializing the adapter
        mHistoryAdapter = new HistoryAdapter(resultHistory, HistoryActivity.this);

        //populating the adapter with the list
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        if (resultHistory.size() != 0) {
            mHistoryRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        mPayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payoutRequest();
            }
        });


    }

    //This function gets all the rides history from the customer's "history" tree child
    private void getUserHistory() {
        //getting reference for the history child that exists inside customer child
        DatabaseReference userHistory = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userId).child("history");
        userHistory.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //the loop gets each ride history unique id
                    for (DataSnapshot history : dataSnapshot.getChildren()) {
                        //We pass the history unique id to the function
                        if (history.getValue().toString().equals("true")) {
                            getRideInformation(history.getKey());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //This function gets the ride details from the History child
    private void getRideInformation(String key) {
        //getting reference for main history child
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("History").child(key);

        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //Getting the unique ride id
                    String rideId = dataSnapshot.getKey();
                    //timestamp
                    Long timestamp = 0L;
                    String destination = "";

                    if (dataSnapshot.child("timestamp").getValue() != null) {
                        timestamp = Long.valueOf(dataSnapshot.child("timestamp").getValue().toString());
                    }

                    if (dataSnapshot.child("destination").getValue() != null) {
                        destination = dataSnapshot.child("destination").getValue().toString();
                    }

                    Double ridePrice = 0.0;

                    if (dataSnapshot.child("customerPaid").getValue() != null && dataSnapshot.child("driverPaidOut").getValue() == null) {
                        if (dataSnapshot.child("distance").getValue() != null) {
                            ridePrice = Double.valueOf(dataSnapshot.child("price").getValue().toString());
                            balance += ridePrice;
                            mBalance.setText("Balance: " + balance + "EGP");
                        }
                    }


                    //Adding each id to the list that populates the adapter
                    HistoryObject obj = new HistoryObject(rideId, getDate(timestamp), destination);
                    resultHistory.add(obj);
                    //notifying the adapter that data changed
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long timestamp) {

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp * 1000);

        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();

        return date;
    }

    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    ProgressDialog progress;
    private void payoutRequest() {
        progress = new ProgressDialog(this);
        progress.setTitle("Processing your payout");
        progress.setMessage("please wait");
        progress.setCancelable(false);
        progress.show();

        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();

        try {
            postData.put("uid", FirebaseAuth.getInstance().getUid());
            postData.put("email", mPayoutEmail.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

        final Request request = new Request.Builder()
                .url("https://us-central1-uberapp-b.cloudfunctions.net/payout")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", "Your Token")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // To ensure everything is going fine
                // for debugging reasons bro
                progress.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int responseCode = response.code();
                if (response.isSuccessful()) {
                    switch (responseCode) {
                        case 200:
                            Snackbar.make(findViewById(R.id.layout), "Payout Successfull!", Snackbar.LENGTH_LONG).show();
                            break;
                        case 500:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could Not Complete Payout", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could Not Complete Payout", Snackbar.LENGTH_LONG).show();
                            break;
                    }
                    progress.dismiss();
                }
            }
        });

    }
}





