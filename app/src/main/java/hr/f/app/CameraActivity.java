package hr.f.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.opencv.core.Core.FONT_HERSHEY_SIMPLEX;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    OcrManager manager = new OcrManager();


    CascadeClassifier plateClasifier;
    MatOfRect rect = new MatOfRect();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        initializeOpenCVDependencies();
        cameraBridgeViewBase = findViewById(R.id.camera_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
       

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch (status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mat = inputFrame.rgba();
        Mat ret = inputFrame.rgba();
        plateClasifier.detectMultiScale(mat, rect);
        List<Rect> moguceReg = rect.toList();

        for(int i = 0; i < moguceReg.size(); i++){
            Imgproc.rectangle(mat,new Point(moguceReg.get(i).x, moguceReg.get(i).y),new Point(moguceReg.get(i).x + moguceReg.get(i).width,moguceReg.get(i).y + moguceReg.get(i).height), new Scalar(0,255,0));
            Mat sken = mat.submat(moguceReg.get(i));
            sken  =  OcrManager.prepareMatforOcr(sken);
            Bitmap bitmap = Bitmap.createBitmap(sken.cols(), sken.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(sken, bitmap);
            Imgproc.putText(mat,manager.recognizeText(bitmap), new Point(moguceReg.get(i).x,moguceReg.get(i).y),Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255,0,0));
        }

        return mat;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"Problem sa initom", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
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
        }}
}