package hema.bakr.uperapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PaymentActivity extends AppCompatActivity {
private TextView mPricee ;
private Button mCash,mPayPal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        mPricee = findViewById(R.id.price);
        mCash = findViewById(R.id.cash);
        mPayPal = findViewById(R.id.paypal);

        final String mRideId = getIntent().getExtras().getString("rideid");
        String mPrice =  getIntent().getExtras().getString("price");
        final String driverOrCustomer = getIntent().getExtras().getString("driverOrCustomer");
        if(driverOrCustomer.equals("driver"))
        {
            mPayPal.setVisibility(View.VISIBLE);
            mPricee.setText("Price : "+mPrice+" EGP");
            mCash.setText("Cash");
        }else{

            mPricee.setText(mPrice);
            mCash.setText("Ok");
        }
        mPayPal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PaymentActivity.this,"Thank you..",Toast.LENGTH_LONG).show();
                Intent intent = new Intent(PaymentActivity.this, DriverMapActivity.class);
                startActivity(intent);
            }
        });
        mCash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(driverOrCustomer.equals("driver"))
                {
                    DatabaseReference mHistoryRideInfoDb = FirebaseDatabase.getInstance().getReference().child("History").child(mRideId);
                    mHistoryRideInfoDb.child("customerPaid").setValue(true);
                    Toast.makeText(PaymentActivity.this,"Thank you..",Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(PaymentActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(PaymentActivity.this,"Thank you..",Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(PaymentActivity.this, CustomerMapActivity.class);
                    startActivity(intent);
                }

            }
        });
    }
}
