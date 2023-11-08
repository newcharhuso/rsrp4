package com.example.rsrp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.format.DateFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PHONE_STATE_PERMISSION = 1;
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 2;

    private Button buttonGetRSRP;
    private TextView textViewRSRP;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRSRPRunnable;
    private TextView textViewLogs; // Added TextView for logs
    private String previousRSRPValue; // Variable to store the previous RSRP value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonGetRSRP = findViewById(R.id.buttonGetRSRP);
        textViewRSRP = findViewById(R.id.textViewRSRP);
        textViewLogs = findViewById(R.id.textViewLogs);

        buttonGetRSRP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_PHONE_STATE_PERMISSION);
                } else {
                    requestPhoneStatePermission();
                }
            }
        });
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private void requestPhoneStatePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSION);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                updateRSRP();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PHONE_STATE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    // ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    // public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    // int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                requestPhoneStatePermission();
            } else {
                textViewRSRP.setText("Permission denied. Cannot access RSRP.");
            }
        } else if (requestCode == REQUEST_FINE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    updateRSRP();
                }
            } else {
                textViewRSRP.setText("Permission denied. Cannot access RSRP.");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void updateRSRP() {
        updateRSRPRunnable = new Runnable() {
            @Override
            public void run() {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                SignalStrength signalStrength = telephonyManager.getSignalStrength();
                String rsrp = signalStrength.toString();
                int mLteIndex = rsrp.indexOf("mLte");
                if (mLteIndex != -1) {
                    String mLteValue = rsrp.substring(mLteIndex + 5, rsrp.indexOf(",", mLteIndex));

                    // Extract the RSRP value
                    String rsrpValue = mLteValue.substring(mLteValue.indexOf("rsrp=") + 5);

                    // Remove any part that starts with "rsrq="
                    if (rsrpValue.contains("rsrq=")) {
                        rsrpValue = rsrpValue.substring(0, rsrpValue.indexOf("rsrq="));
                    }

                    // Get the current timestamp
                    String currentTime = (String) DateFormat.format("hh:mm:ss a", System.currentTimeMillis());
                    textViewRSRP.setText("RSRP: " + rsrpValue.trim() + " dBm" + " [" + currentTime + "]");
                    // Check if the RSRP value has changed
                    if (!rsrpValue.equals(previousRSRPValue)) {
                        // Log the RSRP value with timestamp
                        log("RSRP = " + rsrpValue.trim() + " dBm");

                        // Update textViewRSRP with timestamp

                        previousRSRPValue = rsrpValue; // Update the previous RSRP value
                    }

                    // Schedule the update every 1 second
                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.post(updateRSRPRunnable);
    }


    // Function to log messages with timestamps, update the TextViewLogs, and write to a .txt file
    private void log(String message) {
        String currentTime = (String) DateFormat.format("hh:mm:ss a", System.currentTimeMillis()); // Get current time
        String logMessage = "[" + currentTime + "] " + message; // Add timestamp to the message

        // Append the message to the existing log if it's not already present
        String existingLog = textViewLogs.getText().toString();
        if (!existingLog.contains(logMessage)) {
            String newLog = existingLog + "\n" + logMessage;
            textViewLogs.setText(newLog);

            // Write the log message to the file
            writeToLogFile(logMessage);
        }
    }

    private void writeToLogFile(String logMessage) {
        try {
            // Define the file path
            File logFile = new File(getExternalFilesDir(null), "log.txt");

            // Create the log file if it doesn't exist
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            // Write the log message to the file
            FileWriter writer = new FileWriter(logFile, true); // 'true' to append to the file
            writer.write(logMessage + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
