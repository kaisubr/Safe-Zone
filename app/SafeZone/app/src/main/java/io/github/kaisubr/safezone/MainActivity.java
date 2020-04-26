package io.github.kaisubr.safezone;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.graphics.Color;
import android.location.*;
import android.net.Uri;
import android.os.StrictMode;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.view.menu.MenuBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    String id, username, contact, county, existingResult, cityName, zipName, latlong;
    static boolean healthy, existingUser = false, isSearching = true;
    String dbms = "mysql";
    List<BluetoothDevice> allDiscoveredDevices = new ArrayList<>();
    String fips_id, numCases, numDeaths; boolean is_safe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getLocation();

        AccountManager am = AccountManager.get(this); // "this" references the current Context
        Account[] accounts = am.getAccounts();// .getAccountsByType("com.google");
        System.out.println(accounts[0].name + ", " + accounts[0].type);

        LayoutInflater factory = LayoutInflater.from(this);
        final View signinView = factory.inflate(R.layout.dialog_signin, null);
        final AlertDialog signin = new AlertDialog.Builder(this).create();

        ((TextView)signinView.findViewById(R.id.username)).setText(accounts[0].name);
        signin.setView(signinView);
        signin.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                username = ((TextView)signin.findViewById(R.id.username)).getText().toString();
                contact = ((TextView)signin.findViewById(R.id.phoneNumber)).getText().toString();
                healthy = !(((CheckBox)signin.findViewById(R.id.isInfected)).isChecked());
                try {
                    existingUser = verifyServer(username, contact); //generates ID if not found.
                } catch (IOException | JSONException throwables) { throwables.printStackTrace(); }
                Log.d("app", username + ", " + contact);

                signin.hide();

                county = getCounty(existingUser);

            }
        });

        signin.show();

    }

    private void continueApp() {
        // Delays are needed to ensure that location has enough time to be
        // registered, otherwise it is just null, and the data
        // is useless. To do: write listeners instead of delays.
        healthy = is_safe;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                toast("Updated network just now.");
                isSearching = false;
                ((Button)findViewById(R.id.buttonMapCases)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String lat = latlong.split(",")[0];
                        String lng = latlong.split(",")[1];
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/d/viewer?mid=1yCPR-ukAgE55sROnmBUFmtLN6riVLTu3&hl=en&fbclid=IwAR0VZ0net0x6_yL_cGL2xqSuxoElWY1u8YC3grOyjveMsRwdvDWaDCo71Ck&ll=" + lat + "%2C" + lng + "&z=10"));
                        startActivity(intent);
                    }
                });
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        toast("Verifying network.");
                        String x = readNetworkGraph();
                        Log.d("app_read", x);

                        ((Button)findViewById(R.id.buttonNetwork)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(getBaseContext(), ContactNetwork.class);
                                intent.putExtra("EXTRA_NETWORK_NODES", readNetworkGraph());
                                startActivity(intent);
                            }
                        });
                    }
                }, 4000);
            }
        }, 7500);
    }

    private String makeRequest(String sturl) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        URL url = new URL(sturl);
        connection = (HttpURLConnection) url.openConnection();
        connection.connect();


        InputStream stream = connection.getInputStream();

        reader = new BufferedReader(new InputStreamReader(stream));

        StringBuffer buffer = new StringBuffer();
        String line = "";

        while ((line = reader.readLine()) != null) {
            buffer.append(line+"\n");
            Log.d("app.response", "> " + line);
        }

        return buffer.toString();
    }

    private void toast(String text) {
        runOnUiThread(new Runnable(){
            public void run()  {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });

    }

    private boolean verifyServer(String username, String contact) throws IOException, JSONException {
        String strurl = "https://floating-garden-33325.herokuapp.com/users/"; //add authentication later.
        String result = makeRequest(strurl);
//        JSONObject jsonObject = new JSONObject(result);
//        Iterator<String> keys = jsonObject.keys();
//
//        while(keys.hasNext()) {
//            String key = keys.next();
//            if (jsonObject.get(key) instanceof JSONObject) {
//                // do something with jsonObject here
//            }
//        }

//        Log.d("app", jsonObject.get("Hello").toString());
        int i = result.indexOf(username);
        if (i > -1 && result.charAt(i+username.length()) == '\"') {
            Log.d("app", "existing user");
            toast("Welcome back, " + username);
            existingResult = makeRequest(strurl + "byEmail?d_name=" + username);

            try { JSONArray jsonArray = new JSONArray(result); } catch (Exception e) {}
            id = result.split("\"")[3];
            healthy = Boolean.parseBoolean(result.split("\"")[15]);
            zipName = result.split("\"")[19];
            if (zipName.contains("P")) zipName = "75075";

            String[] res = extractZoneInformation(getFips(zipName));
            fips_id = res[3];
            county = res[1];
            is_safe = (res[8].charAt(0) == 'T');
            numCases = res[4];
            numDeaths = res[5];

            Log.d("app", id + ", " + healthy + Arrays.toString(res));
            ((TextView)findViewById(R.id.textCaseCount)).setText(numCases);
            ((TextView)findViewById(R.id.textCaseCounty)).setText("Cases in ZONE FIPS " + fips_id + " (" + county + " County) " +
                    "and " + numDeaths + " deaths thus far.");
            Log.d("app", "IS safe " + is_safe + " since " + res[8]);
            ((TextView)findViewById(R.id.textCaseZonesNear)).setText("Safe zones nearby: Rockwall (48 cases); Lamar (8 cases); Kaufman (58 cases)");
            ((TextView)findViewById(R.id.textThisSafeShort)).setText( (is_safe)? "Green" : "Red" );
            ((TextView)findViewById(R.id.textThisSafeShort)).setTextColor( (is_safe)? Color.parseColor("#2E7D32") : Color.parseColor("#b71c1c"));
            ((TextView)findViewById(R.id.textThisSafe)).setText( (is_safe)? "You are in a safe zone. Contact or travel to other safe zones to meet with others." : "Your current zone is marked unsafe based on the number of cases in your area.\n\nIf you are uninfected, you can contact others to meet in safe zones below.");

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        discoverDevices();
                        continueApp();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 500);

            return true;
        }
        else {

            final String[] posturl = {"https://floating-garden-33325.herokuapp.com/users/add" + "?d_name=" + username + "&d_phone=" + contact + "&d_health=" + healthy};
            Log.d("app", "new user, ch after is " + result.charAt(i+1));

            if (zipName == null) {
                zipName = "75075";
            }
            String[] res = extractZoneInformation(getFips(zipName));

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Discovered your zone in " + cityName)
                    .setMessage("The closest zone (FIPS ID " + res[3] + " - " + res[1] + " County) is a " +
                            ((res[8].charAt(0) == 'F')? "red, unsafe" : "green, safe") +
                            " zone. \n\nThis is based on " +
                            res[4] + " case(s), " + " and " + res[5] + " death(s) in this area. \n\nWould you like " +
                            "to save this as your home/default zone?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                posturl[0] += "&d_zone=" + zipName;
                                makeRequest(posturl[0]);
                                fips_id = res[3];
                                county = res[1];
                                is_safe = (res[8].charAt(0) == 'T');
                                numCases = res[4];
                                numDeaths = res[5];

                                Log.d("app", id + ", " + healthy + Arrays.toString(res));
                                ((TextView)findViewById(R.id.textCaseCount)).setText(numCases);
                                ((TextView)findViewById(R.id.textCaseCounty)).setText("Cases in ZONE FIPS " + fips_id + " (" + county + " County) " +
                                        "and " + numDeaths + " deaths thus far.");
                                Log.d("app", "IS safe " + is_safe + " since " + res[8]);
                                ((TextView)findViewById(R.id.textCaseZonesNear)).setText("Safe zones nearby: Rockwall (48 cases); Lamar (8 cases); Kaufman (58 cases)");
                                ((TextView)findViewById(R.id.textThisSafeShort)).setText( (is_safe)? "Safe" : "Unsafe" );
                                ((TextView)findViewById(R.id.textThisSafeShort)).setTextColor( (is_safe)? Color.parseColor("#2E7D32") : Color.parseColor("#b71c1c"));
                                ((TextView)findViewById(R.id.textThisSafe)).setText( (is_safe)? "You are in a safe zone. Contact or travel to other safe zones to meet with others." : "Your current zone is marked unsafe based on the number of cases in your area.\n\nIf you are uninfected, you can contact others to meet in safe zones below.");
                                ProgressDialog pd = new ProgressDialog(MainActivity.this);
                                pd.setMessage("Updating zone data...");
                                pd.setCancelable(false);
                                pd.show();
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        pd.cancel();
                                        try {
                                            discoverDevices();
                                            continueApp();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, 500);

                            } catch (IOException e) { e.printStackTrace(); }
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            zipName = "";
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Enter your ZIP, so we can discover your zone");

                            // Set up the input
                            final EditText input = new EditText(MainActivity.this);
                            input.setInputType(InputType.TYPE_CLASS_NUMBER);
                            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            builder.setView(input);

                            // Set up the buttons
                            builder.setPositiveButton("Validate", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    zipName = input.getText().toString();
                                    posturl[0] += "&d_zone=" + zipName;
                                    String[] res = new String[0];
                                    try {
                                        makeRequest(posturl[0]);
                                        res = extractZoneInformation(getFips(zipName));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    fips_id = res[3];
                                    county = res[1];
                                    is_safe = (res[8].charAt(0) == 'T');
                                    numCases = res[4];
                                    numDeaths = res[5];

                                    ((TextView)findViewById(R.id.textCaseCount)).setText(numCases);
                                    ((TextView)findViewById(R.id.textCaseCounty)).setText("Cases in ZONE FIPS " + fips_id + " (" + county + " County) " +
                                            "and " + numDeaths + " deaths thus far.");
                                    ((TextView)findViewById(R.id.textCaseZonesNear)).setText("Safe zones nearby: Rockwall (48 cases); Lamar (8 cases); Kaufman (58 cases)");
                                    Log.d("app", "IS safe " + is_safe + " since " + res[8]);

                                    ((TextView)findViewById(R.id.textThisSafeShort)).setText( (is_safe)? "Green" : "Red" );
                                    ((TextView)findViewById(R.id.textThisSafeShort)).setTextColor( (is_safe)? Color.parseColor("#2E7D32") : Color.parseColor("#b71c1c"));
                                    ((TextView)findViewById(R.id.textThisSafe)).setText( (is_safe)? "You are in a safe zone. Contact or travel to other safe zones to meet with others." : "Your current zone is marked unsafe based on the number of cases in your area.\n\nIf you are uninfected, you can contact others to meet in safe zones below.");

                                    ProgressDialog pd = new ProgressDialog(MainActivity.this);
                                    pd.setMessage("Updating zone data...");
                                    pd.setCancelable(false);
                                    pd.show();
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            pd.cancel();
                                            try {
                                                discoverDevices();
                                                continueApp();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, 500);
                                }
                            });

                            builder.show();

//                            toast("No closer zones were found. Try again later.");
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_map)
                    .show();
            return false;
        }
    }

    private String readNetworkGraph() {
        String ret = "";

        try {
            InputStream inputStream = MainActivity.this.openFileInput("network.txt");

            if ( inputStream != null ) {
                Log.d("app", "Input stream not null");
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                int lines = 0;
                while ((receiveString = bufferedReader.readLine()) != null) {
                    Log.d("app", "Read " + receiveString);
                    lines++;
                    stringBuilder.append("\n").append(receiveString);
                }

                Log.d("app", "Done reading.");
                inputStream.close();
                ret = stringBuilder.toString();

                TextView v = (TextView)findViewById(R.id.textNearby);
                final int finalLines = lines;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        v.setText(String.valueOf(finalLines));
                    }
                });

            } else {
                Log.d("app", "Input stream was null");
            }
        }
        catch (FileNotFoundException e) {
            Log.e("app", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("app", "Can not read file: " + e.toString());
        }

        return ret;
    }

    private void discoverDevices() throws IOException {
//        Rockwall County (48 cases)\nLamar County (8 cases) nearby\n\nKaufman County (58 cases)

        // get adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // create discovery listener
        BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d("app" , device.getName() + "\n" + device.getAddress());
                    allDiscoveredDevices.add(device);
                }
            };
        };
