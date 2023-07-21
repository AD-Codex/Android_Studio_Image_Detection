package com.example.opencv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class opencvCamera extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    TextView textView ;

    ArrayList<String> classList = new ArrayList<String>();
    private Net net;

    Mat mRGBA;
    Mat mRGBAT;
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:{
                    Log.i(TAG,"onManagerConnected: opencv loaded");
                    cameraBridgeViewBase.enableView();
                }
                default:{
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(opencvCamera.this, new String[]{Manifest.permission.CAMERA}, 1);
        setContentView(R.layout.activity_opencv_camera);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_surface);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

//        cameraBridgeViewBase.setMaxFrameSize(320,320);

    }


    public void onCameraViewStarted(int width, int height) {

        // face detection
        String proto = getPath("deploy.prototxt", this);
        String weights = getPath("res10_300x300_ssd_iter_140000.caffemodel", this);

        String model1 = getPath("frozen_inference_graph.pb", this);
        String config = getPath("ssd_mobilenet_v3_large_coco_2020_01_14.pbtxt", this);

//        // caffe model face detect
//        net = Dnn.readNetFromCaffe(proto, weights);

        // object detect
        net = Dnn.readNet( model1, config);


        Log.i(TAG, "Network loaded successfully " + net);
        Toast.makeText(this, "Network loaded successfully", Toast.LENGTH_SHORT).show();


        String string = "";
        InputStream is = this.getResources().openRawResource(R.raw.labels);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (true) {
            try {
                if ((string = reader.readLine()) == null)
                    break;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            classList.add(string);
            Log.d("TAG", string);
        }
//        Toast.makeText(this, classList.get(0), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Class list uploaded", Toast.LENGTH_SHORT).show();




    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 1/127.5;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.5;

        // Get a new frame
        Mat frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

//        Rect roi = new Rect(0,0,700,700);
//        Mat crop = new Mat(frame, roi);
//        Imgproc.rectangle(frame, new Point(0,0), new Point(700,700), new Scalar(255, 0, 0));


        // Forward image through network.
        Mat blob = Dnn.blobFromImage( frame, IN_SCALE_FACTOR, new Size(IN_WIDTH, IN_HEIGHT), new Scalar(127.5, 127.5, 127.5), /*swapRB*/true, /*crop*/false);


        net.setInput(blob);
        Mat detections = net.forward();
        int cols = frame.cols();
        int rows = frame.rows();
        detections = detections.reshape(1, (int)detections.total() / 7);


        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];
            if (confidence > THRESHOLD) {
                int classId = (int)detections.get(i, 1)[0];
                int left = (int)(detections.get(i, 3)[0] * cols)-25;
                int top = (int)(detections.get(i, 4)[0] * rows)-25;
                int right = (int)(detections.get(i, 5)[0] * cols)-25;
                int bottom = (int)(detections.get(i, 6)[0] * rows)-25;
                // Draw rectangle around detected object.
                Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom), new Scalar(0, 255, 0), 3);

//                String label = classList.get(classId-1) + " " + String.format("%d", classId);
                String label = classList.get(classId-1) + " " + String.format("%.2f", confidence);

                Log.d("TAG", label);
//                int[] baseLine = new int[1];
//                Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
//                // Draw background for label.
//                Imgproc.rectangle(frame, new Point(left, top - labelSize.height),
//                        new Point(left + labelSize.width, top + baseLine[0]),
//                        new Scalar(255,255,255), Imgproc.COLOR_RGBA2RGB);
//                // Write class name and confidence.
                Imgproc.putText(frame, label, new Point(left+10, top+10), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 255), 3);
            }
        }
        return frame;
    }


    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
            Toast.makeText(context, "Failed to upload a file", Toast.LENGTH_SHORT).show();
        }
        return "";
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // camera request
        switch (requestCode){
            case 1:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                    cameraBridgeViewBase.setCameraPermissionGranted();
                }
                else{

                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.d(TAG,"onResume: opencv initialized");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.d(TAG,"onResume: opencv not initialized");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase != null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase != null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public void onCameraViewStopped() {

//        mRGBA.release();
    }

//    @Override
//    public void onCameraViewStarted(int width, int height) {
//        mRGBA = new Mat(height, width, CvType.CV_8SC4);
//        mRGBAT = new Mat(height, width, CvType.CV_8SC1);
//    }

//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        mRGBA = inputFrame.rgba();
//        mRGBAT = inputFrame.gray();
//        return mRGBA;
//    }
}