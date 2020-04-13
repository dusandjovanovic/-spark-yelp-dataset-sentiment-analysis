import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.util.DoubleAccumulator;

import helpers.DatasetUtils;
import helpers.OutputUtils;
import scala.Tuple2;
import scala.Option;

public class Subtask01PolarityByReview {
	
	private static String appName = "Subtask01PolarityByReview";
	
	private static String uriReviewers = "src/main/resources/yelp_top_reviewers_with_reviews.csv";
	
	private static String output = "./output-01.csv";

	public static void main(String[] args) throws Exception {
		SparkConf config = new SparkConf().setAppName(appName).setMaster("local[*]");
		SparkContext sparkContext = new SparkContext(config);
		JavaSparkContext context = new JavaSparkContext(sparkContext);
		
		JavaRDD<String> rddReviewers = context.textFile(uriReviewers);
		
		JavaRDD<String> rddReviewersNoHeader = rddReviewers
				.mapPartitionsWithIndex(DatasetUtils.RemoveHeader, false);
		
		Double rddReviewersAvarage = null;
		
		JavaRDD<Double> rddReviewersReviewText = rddReviewersNoHeader
				.map(row -> Double.valueOf(DatasetUtils.decodeBase64(row.split("	")[3]).length()));
		rddReviewersAvarage = rddReviewersReviewText.reduce((accum, n) -> (accum + n));
		rddReviewersAvarage /= rddReviewersReviewText.count();
		
		OutputUtils.writerInit(output);
        OutputUtils.writeLine(Arrays.asList(rddReviewersAvarage.toString()));
        OutputUtils.writerCleanup();
		
		cleanup(context);
	}

	private static void cleanup(JavaSparkContext context) {
		context.close();
	}
	
	private static void debugRDD(JavaRDD<String> rdd) { 
		for(String line:rdd.collect())
            System.out.println(line);
	}
	
	private static void debugPairRDD(JavaPairRDD<String, Long> rdd) {
		rdd.foreach(data -> {
	        System.out.println(data._1() + " " + data._2());
	    }); 
	}
}
