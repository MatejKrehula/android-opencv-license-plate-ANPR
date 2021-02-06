package hr.f.app;

import android.graphics.Bitmap;
import android.provider.ContactsContract;

import androidx.core.view.accessibility.AccessibilityViewCommand;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OcrManager {
       TessBaseAPI baseAPI = null;


    public void initApi(){
        baseAPI = new TessBaseAPI();
        String dataPath = MainActivity.instance.getTessDataParentDirectory();

        baseAPI.init(dataPath, "hrv");

    }


    public String recognizeText(Bitmap bitmap){
        if (baseAPI == null){
            initApi();
        }

        baseAPI.setImage(bitmap);
        return  baseAPI.getUTF8Text();
    }


    public  static Mat prepareMatforOcr(Mat input){

            Mat dest = new Mat();
            Imgproc.cvtColor(input, dest, Imgproc.COLOR_RGB2GRAY);
            Imgproc.resize(dest, dest, new Size(), 1.2, 1.2, Imgproc.INTER_CUBIC);
            Imgproc.threshold(dest, dest, 0, 255, Imgproc.THRESH_OTSU);
            Imgproc.erode(dest, dest, new Mat(), new Point(-1, -1), 1);
            Imgproc.dilate(dest, dest, new Mat(), new Point(-1, -1), 1);
            List<MatOfPoint> contours = new ArrayList();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dest, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            Collections.sort(contours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint o1, MatOfPoint o2) {
                    return -1 * Double.compare(Imgproc.contourArea(o1), Imgproc.contourArea(o2));
                }

                ;
            });

            Rect candidate = Imgproc.boundingRect(contours.get(0));
            Mat finalMat = dest.submat(candidate);


            Imgproc.dilate(finalMat, finalMat, new Mat(), new Point(-1, -1), 2);
            Imgproc.erode(finalMat, finalMat, new Mat(), new Point(-1, -1), 2);


            return finalMat;
        }
}

