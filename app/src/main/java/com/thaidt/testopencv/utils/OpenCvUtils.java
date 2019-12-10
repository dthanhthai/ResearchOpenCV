package com.thaidt.testopencv.utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

public class OpenCvUtils {
    public static Mat detectEdges(Mat frame) {
        Mat edges = new Mat(frame.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(frame, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(edges, edges, 80, 100);

        // Don't do that at home or work it's for visualization purpose.
//        BitmapHelper.showBitmap(this, bitmap, imageView);
//        Bitmap resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(edges, resultBitmap);
//        BitmapHelper.showBitmap(this, resultBitmap, detectEdgesImageView);
        return edges;
    }

    /**
     * Apply Sobel
     *
     * @param frame the current frame
     * @return an image elaborated with Sobel derivation
     */
    public static Mat doSobel(Mat frame) {
        // init
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();
        int scale = 1;
        int delta = 0;
        int ddepth = CvType.CV_16S;
        Mat grad_x = new Mat();
        Mat grad_y = new Mat();
        Mat abs_grad_x = new Mat();
        Mat abs_grad_y = new Mat();

        // reduce noise with a 3x3 kernel
        Imgproc.GaussianBlur(frame, frame, new Size(3, 3), 0, 0, Core.BORDER_DEFAULT);

        // convert to grayscale
        Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Gradient X
        // Imgproc.Sobel(grayImage, grad_x, ddepth, 1, 0, 3, scale,
        // this.threshold.getValue(), Core.BORDER_DEFAULT );
        Imgproc.Sobel(grayImage, grad_x, ddepth, 1, 0);
        Core.convertScaleAbs(grad_x, abs_grad_x);

        // Gradient Y
        // Imgproc.Sobel(grayImage, grad_y, ddepth, 0, 1, 3, scale,
        // this.threshold.getValue(), Core.BORDER_DEFAULT );
        Imgproc.Sobel(grayImage, grad_y, ddepth, 0, 1);
        Core.convertScaleAbs(grad_y, abs_grad_y);

        // Total Gradient (approximate)
        Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 0, detectedEdges);
        // Core.addWeighted(grad_x, 0.5, grad_y, 0.5, 0, detectedEdges);

        return detectedEdges;
    }

    public static Mat drawHoughLines(Mat frame) {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat lines = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(frame, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(grayMat, cannyEdges, 80, 100);

        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, 50, 20, 20);

        Mat houghLines = new Mat();
        houghLines.create(cannyEdges.rows(), cannyEdges.cols(), CvType.CV_8UC1);

        //Drawing lines on the image
        for (int i = 0; i < lines.cols(); i++) {
            double[] points = lines.get(0, i);
            double x1, y1, x2, y2;

            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            org.opencv.core.Point pt1 = new org.opencv.core.Point(x1, y1);
            org.opencv.core.Point pt2 = new org.opencv.core.Point(x2, y2);

            //Drawing lines on an image
            Imgproc.line(houghLines, pt1, pt2, new Scalar(255, 0, 0), 1);
        }

        return houghLines;
    }

    public static Mat newHoughLines(Mat originFrame) {
        Mat mRGBA = originFrame;
        Imgproc.cvtColor(mRGBA, mRGBA, Imgproc.COLOR_BGR2RGB);

        Mat mGray = new Mat();
        Imgproc.cvtColor(mRGBA, mGray, Imgproc.COLOR_RGBA2GRAY);

        Imgproc.medianBlur(mGray, mGray, 7);

        /* Main part */

        Imgproc.Canny(mGray, mGray, 50, 60, 3, true);

        Mat aretes = new Mat();
        Imgproc.HoughLinesP(mGray, aretes, 1, 0.01745329251, 30, 10, 4);

/**
 * Tag Canny edges in the gray picture with indexes from 1 to 65535 (0 = background)
 * (Make sure there are less than 255 components or convert mGray to 16U before)
 */
        int nb = Imgproc.connectedComponents(mGray, mGray, 8, CvType.CV_16U);

        Imgproc.dilate(mGray, mGray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));


// for each Hough line
        for (int x = 0; x < aretes.rows(); x++) {
            double[] vec = aretes.get(x, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];

            /**
             * Take 5 points from the line
             *
             *   x----x----x----x----x
             *   P1                  P2
             */
            double[] pixel_values = new double[5];
            pixel_values[0] = mGray.get((int) y1, (int) x1)[0];
            pixel_values[1] = mGray.get((int) (y1 * 0.75 + y2 * 0.25), (int) (x1 * 0.75 + x2 * 0.25))[0];
            pixel_values[2] = mGray.get((int) ((y1 + y2) * 0.5), (int) ((x1 + x2) * 0.5))[0];
            pixel_values[3] = mGray.get((int) (y1 * 0.25 + y2 * 0.75), (int) (x1 * 0.25 + x2 * 0.75))[0];
            pixel_values[4] = mGray.get((int) y2, (int) x2)[0];

            /**
             * Look for the most frequent value
             * (To make it readable, the following code accepts the line only if there are at
             * least 3 good pixels)
             */
            double value;
            Arrays.sort(pixel_values);

            if (pixel_values[1] == pixel_values[3] || pixel_values[0] == pixel_values[2] || pixel_values[2] == pixel_values[4]) {
                value = pixel_values[2];
            } else {
                value = 0;
            }

            /**
             * Now value is the index of the connected component (or 0 if it's a bad line)
             * You can store it in an other array, here I'll just draw the line with the value
             */
            if (value != 0) {
                Imgproc.line(mRGBA, new org.opencv.core.Point(x1, y1), new org.opencv.core.Point(x2, y2), new Scalar(255, 255, 255), 3);
            }
        }

        Imgproc.cvtColor(mRGBA, mRGBA, Imgproc.COLOR_RGB2BGR);
        return mRGBA;
    }

}
