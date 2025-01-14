/*
 *  Copyright 2023 The original authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.morling.onebrc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;

import static java.util.stream.Collectors.groupingBy;

public class CalculateAverage_matiasdwek {

    private static final String FILE = "./measurements.txt";

    private record Measurement(String station, double value) {
        private Measurement(String[] parts) {
            this(parts[0], Double.parseDouble(parts[1]));
        }
    }

    private record ResultRow(double min, double mean, double max) {

        public String toString() {
            return round(min) + "/" + round(mean) + "/" + round(max);
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    };

    private static class MeasurementAggregator {
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;
        private double sum;
        private long count;
    }

    private static Map<String, ResultRow> calcMeasurements() throws IOException {
        Collector<Measurement, MeasurementAggregator, ResultRow> collector = Collector.of(
                MeasurementAggregator::new,
                (a, m) -> {
                    a.min = Math.min(a.min, m.value);
                    a.max = Math.max(a.max, m.value);
                    a.sum += m.value;
                    a.count++;
                },
                (agg1, agg2) -> {
                    agg1.min = Math.min(agg1.min, agg2.min);
                    agg1.max = Math.max(agg1.max, agg2.max);
                    agg1.sum = agg1.sum + agg2.sum;
                    agg1.count = agg1.count + agg2.count;
                    return agg1;
                },
                agg -> new ResultRow(agg.min, (Math.round(agg.sum * 10.0) / 10.0) / agg.count, agg.max));

        Map<String, ResultRow> result;
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            var fileStream = reader
                    .lines()
                    .unordered()
                    .parallel();

            result = fileStream
                    .map(l -> new Measurement(l.split(";")))
                    .collect(groupingBy(Measurement::station, collector));
        }

        return new TreeMap<>(result);
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();

        System.out.println(calcMeasurements());
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed Time: " + elapsedTime /1000 + " seconds");
    }
}
