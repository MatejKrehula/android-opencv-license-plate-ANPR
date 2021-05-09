package hr.f.app;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }
    public static MainActivity instance = null;
    private static final int PICK_IMAGE = 100;
    private Uri imagUri;
    private TextView imageLoc;
    private  String test = "";
    private ImageView imgView;
    private OcrManager manager = new OcrManager();
    public CascadeClassifier plateClasifier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        copyTessDataForTextRecognizor();
        initializeOpenCVDependencies();
        setContentView(R.layout.activity_main);

        //wire up the button to take the picture
        Button huoButton = findViewById(R.id.huoTestButton);
        Button btn = findViewById(R.id.takePhoto);
        imageLoc = findViewById(R.id.imageLoc);
        imgView = findViewById(R.id.imgView);
        imageLoc.setText(test);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takPictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    startActivityForResult(takPictureIntent,1);
                }catch (ActivityNotFoundException e){
                    Toast.makeText(getApplicationContext(),"Can't open camera", Toast.LENGTH_SHORT).show();
                }
            }
        });

        huoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openHuoActivity = new Intent(MainActivity.this, HuoActivity.class);
                startActivity(openHuoActivity);
            }
        });

        //get picture from gallery and show the result on screen

        Button getPhoto = findViewById(R.id.button2);

        getPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();

            }
        });



        Button openCameraView = findViewById(R.id.OpenVideoActivity);

        openCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openVideoIntent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(openVideoIntent);
            }
        });

        manager.initApi();
    }



    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent,PICK_IMAGE);
    }

    private  String tessDataPath(){
        return  MainActivity.instance.getExternalFilesDir(null)+ "/tessdata/";
    }

    public String getTessDataParentDirectory(){
        return MainActivity.instance.getExternalFilesDir(null).getAbsolutePath();
    }




    //kopiranje treniranih podataka
    private  void copyTessDataForTextRecognizor(){

        Runnable run = new Runnable() {
            @Override
            public void run() {
                AssetManager assetManager = MainActivity.instance.getAssets();
                OutputStream out = null;
                try {
                    InputStream in = assetManager.open("hrv.traineddata");
                    String tesspath = instance.tessDataPath();
                    File tessFolder = new File(tesspath);
                    if(!tessFolder.exists()){
                        tessFolder.mkdir();
                    }

                    String tessData = tesspath + "/" + "hrv.traineddata";
                    File tessFile = new File(tessData);
                    if(!tessFile.exists()){
                        out = new FileOutputStream(tessData);
                        byte[] buffer = new byte[1024];
                        int read = in.read(buffer);
                        while (read != -1){
                            out.write(buffer,0,read);
                            read = in.read(buffer);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (out != null){
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        new Thread(run).start();
    }

    public void initializeOpenCVDependencies() {
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.haarcascade_russian_plate_number);
            File cascadeDir = getDir("haarcascade_russian_plate_number", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_russian_plate_number.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            // Load the cascade classifier
            plateClasifier= new CascadeClassifier(mCascadeFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Mat input = new Mat();
        Mat grayMat = new Mat();
        Mat finalMat = new Mat();

        Bitmap bitmap = null;
        Bitmap finalBitmap = null;
        if(resultCode == RESULT_OK && (requestCode == PICK_IMAGE || requestCode == 1)){
                if(requestCode == PICK_IMAGE) {
                    imagUri = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imagUri);
                        finalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imagUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    bitmap=(Bitmap) data.getExtras().get("data");
                    finalBitmap = (Bitmap) data.getExtras().get("data");

                }


            Utils.bitmapToMat(bitmap, input );
            Utils.bitmapToMat(finalBitmap, finalMat );


            Imgproc.cvtColor(input,grayMat, Imgproc.COLOR_RGB2GRAY);


            MatOfRect plates = new MatOfRect();
            plateClasifier.detectMultiScale(input, plates);

            List<Rect> moguceReg = plates.toList();
            try {
                moguceReg.get(0).height = (int) (0.90 * moguceReg.get(0).height);

            } catch (IndexOutOfBoundsException e){
                imageLoc.setText("Registracija nije nadena");
            }


            try {
                Mat imageOut = finalMat.submat(moguceReg.get(0));
                imageOut = OcrManager.prepareMatforOcr(imageOut);
                //Imgproc.resize(imageOut, imageOut, new Size(250,50));
                bitmap = Bitmap.createBitmap(imageOut.cols(), imageOut.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(imageOut, bitmap);
                imgView.setImageBitmap(bitmap);
                imageLoc.setText(manager.recognizeText(bitmap));
            } catch (Exception e){
                imageLoc.setText("Registracija nije nadena");
            }

        }
    }
}