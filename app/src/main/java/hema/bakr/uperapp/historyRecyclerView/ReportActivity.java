package hema.bakr.uperapp.historyRecyclerView;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import hema.bakr.uperapp.R;

public class ReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        final TextView reportMessage = findViewById(R.id.report_message);
        final TextView doneReport = findViewById(R.id.done_report);
        final Button submit = findViewById(R.id.submit_report);
        Toolbar toolbar = findViewById(R.id.reportToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);


        final String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String report = reportMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(report)){

                    DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference().child("Reports");

                    String repKey = reportRef.push().getKey();

                    HashMap<String, Object> map = new HashMap<>();

                    map.put("customerId" , customerId);
                    map.put("reportMessage",report);

                    reportRef.child(repKey).updateChildren(map);

                    reportMessage.setVisibility(View.GONE);
                    submit.setVisibility(View.GONE);
                    doneReport.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
