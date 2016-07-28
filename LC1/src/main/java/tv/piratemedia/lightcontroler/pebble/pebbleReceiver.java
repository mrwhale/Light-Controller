package tv.piratemedia.lightcontroler.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import static com.getpebble.android.kit.Constants.INTENT_APP_ACK;
import static com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE;
import static tv.piratemedia.lightcontroler.Constants.WatchUUID;
import org.json.JSONException;
import static com.getpebble.android.kit.Constants.MSG_DATA;
import static com.getpebble.android.kit.Constants.TRANSACTION_ID;
import static com.getpebble.android.kit.PebbleKit.PebbleAckReceiver;
import tv.piratemedia.lightcontroler.controlCommands;
import tv.piratemedia.lightcontroler.controller;
import tv.piratemedia.lightcontroler.controlPreferences;

import java.util.UUID;
/*
created by mrwhale 18/06/2016
This is the class that connects to and listens to a pebble watch. Receives commands and then actions them
Commands come in the form of a pebble dictionary, which is a tuple (key/value pairs) of data sent by the watch (because
the watch app is written in c) Once received, we extract the data by asking the tuple for the key value (in this case KEY_ZONE,
same as the variable in the watch app) and it gives us back the value, which is the zone number (0-9) Once we have the Zone,
 we send that to a control commands instance which handles switching the lights
 */
public class pebbleReceiver extends BroadcastReceiver {

    // todo add in a setting option to "enable" pebble. Then we can use this to check if they actually want to be calling this method
    // TODO modify wear wifi option to be "wearable" to include pebble too
    // todo add the ability to send data to the watch.Need to send Zone data to the watch so it can display it - In progress

    // Variables to hold the key values in the tuple sent from pebble. Matches pebble app values
    private static final int KEY_CMD = 3;
    private static final int KEY_ZONE = 2;
    //Create a new controller to handle the sending of commands to the bridge
    controller mCont = new controller();
    controlCommands contCmd;
    //private static SharedPreferences prefs;

    //Tag for logging
    //TODO is there a way to only have it send to log if its flagged as debug or something? if(DEBUG){ Log.D()} something like this
    static String TAG = "Pebble Receiver";

    @Override
    //On receive. Does the deed when we hear from the pebble
    public void onReceive(Context context, Intent intent) {
        /*if(!prefs.getBoolean("pref_pebble", false)) {
            Log.d(TAG,"pebble prefs is false");
        } */

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean test = prefs.getBoolean("pref_pebble", false);
        //Log.d(TAG,"Pebble prefs is " + test);
        /* Future special. Code to see if we are on the right wifi, if not then dont bother to send command?
        utils utils = new utils(context);
        final SharedPreferences prefs = context.getSharedPreferences(WearSettings.NETWORKS_PREFS, Context.MODE_PRIVATE);

        if(utils.getWifiName() != null && prefs.getBoolean(utils.getWifiName(), false)) {
            Log.d(TAG,"Not on home wifi");
        }*/
        //Code necessary to have this Receiver service accept input from the pebble even if main app is not running
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //Log.d(TAG, "on recieve");
        //Check to see if the intent is from the pebble
        if (intent.getAction().equals(INTENT_APP_RECEIVE)) {
            contCmd = new controlCommands(context, mCont.mHandler);
            //Log.d("pebble app R", "Got intent to receive");
            //Log.d(TAG,"WAT UUID -" + WatchUUID + "- this");

            final UUID receivedUuid = (UUID) intent.getSerializableExtra(Constants.APP_UUID);
            //Log.d(TAG, "APP UUID -" + receivedUuid + "- this");
            //Check to see if the receieverUUID matches the one we want, If not, then ignore
            if (!WatchUUID.equals(receivedUuid)) {
                Log.d(TAG , "Not my uuid, plz ignore");
                return;
            }
            final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
            final String jsonData = intent.getStringExtra(MSG_DATA);
            //final String testack = intent.getStringExtra(INTENT_APP_ACK);
            //Log.d(TAG, "pebble ack = " + testack);
            if (jsonData == null || jsonData.isEmpty()) {
                Log.d(TAG, "jsonData null");
                return;
            }
            try {
                //extract data from pebble message
                final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
                Long zoneValue = data.getUnsignedIntegerAsLong(KEY_ZONE);
                Long cmdValue = data.getUnsignedIntegerAsLong(KEY_CMD);
                // do what you need with the data
                if (zoneValue != null && cmdValue != null) {
                    //TODO throw error if either of these dont exist and tell pebble? shouldnt need this as we are only getting response from pebble app. so if its doing something wrong then ive made bad code in the pebble watch app
                    int zone = zoneValue.intValue();
                    int cmd = cmdValue.intValue();
                    Log.d(TAG, "going to turn zone " + zone + " cmd " + cmd);
                    //Switch statement to see cmd (on/off) 0 = off, 1= on, then send the command to the controler with zone
                    switch (cmd) {
                        case 0:
                            //Turning off
                            contCmd.LightsOff(zone);
                            contCmd.appState.setOnOff(zone, false);
                            break;
                        case 1:
                            //Turning on
                            contCmd.LightsOn(zone);
                            contCmd.appState.setOnOff(zone, true);
                            break;
                    }
                }
                //Todo add exception handle if cant send the message
                PebbleKit.sendAckToPebble(context, transactionId);
            } catch (JSONException e) {
                Log.d(TAG,"failed received -> dict " + e);
                return;
            }
            //Todo add here at the end of a receive, send state information back to the watch by calling pebbleSender. maybe we just do it locally on the pebble storage, save the message for later
           // pebbleSender pSender = new pebbleSender();
            //pSender.sendState(context, contCmd);
        }
    }
}