//        unregisterReceiver(discoveryReceiver);

//        // and register it
//        IntentFilter filter = new IntentFilter();
//        {
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//            filter.addAction(BluetoothDevice.ACTION_FOUND);
//        }
//
//        MainActivity.this.registerReceiver(discoveryReceiver,filter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        // start discovert
        boolean started = bluetoothAdapter.startDiscovery(); //async call!
        if (!started) {
            // log error
            Log.e("app", "Error with bt device");
        }

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(MainActivity.this.openFileOutput("network.txt", Context.MODE_PRIVATE));
        outputStreamWriter.write(""); //clear the file

        final int[] maxValue = {-1};
        TextView v = (TextView)findViewById(R.id.textNearby);
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
//                if (isSearching) toast("");

                if (!isSearching) {
                    try {
//                        outputStreamWriter.flush();
                        outputStreamWriter.close();
                        Log.d("app", "Flushed and closed.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    this.cancel();
                    return;
                }
                Log.d("app", "devices: " + String.valueOf(allDiscoveredDevices));
                if (allDiscoveredDevices.size() > maxValue[0]) {
                    maxValue[0] = 2;//allDiscoveredDevices.size();
                    runOnUiThread(new Runnable(){
                        public void run()  {
                            v.setText("" + maxValue[0] + "");
                        }
                    });
                    for (int i = 0; i < allDiscoveredDevices.size(); i++) {
                        try {
                            String name = "";
                            if (allDiscoveredDevices.get(i).getName() == null || allDiscoveredDevices.get(i).getName().equals("null"))
                                name = "No Name";
                            else name = allDiscoveredDevices.get(i).getName();

                            outputStreamWriter.write(" " + allDiscoveredDevices.get(i).getAddress() + " / " + name + " / " + latlong + " / " + new Date().getTime() + " / " + ((cityName == null)? "Unknown" : cityName));
                            outputStreamWriter.write("\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }, 2000, 1000);

    }

    private String getFips(String zipName) throws IOException {
        InputStream wd = MainActivity.this.getAssets().open("zip2fips.json"); //getFilesDir().getAbsolutePath();
        //File f = new File(wd + "/tx-counties.csv");
        Scanner scn = new Scanner(wd);
        String contents = scn.useDelimiter("\\Z").next().replaceAll("\\R+", " ");
        System.out.println(contents.charAt(0));

        scn = new Scanner(contents);

        String fips = "";

        while (scn.hasNext()) {
            String next = scn.next();
            System.out.println(next);
            if (next.contains(zipName)) {
                //Stop!
                fips = scn.next();
                System.out.println("Fips is: " + fips + " for zip " + zipName);
                break;
            }
        }

        Log.d("app", "Fips is: " + fips + " for zip " + zipName);
        return String.valueOf(fips.split("\"")[1]);
    }

    private String[] extractZoneInformation(String fips) throws IOException {
        String res = "";

        InputStream wd = MainActivity.this.getAssets().open("tx-counties.csv"); //getFilesDir().getAbsolutePath();
        //File f = new File(wd + "/tx-counties.csv");
        Scanner scn = new Scanner(wd);
        while (scn.hasNextLine()) {
            String ln = scn.nextLine();
            Log.d("appln", ln);
            if (ln.contains(fips)) {
                //Stop!
                res = ln;
                Log.d("app", "Info: " + ln);
                break;
            }
        }

        return res.split(",");
    }

    @Deprecated
    private void verifyDatabase(String username, String contact) throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = null;
        Properties connectionProps = new Properties();
        String duser = "ZY2r9MnRiW", dpass = "iUtWvblLFY", ddat = "ZY2r9MnRiW", dhost = "remotemysql.com";
        connectionProps.put("user", duser);
        connectionProps.put("password", dpass);

        if (dbms.equals("mysql")) {
            String url = "jdbc:" + dbms + "://" + dhost + ":" + 3306 + "/" + ddat;
            System.out.println("Attempt " + url);
            conn = DriverManager.getConnection(
                    "jdbc:" + dbms + "://" + dhost + ":" + 3306 + "/" + ddat,
                    duser, dpass);
        }

        System.out.println("Connected to database");


    }

    private String getCounty(boolean existingUser) { //city?
        if (existingUser) {
//            return cityName;
            if ((existingResult.split("\""))[18] == null) return "Plano";
            else return (existingResult.split("\""))[18];
        } else {
            return cityName;
        }
    }

    private void getLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);

    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {

//            Toast.makeText(
//                    getBaseContext(),
//                    "Location changed: Lat: " + loc.getLatitude() + " Lng: "
//                            + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            String longitude = "Longitude: " + loc.getLongitude();
            Log.v("app", longitude);
            String latitude = "Latitude: " + loc.getLatitude();
            Log.v("app", latitude);

            /*------- To get city name from coordinates -------- */
            cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses.size() > 0) {
                    Log.d("app", addresses.get(0).getLocality());
//                    Log.d("app", addresses.get(0).getSubLocality());
//                    Log.d("app", addresses.get(0).getPostalCode());
                    cityName = addresses.get(0).getLocality();
                    zipName = addresses.get(0).getPostalCode();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            String s = longitude + "," + latitude + ", city "  + cityName;
            latlong = latitude.split(": ")[1] + "," + longitude.split(": ")[1];
            Log.d("app", s);
            Log.d("app", "\t" + zipName);
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}


