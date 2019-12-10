package com.thaidt.testopencv;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.LifecycleOwner;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Rational;
import android.util.Size;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thaidt.testopencv.utils.ImageUtils;
import com.thaidt.testopencv.utils.OpenCvUtils;
import com.thaidt.testopencv.utils.QueueLinearFloodFiller;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGR;

public class NewFloodFillActivity extends AppCompatActivity implements View.OnTouchListener {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    TextureView textureView;
    ImageView ivBitmap, ivFillBitmap;
    int currentImageType = Imgproc.COLOR_RGB2GRAY;

    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    Preview preview;
    FloatingActionButton btnCapture;

    final Point mainPoint = new Point();
    Bitmap mainBitmap;
    Bitmap fillBitmap;
    Paint mPaint;

    ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_flood_fill);

        btnCapture = findViewById(R.id.btnCapture);

        textureView = findViewById(R.id.textureView);
        ivBitmap = findViewById(R.id.ivBitmap);
        ivFillBitmap = findViewById(R.id.ivFillBitmap);

        ivBitmap.setOnTouchListener(this);

        progressDialog = new ProgressDialog(this);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(5f);
        mPaint.setColor(Color.parseColor("#A6314B"));

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }


    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {

        CameraX.unbindAll();
        preview = setPreview();
        imageCapture = setImageCapture();
        imageAnalysis = setImageAnalysis();

        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner) this, preview, imageCapture, imageAnalysis);
    }


    private Preview setPreview() {

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
//                        updateTransform();
                    }
                });

        return preview;
    }


    private ImageCapture setImageCapture() {
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCapture = new ImageCapture(imageCaptureConfig);


        btnCapture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                imgCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        Bitmap bitmap = textureView.getBitmap();

                        CameraX.unbind(preview, imageAnalysis);
                        mainBitmap = bitmap;

                        fillBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                        fillBitmap.setHasAlpha(true);

                        ivBitmap.setVisibility(View.VISIBLE);
                        ivBitmap.setImageBitmap(bitmap);

                        btnCapture.hide();
                    }

                    @Override
                    public void onError(ImageCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
                        super.onError(useCaseError, message, cause);
                    }
                });


                /*File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "" + System.currentTimeMillis() + "_JDCameraX.jpg");
                imgCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        Bitmap bitmap = textureView.getBitmap();
                        showAcceptedRejectedButton(true);
                        ivBitmap.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {

                    }
                });*/
            }
        });

        return imgCapture;
    }

    private ImageAnalysis setImageAnalysis() {

        // Setup image analysis pipeline that computes average pixel luminance
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();


        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                .setImageQueueDepth(1).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        //Analyzing live camera feed begins.

                        final Bitmap bitmap = textureView.getBitmap();


                        if (bitmap == null)
                            return;

                        Mat mat = new Mat();
                        Utils.bitmapToMat(bitmap, mat);

//                        mat = OpenCvUtils.drawHoughLines(mat);

//                        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
//                        org.opencv.core.Point seedPoint = new org.opencv.core.Point(0,0);
//                        // flood fill with red
//                        Imgproc.floodFill(
//                                mat,
//                                new Mat(),
//                                seedPoint,
//                                new Scalar(204, 204, 204), // LTGRAY
//                                new Rect(),
//                                new Scalar(0, 0, 0),
//                                new Scalar(0, 0, 0),
//                                4
//                        );

//                        Imgproc.cvtColor(mat, mat, currentImageType);
                        Utils.matToBitmap(mat, bitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ivBitmap.setImageBitmap(bitmap);
                            }
                        });

                    }
                });
        return imageAnalysis;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mainPoint.x = (int) x;
            mainPoint.y = (int) y;
            final int sourceColor = mainBitmap.getPixel((int) x, (int) y);
            final int targetColor = mPaint.getColor();


//            Mat mat = new Mat();
//            Utils.bitmapToMat(mainBitmap, mat);
//
//            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
//            org.opencv.core.Point seedPoint = new org.opencv.core.Point(x, y);
//            // flood fill with red
//            Mat mask = Mat.zeros(mat.rows() + 2, mat.cols() + 2, CvType.CV_8U);
//
//            Imgproc.floodFill(
//                    mat,
//                    mask,
//                    seedPoint,
//                    new Scalar(Color.red(mPaint.getColor()), Color.green(mPaint.getColor()), Color.blue(mPaint.getColor())), // LTGRAY
//                    new Rect(),
//                    new Scalar(1, 1, 1),
//                    new Scalar(2, 2, 2),
//                    8
//            );

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ivBitmap.setImageBitmap(mainBitmap);
//                }
//            });

            new FloodFillTask(ivBitmap, ivFillBitmap, mainBitmap, fillBitmap, mainPoint, sourceColor, targetColor).execute();
        }
        return false;
    }

    private

    class FloodFillTask extends AsyncTask<Void, Integer, Bitmap> {

        Bitmap bmp;
        Bitmap fillBmp;
        Point pt;
        int newColor, oldColor;
        ImageView outputIv;
        ImageView fillOutputIv;

        public FloodFillTask(ImageView outIv, ImageView fillOutIv, Bitmap bm, Bitmap fillBmp, Point p, int nc, int oc) {
            this.bmp = bm;
            this.fillBmp = fillBmp;
            this.pt = p;
            this.newColor = oc;
            this.oldColor = nc;
            outputIv = outIv;
            fillOutputIv = fillOutIv;
            progressDialog.setMessage("Filling....");
            progressDialog.show();
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(bmp, fillBmp, oldColor, newColor);
            queueLinearFloodFiller.setTolerance(9 * 255 / 100);
            queueLinearFloodFiller.floodFill(pt.x, pt.y);
            return queueLinearFloodFiller.getFillImage();
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                fillOutputIv.setVisibility(View.VISIBLE);
                fillOutputIv.setImageBitmap(result);
            }
            progressDialog.dismiss();
        }
    }
}
