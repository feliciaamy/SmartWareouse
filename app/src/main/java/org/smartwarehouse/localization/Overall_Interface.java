package org.smartwarehouse.localization;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import org.smartwarehouse.R;

import java.io.IOException;
import java.util.UUID;


public class Overall_Interface extends ActionBarActivity {

    Button Motor, LinearElevator, Scanning, btnDis;
    private int yCoor = 1458;
    int M_state = 0;
    int LE_state = 0;
    SeekBar brightness;
    TextView lumn;
    String address = null;
    private ProgressDialog progress;
    static BluetoothAdapter myBluetooth = null;
    static BluetoothSocket btSocket = null;
    static boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the overall_interface
        setContentView(R.layout.overall_interface);

        //call the widgtes
        btnDis = (Button) findViewById(R.id.button4);
        Scanning = (Button) findViewById(R.id.button5);


        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        Motor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yCoor += 100;
                send_message("s,2592," + yCoor + "\n");

//                if (M_state == 0){
//                    send_message("m0,\n");
//                    Motor.setText("Stop Motor");
//                    Motor.setBackgroundColor(Color.RED);
//                    M_state = 1;
//                }
//                else{
//                    send_message("m1,\n");
//                    Motor.setText("Start Motor");
//                    Motor.setBackgroundColor(Color.GREEN);
//                    M_state = 0;
//                }
            }
        });

        LinearElevator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yCoor -= 100;
                send_message("s,2592," + yCoor + "\n");
//                if (LE_state == 0){
//                    send_message("l0,\n");
//                    LinearElevator.setText("Stop Linear Elevator");
//                    LinearElevator.setBackgroundColor(Color.RED);
//                    LE_state = 1;
//                }
//                else{
//                    send_message("l1,\n");
//                    LinearElevator.setText("Start Linear Elevator");
//                    LinearElevator.setBackgroundColor(Color.GREEN);
//                    LE_state = 0;
//                }
            }
        });

        Scanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Make an intent to start next activity.
                Intent i = new Intent(Overall_Interface.this, MainActivity.class);

                startActivity(i);
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
            }
        });
    }

    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish(); //return to the first layout

    }


    private void send_message(String text) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(text.toString().getBytes());
                Log.d("debug", "message send");
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(Overall_Interface.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}
