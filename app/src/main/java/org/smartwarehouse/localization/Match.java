///*
// * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
//package org.smartwarehouse.localization;
//
//import georegression.struct.point.Point2D_I32;
//
///**
// * Found match during template matching.  Provides location and fit score.
// *
// * @author Peter Abeles
// */
//public class Match extends Point2D_I32 {
//    /**
//     * Score indicating the match quality.  Higher the score the better.  The range will
//     * depending on the algorithm used.  For some algorithms the score will even be negative.
//     */
//    public double score;
//
//    public Match(int x, int y, double score) {
//        this.x = x;
//        this.y = y;
//        this.score = score;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Match match = (Match) o;
//
//        return Math.abs(match.x - x) < 50 && Math.abs(match.y - y) < 35;
//    }
//
//    @Override
//    public int hashCode() {
//        int result = 17;
//        result = 31 * result + x;
//        result = 31 * result + y;
//        return result;
//    }
//
//    public Match() {
//    }
//
//    @Override
//    public String toString() {
//        return "Match{x=" + x + ",y=" + y + ",score=" + score + "}";
//    }
//
//}
