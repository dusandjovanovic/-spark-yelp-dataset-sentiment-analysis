import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.util.DoubleAccumulator;

import helpers.DatasetUtils;
import helpers.OutputUtils;
import helpers.InputUtils;
import scala.Tuple2;
import scala.Option;

public class Subtask01PolarityByReview {
	
	private static String appName = "Subtask01PolarityByReview";
	
	private static String uriReviewers = "data/yelp_top_reviewers_with_reviews.csv";
	private static String uriStopwrods = "src/main/resources/stopwords.txt";
	private static String uriAFINN = "src/main/resources/AFINN-111.txt";
	
	private static String output = "./output-01";

	public static void main(String[] args) throws Exception {
		SparkConf config = new SparkConf().setAppName(appName).setMaster("local[*]");
		SparkContext sparkContext = new SparkContext(config);
		JavaSparkContext context = new JavaSparkContext(sparkContext);
		
		Map<String, Integer> stopwordsMap = InputUtils.readLinesToMap(uriStopwrods);
		Map<String, Integer> sentimentMap = InputUtils.readLinesToDictionary(uriAFINN);
		
		JavaRDD<String> rddReviews = context.textFile(uriReviewers);
		JavaRDD<String> rddAfinn = context.textFile(uriAFINN);
		
		JavaRDD<String> rddReviewsNoHeader = rddReviews
				.mapPartitionsWithIndex(DatasetUtils.RemoveHeader, false);
		
		JavaPairRDD<String[], String[]> rddReviewsText = rddReviewsNoHeader
				.mapToPair(row -> new Tuple2<String[], String[]>(
						Arrays.copyOfRange(row.split("	"), 0, 3),
						DatasetUtils.ExtractAndPreprocess(row.split("	")[3], stopwordsMap)
					));
		
		JavaPairRDD<String, Integer> rddReviewsTextAffinity = rddReviewsText
				.mapToPair(row -> new Tuple2<String, Integer>(
						Arrays.toString(row._1),
						DatasetUtils.IteratorSentiment(row._2, sentimentMap)
					));
		
		rddReviewsTextAffinity
			.repartition(1)
			.saveAsTextFile(output);

		cleanup(context);
	}

	private static void cleanup(JavaSparkContext context) {
		context.close();
	}
	
	private static void debugRDD(JavaRDD<String> rdd) { 
		for(String line:rdd.collect())
            System.out.println(line);
	}
	
	private static void debugPairRDD(JavaPairRDD<String, Integer> rdd) {
		rdd.foreach(data -> {
	        System.out.println(data._1() + " " + data._2());
	    }); 
	}
	
	private static void debugPairRDD(List<Tuple2<String, String>> rdd) {
		for(Tuple2<String, String> line:rdd) {
			System.out.println(line);
			System.out.println("***");
		}
	}
}
