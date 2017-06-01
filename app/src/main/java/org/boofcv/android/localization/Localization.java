///*
// * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
// *
// * This file is part of BoofCV (http://boofcv.org).
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.boofcv.android.localization;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.os.Environment;
//import android.util.Log;
//
//import boofcv.alg.feature.detect.template.TemplateMatching;
//import boofcv.alg.feature.detect.template.TemplateMatchingIntensity;
//import boofcv.alg.misc.ImageStatistics;
//import boofcv.alg.misc.PixelMath;
//import boofcv.android.ConvertBitmap;
//import boofcv.factory.feature.detect.template.FactoryTemplateMatching;
//import boofcv.factory.feature.detect.template.TemplateScoreType;
//import boofcv.gui.image.ShowImages;
//import boofcv.io.UtilIO;
//import boofcv.io.image.ConvertBufferedImage;
//import boofcv.io.image.UtilImageIO;
////import boofcv.struct.feature.Match;
//import boofcv.struct.image.GrayF32;
//import boofcv.struct.image.GrayU8;
//
//import java.io.File;
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Queue;
//
//
///**
// * Example of how to find objects inside an image using template matching.  Template matching works
// * well when there is little noise in the image and the object's appearance is known and static.  It can
// * also be very slow to compute, depending on the image and template size.
// *
// * @author Peter Abeles
// */
//public class Localization {
//    private static List<Dimension> boxes;
//
//    /**
//     * Demonstrates how to search for matches of a template inside an image
//     *
//     * @param image           Image being searched
//     * @param template        Template being looked for
//     * @param mask            Mask which determines the weight of each template pixel in the match score
//     * @param expectedMatches Number of expected matches it hopes to find
//     * @return List of match location and scores
//     */
//    private static List<Match> findMatches(GrayF32 image, GrayF32 template, GrayF32 mask,
//                                           int expectedMatches) {
//        // create template matcher.
//        TemplateMatching<GrayF32> matcher =
//                FactoryTemplateMatching.createMatcher(TemplateScoreType.SUM_DIFF_SQ, GrayF32.class);
//
//        // Find the points which match the template the best
//        matcher.setImage(image);
//        matcher.setTemplate(template, mask, expectedMatches);
//        matcher.process();
//        List<boofcv.struct.feature.Match> temp = matcher.getResults().toList();
//        List<Match> matchList = new ArrayList<Match>();
//        for (boofcv.struct.feature.Match m : temp) {
//            matchList.add(new Match(m.x, m.y, m.score));
//        }
//        return matchList;
//
//    }
//
//    /**
//     * Computes the template match intensity image and displays the results. Brighter intensity indicates
//     * a better match to the template.
//     */
//    public static void showMatchIntensity(GrayF32 image, GrayF32 template, GrayF32 mask) {
//
//        // create algorithm for computing intensity image
//        TemplateMatchingIntensity<GrayF32> matchIntensity =
//                FactoryTemplateMatching.createIntensity(TemplateScoreType.NCC, GrayF32.class);
//
//        // apply the template to the image
//        matchIntensity.setInputImage(image);
//        matchIntensity.process(template, mask);
//
//        // get the results
//        GrayF32 intensity = matchIntensity.getIntensity();
//        // adjust the intensity image so that white indicates a good match and black a poor match
//        // the scale is kept linear to highlight how ambiguous the solution is
//        float min = ImageStatistics.min(intensity);
//        float max = ImageStatistics.max(intensity);
//        float range = max - min;
//        System.out.println("Min: " + min + ", Max: " + max + ", Range: " + range);
//        PixelMath.plus(intensity, -min, intensity);
//        PixelMath.divide(intensity, range, intensity);
//        PixelMath.multiply(intensity, 255.0f, intensity);
//
////        BufferedImage output = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_BGR);
////        VisualizeImageData.grayMagnitude(intensity, output, -1);
////        ShowImages.showWindow(output, "Match Intensity", true);
//    }
//
//    public static List<Dimension> runTemplateMatching(Bitmap shelf) {
//
//        // Load image and templates
////        GrayF32 t = BitmapFactory.decodeByteArray(imageDir.toString(), GrayF32.class);
////        GrayF32 image = UtilImageIO.loadImage(shelf.toString(), GrayF32.class);
////        GrayU8 imageU8 = ConvertBitmap.bitmapToGray(shelf, (GrayU8)null, null);
//        byte[] workBuffer = ConvertBitmap.declareStorage(shelf, null);
//        GrayF32 image = ConvertBitmap.bitmapToGray(shelf, (GrayF32) null, workBuffer);
//
//        String templatesDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES), "templates").toString();
//
//        Bitmap templateTemp = BitmapFactory.decodeFile(templatesDir + "/template1.png");
//        byte[] templateBuffer = ConvertBitmap.declareStorage(templateTemp, null);
//        GrayF32 template = ConvertBitmap.bitmapToGray(templateTemp, (GrayF32) null, templateBuffer);
//        templateTemp = BitmapFactory.decodeFile(templatesDir + "/template2.png");
//        templateBuffer = ConvertBitmap.declareStorage(templateTemp, null);
//        GrayF32 template2 = ConvertBitmap.bitmapToGray(templateTemp, (GrayF32) null, templateBuffer);
//        templateTemp = BitmapFactory.decodeFile(templatesDir + "/template3.png");
//        templateBuffer = ConvertBitmap.declareStorage(templateTemp, null);
//        GrayF32 template3 = ConvertBitmap.bitmapToGray(templateTemp, (GrayF32) null, templateBuffer);
//        templateTemp = BitmapFactory.decodeFile(templatesDir + "/template4.png");
//        templateBuffer = ConvertBitmap.declareStorage(templateTemp, null);
//        GrayF32 template4 = ConvertBitmap.bitmapToGray(templateTemp, (GrayF32) null, templateBuffer);
//        templateTemp = BitmapFactory.decodeFile(templatesDir + "/template5.png");
//        templateBuffer = ConvertBitmap.declareStorage(templateTemp, null);
//        GrayF32 template5 = ConvertBitmap.bitmapToGray(templateTemp, (GrayF32) null, templateBuffer);
//        // create output image to show results
//        showMatchIntensity(image, template, null);
//        int i = 0;
//        int th = 4500000;
//        System.out.println(th);
//        List<Match> allMatches = new ArrayList<>();
//
//        int expected = 15;
//
//        allMatches = findMatches(image, template, null, expected);
//        allMatches.addAll(findMatches(image, template2, null, expected));
//        allMatches.addAll(findMatches(image, template3, null, expected));
//        allMatches.addAll(findMatches(image, template4, null, expected));
//        allMatches.addAll(findMatches(image, template5, null, expected));
//        allMatches = eliminateMatches(allMatches, th);
//        int width = (template.getWidth() + template2.getWidth()) / 2;
//        int height = (template.getHeight() + template2.getHeight()) / 2;
//        int image_height = image.height;
//        boxes = (getboxesDimensions(allMatches, width, height, image_height));
//        Log.d("Template dimension", template.getWidth() + ", " + template.getHeight());
//
////            File outputfile = new File("shelf_" + th + ".jpg");
////            try {
////                ImageIO.write(output, "jpg", new File("data/boxDetection/Samsung/" + outputfile.getName()));
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
//
////        }
////        System.out.println("DONE");
//        return boxes;
//    }
//
//    private static List<Match> eliminateMatches(List<Match> matches, int treshold) {
////        System.out.println(matches + " " + "Size: " + matches.size());
//        List<Match> result = new ArrayList<Match>();
//        Queue<Match> queue = new LinkedList<>(matches);
//        while (!queue.isEmpty()) {
//            boolean match = false;
//            Match m = queue.poll();
//            if (m.score < -treshold)  // 2900000
//                continue;
////            System.out.println(m.toString());
//            Queue tempQ = new LinkedList();
//            while (!queue.isEmpty()) {
////                System.out.println("Size Q: " + queue.size());
//                Match temp = queue.poll();
//                if (temp.equals(m)) {
//                    match = true;
////                    System.out.println("Match: " + m.toString() + " & " + temp.toString());
//                    if (temp.score > m.score) {
//                        m = temp;
//                    }
//                } else {
//                    tempQ.add(temp);
//                }
//            }
//            queue = tempQ;
//            if (match) {
//                result.add(m);
//            }
//        }
//        Log.d("RESULT SIZE", "" + result.size());
//        return result;
//    }
//
//    /**
//     * Helper function will is finds matches and displays the results as colored rectangles
//     */
//    private static List<Dimension> getboxesDimensions(List<Match> found, int width, int height, int image_height) {
//        List<Dimension> boxes = new ArrayList<Dimension>();
//        int r = 2;
//        int w = width + 2 * r;
//        int h = height + 2 * r;
//
//        List<Integer> xs = new ArrayList<Integer>();
//        List<Integer> y_min = new ArrayList<Integer>();
//        int y_max = 0;
//        for (Match m : found) {
//            System.out.println("Match " + m.x + " " + m.y + "    score " + m.score);
//            int x0 = m.x - r;
//            int y0 = m.y - r;
//            int x1 = x0 + w;
//            int y1 = y0 + h;
//            boxes.add(new Dimension(x0, y0, x1, y1, Color.RED));
////            g2.setColor(Color.RED);
////            g2.setStroke(new BasicStroke(2));
////            g2.drawString(m.x + " " + m.y, m.x, m.y + 10);
////            g2.drawLine(x0, y0, x1, y0);
////            g2.drawLine(x1, y0, x1, y1);
////            g2.drawLine(x1, y1, x0, y1);
////            g2.drawLine(x0, y1, x0, y0);
//
//            if (xs.isEmpty()) {
//                xs.add(m.x);
//                y_min.add(m.y);
//                y_max = m.y;
//            } else {
//                boolean done = false;
//                for (int i = 0; i < xs.size(); i++) {
//                    int x = xs.get(i);
//                    if (Math.abs(m.x - x) < 10) {
//                        xs.set(i, (x + m.x) / 2);
//                        done = true;
//                        if (y_min.get(i) > m.y) {
//                            y_min.set(i, m.y);
//                        }
//                        break;
//                    }
//                }
//                if (!done) {
//                    xs.add(m.x);
//                    y_min.add(m.y);
//                }
//                if (y_max < m.y) {
//                    y_max = m.y;
//                }
//            }
//            Log.d("List uneliminated", xs.toString() + " " + xs.size());
//            Log.d("Y min", y_min.toString());
//            Log.d("Y max", y_max + "");
//        }
//
//        DecimalFormat df = new DecimalFormat("#.00");
//        for (int i = 0; i < xs.size(); i++) {
//            int x0 = xs.get(i) - r;
//            int y0 = y_min.get(i) - r;
//            int x1 = x0 + w;
//            int y1 = y_max + h;
//            boxes.add(new Dimension(x0, y0, x1, y1, Color.WHITE));
////            g2.setColor(Color.WHITE);
////            g2.drawLine(x0, y0, x1, y0);
////            g2.drawLine(x1, y0, x1, y1);
////            g2.drawLine(x1, y1, x0, y1);
////            g2.drawLine(x0, y1, x0, y0);
////            String angleFormated = df.format((y_max + h - y_min.get(i)) / (double) (y_max + h) * 100);
////            g2.drawString(angleFormated + "%", xs.get(i), y_min.get(i) - 10);
//        }
//        return boxes;
//    }
//}
//
