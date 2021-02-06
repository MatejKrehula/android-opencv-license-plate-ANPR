package hr.f.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HuoActivity extends AppCompatActivity {

    private String ipAdress;
    private String registration;
    private String date;
    private TextView showResponse;
    private EditText ipAdresa;
    private EditText registracija;
    private EditText datum;
    private Button submitRegButton;
    private TextView model;
    private TextView sasija;
    private TextView osiguranje;
    private TextView polica;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_huo);

        ipAdresa = findViewById(R.id.ipAdresa);
        registracija = findViewById(R.id.registracija);
        datum = findViewById(R.id.datum);
        submitRegButton = findViewById(R.id.sumbitRegButton);
        showResponse = findViewById(R.id.showResponseText);
        model = findViewById(R.id.model);
        sasija = findViewById(R.id.sasija);
        osiguranje = findViewById(R.id.osiguranje);
        polica = findViewById(R.id.polica);

        submitRegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAdress = ipAdresa.getText().toString();
                registration = registracija.getText().toString();
                date = datum.getText().toString();
                sendRequestToServer(ipAdress, registration, date);
            }
        });

    }

    public void sendRequestToServer(String ip, String reg, final String date){
        final String grad = reg.substring(0,2);
        final String desniDio =  reg.replaceAll(grad, "").trim();
        RequestQueue queue= Volley.newRequestQueue(HuoActivity.this);
        String url="http://" + ip;
        StringRequest stringRequest=new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response.equals("NE")){
                    setNull();
                    showResponse.setText("NIJE OSIGURAN");

                } else {
                    setNull();
                    try {
                        JSONObject object = new JSONObject(response);
                        model.setText("Model vozila: " + object.getString("marka"));
                        sasija.setText("Broj šasije: " + object.getString("sasija"));
                        osiguranje.setText("Osiguranje: " + object.getString("osiguranje"));
                        polica.setText("Broj police: " + object.getString("polica"));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                    setNull();
                    showResponse.setText(error.toString());
                    
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                params.put("date", date);
                params.put("reg2", desniDio);
                params.put("city", grad);
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    public void setNull(){
        showResponse.setText("");
        model.setText("");
        sasija.setText("");
        osiguranje.setText("");
        polica.setText("");
    }
}