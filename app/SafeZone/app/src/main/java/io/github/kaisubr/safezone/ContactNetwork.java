package io.github.kaisubr.safezone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import giwi.org.networkgraph.GraphSurfaceView;
import giwi.org.networkgraph.beans.NetworkGraph;
import giwi.org.networkgraph.beans.Vertex;
import me.angrybyte.numberpicker.view.ActualNumberPicker;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleNode;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ContactNetwork extends AppCompatActivity {

    //Vertices in the network.
    private Node[] out;

    //Used by the model.
    static double[] defaultPredictors = {0.3289518895612236,0.4055666833453549,0.7072831683213328,0.292586861446397,0.4694621717727631,-3.1217994208190545,-1141.491226465791,-1827.3792122623274,-4276.360201105384,-4825.057904873224,-5765.535202882489,-3230.038779179848,-7040.293594993108,0.041999966319193005,0.008942254256411984,0.049275020629494286,0.01455010862060255,0.008622286589986696,0.01259662181505869,0.013910173287751975,0.01040736936056988,0.006651959380946768,0.03645947356898671,0.05801519004395345,0.010003199676664253,0.0059615028376079895,0.007847628029167579,0.19046496354052644,0.012714504639531164,0.009161179501860864,0.0074939795557501555,0.009295902729829407,0.00813391488860073,0.014600629831090752,0.03716677051582156,0.09777538269816945,0.018894932722588034,0.08945622337111198,0.013438641989862077,0.011855644061231707,0.014937437901012108,0.0117546016402553,0.02504167999865277,0.010895741061955844,0.021168387194557183,0.02283558714066789,0.02064633468617908,0.0069382462403799194,0.01040736936056988,0.06658695542345194,0.006837203819403513,0.013657567235310959,0.05695424462370118,0.010053720887152456,0.04553645105336724,0.0107104966234991,0.007527660362742291,0.013691248042303093,0.008487563362018153,0.0199053569323521,0.05449554571327529,8.641821458042134,1.300904329667739,2.142958185278119,2.6862296020612653,1.7276064734511039,1.0557922567824725,2.1469830417136793,1.958707330627642,1.9019888516528856,2.048483521665179,2.4193597278590797,1.2093262154561224,2.007426617941766,5.835839746720332,2.883666492649164,1.0271804112426535,1.4091881241474546,1.038530843199003,-1490.023492362877,-9903.88462639565,2.993836412320439,2.056600596150284,2.768141324666139,2.9685421262693454,-7478.580657112544,1.327529007595022,2.9780064330341354,1.0535356427139995,1.0344554655529548,253.98710025092203,1.985079402502484,1.1089910914265506,1.9816439601892861,2.528115053636685,-9355.719068388878,1.1949613512739765,2.8089793031441035,2.9802125258921204,1.0672100503528064,2.54269884306428,2.102170728010643,2.04077061686398,2.9852646469409407,-9812.372307640491,2.804617638638622,2.689075630252101,1.0020545292265202,2.179468180057594,1.938397804011384,1.0048500362068675,2.830720264057527,1.6548727707515871,2.9675990636735654,1.6410636398848117,1.0073592563277816,2.889897441942709,2.0122766541486334,2.0440881763527052,1.7699432478402182,1.026355231471346,0.03979387346120813,0.03324295650123777,0.016453074215658208,0.003536484734174231,0.013051312709452518,0.03533116653475017,0.02329027803506172,0.11510415789562319,0.0009093817887876593,0.018894932722588034,0.004799514996379314,0.0026944645593708423,0.005153163469796736,0.0036880483656388406,0.11037200451322814,0.10585877637628198,0.24117141846718648,0.1820784425994847,0.04457654805409138,24.415654839089946,2.006955086643876,2.6735992994392146,1.0435829642478234};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_network);

        String data = getIntent().getStringExtra("EXTRA_NETWORK_NODES");

        NetworkGraph graph = new NetworkGraph();

        Scanner scn = new Scanner(data);
        Node v = new SimpleNode("You");
        graph.getVertex().add(new Vertex(v, ContextCompat.getDrawable(ContactNetwork.this, R.drawable.person_purple)));

        String[] strNodes = data.split("\n");
        Log.d("app.con", Arrays.toString(strNodes));
        out = new SimpleNode[strNodes.length];

        for (int i = 1; i < strNodes.length; i++) {
            String line = strNodes[i];
            String[] config = line.split(" / ");
            Log.d("app.con", Arrays.toString(config));
//config[0].substring(1, 3) + config[3].substring(config[3].length() - 3)
            long millis = (new Date().getTime() - Long.parseLong(config[3]));
            long days = TimeUnit.MILLISECONDS.toDays(millis);
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
            Log.d("app.con", "dhrms: " + days + ", " + hours + ", " + minutes + ", " + seconds);
            String label = hours + " hr (" + "" + config[4] + ")";
            Log.d("app.con", "Label '" + label + "'");
            out[i] = new SimpleNode(label);

            graph.getVertex().add(new Vertex(out[i], ContextCompat.getDrawable(ContactNetwork.this, R.drawable.person_bw)));
            graph.addEdge(new SimpleEdge(out[i], v,"11"));
        }

        GraphSurfaceView surface = (GraphSurfaceView) findViewById(R.id.mysurface);
        surface.init(graph);

        ((Button)findViewById(R.id.buttonRiskAnalysis)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForm();
            }
        });

    }

    private void showForm() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View myclaimView = factory.inflate(R.layout.dialog_myclaim, null);
        final AlertDialog myclaim = new AlertDialog.Builder(this).create();

        myclaim.setView(myclaimView);
        myclaim.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int age = ((ActualNumberPicker)myclaim.findViewById(R.id.numberPickerAge)).getValue();
                int health = ((ActualNumberPicker)myclaim.findViewById(R.id.numberPickerHealth)).getValue();

                Log.d("app.con", "age " + age + " health " + health);

                predict(age, health, MainActivity.healthy, out.length);

                myclaim.hide();
            }
        });

        myclaim.show();
        ((TextView)myclaim.findViewById(R.id.textNumOthers)).setText(out.length + " others");
        ((TextView)myclaim.findViewById(R.id.textIsInfected)).setText( ((MainActivity.healthy)? "Healthy, based on information at registration." : "Infected, based on information at registration.") + " (tap to change)");
        ((TextView)myclaim.findViewById(R.id.textIsInfected)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.healthy = !MainActivity.healthy;
                ((TextView)myclaim.findViewById(R.id.textIsInfected)).setText( ((MainActivity.healthy)? "Healthy, based on information at registration." : "Infected, based on information at registration.") + " (tap to change)");
            }
        });
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
                Toast.makeText(ContactNetwork.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Higher is better (better response for low risk claim)
    private void predict(int age, int health, boolean isHealthy, int numContact) {
        defaultPredictors[1] = age/100.;
        for (int i = 13; i <= 48; i++) defaultPredictors[i] = health/50.;
        int[] histInd = {81,82,83,84,86,87,88,89,91,92,93,94,96,97,98,99,100,101,102,103,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119};
        for (int j = 0; j < histInd.length; j++)
            defaultPredictors[histInd[j]] += ((isHealthy)? 0.05 : 0.2) * numContact;
        String array = "";
        for (int k = 0; k < defaultPredictors.length; k++) array += defaultPredictors[k] + ",";
        String command = "https://floating-garden-33325.herokuapp.com/execute/mlpreg?array=" + array;
        Log.d("app.con", "> " + command);
        try {
            String result = makeRequest(command).split("n\\.")[1];
            double norm = normalize(Double.valueOf(result), age, health);

            ((TextView)findViewById(R.id.textViewResponseHighIsBetter)).setText(
                    "You have a predicted " + Math.round(norm) + "% response rate. " +
                            ((norm > 50)? "This is was predicted to be a relatively low-risk claim." : "This might be a high-risk claim.")
            );

//            toast(String.valueOf(norm));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double normalize(double v, int age, int health) {
        double r = 10 * Math.abs(Math.round( 10000.0 * (v-(age/57.)-(health/1.7)/*(int)(out[2].getLabel().charAt(out[2].getLabel().length()-1))/100.*/)*1.4) / 10000.0);
        return r;
    }
//
//    private static Bitmap getBitmap(Context context, int drawableId) {
//        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
//        if (drawable instanceof BitmapDrawable) {
//            return ((BitmapDrawable) drawable).getBitmap();
//        } else if (drawable instanceof VectorDrawable) {
//            return getBitmap((VectorDrawable) drawable);
//        } else {
//            throw new IllegalArgumentException("unsupported drawable type");
//        }
//    }
//
//    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
//        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
//                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//        vectorDrawable.draw(canvas);
//        return bitmap;
//    }
}
