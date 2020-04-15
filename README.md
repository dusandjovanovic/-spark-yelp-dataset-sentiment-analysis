# SPARK: Yelp dataset sentiment analysis

## Project structure

Task is located in a **separate runnable java class** and all associted code is located in the `.java` files which will be explained. Dataset files and helper text files are placed in the `resources` folder and loaded locally.

`Subtask01PolarityByReview.java` and `Subtask02TopBusinesses.java` are runnable classes for outputing review poalrities and top-k businesses with gathered polaraty values in mind. In the end, the class named `Main.java` incorporates both subtasks from separate classes into one job.

The location of output files is the outputs/ folder. Output folders are named accordingly to the coresponding subtask such where `output-02` is the output folder of the Second subtask and `output-main-topK` is the output folder of the same subtask, just outputed from the shared class.

```
/
  src/
    ...
    helpers/
        DatasetUtils.java
        OutputUtils.java
	InputUtils.java
	Schemas.java
    <default_package>/
        Main.java
        Subtask01PolarityByReview.java
        Subtask02TopBusinesses.java
  target/
  outputs/
  .cache-main
  .classpath
  .project
  pom.xml
```

The class which could be found in `Main.java` operates everything explained in the subtasks, whereas a class named `Subtask01PolarityByReview.java` represents just a subtask portion of the required analysis. All other task-related classes are in the `<default_package>`, on the other hand the `helpers` package has three utility classes with shared behaviours.

```diff
- Important* resource files are not present on this repository since they are 1GB+ in size!

+ All dataset resources should be imported in the /resources folder.
```

## Shared behaviours and classes

`DatasetUtils` class has several shared and isolated methods such as:
1) `String decodeBase64` - decoding base64 strings
2) `Long ExtractYear` - extracting year from timestamp
3) `String ExtractTimestamp` - extracting and trimming timestamp strings
4) `String ExtractDate` - extracting date object from timestamp
5) `Double IteratorAverage` - finding the average value from an Iterator
6) `Tuple2<Double, Double> IteratorGeographicalCentroid` - finding the centroid location
7) `Function2<Integer, Iterator<String>, Iterator<String>> RemoveHeader` - clearing header rows from .csv files
8) `FlatMapFunction<String, String> RemoveSpaces` - clearing spaces in strings
9) `String[] ExtractAndPreprocess` - **tokenization of reviews** with extraction and preprocessing
10) `Integer IteratorSentiment` - **finding the accumulated sentiment** of one review 

In this report only signatures of mentioned methods are shown. Source code could be inspected for a detailed look.

```java
package helpers;

public class DatasetUtils {
	
	private static DatasetUtils single_instance = null;
	
	public static DatasetUtils getInstance();
	
	public static String decodeBase64(String bytes);
	
	public static String[] ExtractAndPreprocess(String bytes, Map<String, Integer> stopwords);
	
	public static Long ExtractYear(String stamp);
	
	public static String ExtractTimestamp(String stamp);
	
	public static String ExtractDate(Long timestamp);
	
	public static Double IteratorAverage(Iterable<Long> iter);
	
	public static Integer IteratorSentiment(String[] iter, Map<String, Integer> sentimentMap);
	
	public static Tuple2<Double, Double> IteratorGeographicalCentroid(Iterable<Tuple2<Double, Double>> iter);
	
	public static Function2<Integer, Iterator<String>, Iterator<String>> RemoveHeader = new Function2<Integer, Iterator<String>, Iterator<String>>();
	
	public static FlatMapFunction<String, String> RemoveSpaces = new FlatMapFunction<String, String>();
	
}
```

```java
	public static String decodeBase64(String bytes) {
		String byteString = new String(Base64.decodeBase64(bytes.getBytes()));
		return byteString;
	}
	
	public static String[] ExtractAndPreprocess(String bytes, Map<String, Integer> stopwords) {
		String review = decodeBase64(bytes);
		String[] tokenized = review
			.replaceAll("[^a-zA-Z ]", "")
			.toLowerCase()
			.split("\\s+");
		
		return Arrays.stream(tokenized)
			.filter(word -> word.length() != 1 && !stopwords.containsKey(word))
			.toArray(String[]::new);
	}

	public static Integer IteratorSentiment(String[] iter, Map<String, Integer> sentimentMap) {
		Integer _result = new Integer(0);
		
		for(String word : iter)
			if (sentimentMap.containsKey(word))
				_result += sentimentMap.get(word);
			else continue;
		
       		return _result;
	}
```

In a shred `DatasetUtils.java` class three new important methods are being introduced. `decodeBase64` and `ExtractAndPreprocess` are used to extract tokenized and preprocessed words from the dataset. As we can see, the bytes are first decoded, then filtered and split, and lastly loaded `stopwords` dictionary is being used to remove unnecessary words. The third method named `IteratorSentiment` is used to calculate sentiment values of every review - based upon separate sentiment values of each word. A collection of tokenized words is iterable and thereby should be traversed for every review text separately. All three __methods are being initiated from a parallelized RDD processing__.

Similarly, the `OutputUtils` class encapsulates methods for writing and comosing .csv files as output. Important methods are:
1) `void writerInit` - opening a new file-stream by given path
2) `void writerCleanup` - closing the stream
3) `void writeLine` - writing a new line to the file

```java
package helpers;

public class OutputUtils {
    
    public static void writerInit(String output) throws IOException;
    
    public static void writerCleanup() throws IOException;
    
    public static void writeLine(List<String> values) throws IOException;

    public static void writeLine(List<String> values, char separators) throws IOException;

    private static String followCVSformat(String value);

    public static void writeLine(List<String> values, char separators, char customQuote) throws IOException;
}
```

Newly introduced class comapred to the first part of the project, `InputUtils` has methods for reading from text files into maps. These methods are being used to load stopwords and sentiment statistics. Important methods are:
1) `String[] readLinesFromTextFile` - reading separated lines
2) `Map<String, Integer> readLinesToMap` - reading lines into a basic map
3) `Map<String, Integer> readLinesToDictionary` - reading lines into a map with concrete values (used for **sentiment data realted to a certain word**)


```java
package helpers;
...

public class InputUtils {
	
	private static String DELIMITER = "	";
	private static Integer STOPWORD_PRESENT = 1;
	
	public static String[] readLinesFromTextFile(String filename);
	
	public static Map<String, Integer> readLinesToMap(String filename);
	
	public static Map<String, Integer> readLinesToDictionary(String filename);
}
```

Lastly, `Schemas` is a static class holding database `StructType` schemas used in last two tasks.

```java
package helpers;

import org.apache.spark.sql.types.StructType;

public class Schemas {
	
	public static StructType schemaReviewers = new StructType()
	    .add("review_id", "string")
	    .add("user_id", "string")
	    .add("business_id", "string")
	    ...
	
}
```

## Subtask 01 - Sentiment analysis per user-review

This solution is presented in the class `Subtask01PolarityByReview.java`. In order to begin this data analysis it is neccessary to load both stopwords and sentiment information for certain words. Dictionaries in a form of a `hashed maps` are being used for both.

```java
	Map<String, Integer> stopwordsMap = InputUtils.readLinesToMap(uriStopwrods);
	Map<String, Integer> sentimentMap = InputUtils.readLinesToDictionary(uriAFINN);

	JavaRDD<String> rddReviews = context.textFile(uriReviewers);

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
```

Afterwards, `JavaPairRDD` is formed representing first three columns for reviews which are `review_id`, `user_id` and `business_id` along with a newly **preprocessed and tokenized review text** with the help from `DatasetUtils.ExtractAndPreprocess` which has been explained previously.

**Performing sentiment analysis** is the next in line and is based on tokenized arrays of extracted words. It includes mapping over every element (review sample with added data) with a shared method `DatasetUtils.IteratorSentiment`. This method will summarise sentiment values for all words in a certain review and thereby give an approximation of review's sentiment score.

Last step involves repartitioning and the resulting RDD being written to an output file. 

The result can be found in the directory `output-01`. An excerpt from the output data is shown below. We can see that the second value in the pair represent an integer as a result of previous **sentiment analysis**, whereas the first value is a tuple in the form of (`review_id`, `user_id`, `business_id`). For example, for a review with an id of `-lFvxYOmAuZMOelAs0dwgw` and a corespoinding business with an id of `XJGMgs9Kh4kcgf8Oskiewg` the review's sentiment value is calculated to be `18`

```json
	([-lFvxYOmAuZMOelAs0dwgw, ---1lKK3aKOuomHnwAkAow, XJGMgs9Kh4kcgf8Oskiewg],18)
	([-nyKSlK-acm7Tkuobbw3MA, ---1lKK3aKOuomHnwAkAow, cHuA0Yb5oYwx1lrNVABqdQ],10)
	([-pk4s5YUD0grEEBt2QYlDA, ---1lKK3aKOuomHnwAkAow, bPcqucuuClxYrIM8xWoArg],8)
	([-UtICN8nUQ4g9qIHlQRrxw, ---1lKK3aKOuomHnwAkAow, rq5dgoksPHkJwJNQKlGQ7w],19)
	(["0cdjRebZLHYu-xyMSFKgMQ", ---1lKK3aKOuomHnwAkAow, R-McIj4Psxl1VlEacsXeRg],1)
	(["0hS9a57nL2qBTWoZCJBB2A", ---1lKK3aKOuomHnwAkAow, Vg1C_1eqwIwkZLIXGMTW3g],-2)
	(["12_4xbZupkMox3adrUCwwA", ---1lKK3aKOuomHnwAkAow, "5cbsjFtrntUAeUx51FaFTg"],6)
	(["1ikB-TEgwg2gigixDEDSuA", ---1lKK3aKOuomHnwAkAow, kosTPb88O4Q0XGbVbEOGCA],6)
	(["1PJpo48hSChCbriXEHGSjw", ---1lKK3aKOuomHnwAkAow, yp2nRId4v-bDtrYl5A3F-g],-5)
	(["23MKMYyMrw7mRrNlC2hwBA", ---1lKK3aKOuomHnwAkAow, sZsJooAzpKqOvDysphkqpQ],7)
	(["2AXKTUbIkwuP8wcvzSt7Tg", ---1lKK3aKOuomHnwAkAow, hubbaEcYPYEZu5Ziz6i0lw],-2)
	(["2D3lifCSaaKLr73PK27eyg", ---1lKK3aKOuomHnwAkAow, slVkMoNTCGI2rOhMaL5u5A],1)
	(["3cCBqmhi0ldJR31k5XYX6g", ---1lKK3aKOuomHnwAkAow, YbKjkJCD3lcQcLSMNKglKg],9)
	(["3R2e-knpN5lCHu2LVk6hsQ", ---1lKK3aKOuomHnwAkAow, "5aeR9KcboZmhDZlFscnYRA"],9)
...
```

## Subtask 02 - Top K businesses based on user-review sentiment

This solution is presented in the class `Subtask02TopBusinesses.java`. In order to begin this data analysis it is neccessary to load both stopwords and sentiment information for certain words. Dictionaries in a form of a `hashed maps` are being used for both.

```java
	Map<String, Integer> stopwordsMap = InputUtils.readLinesToMap(uriStopwrods);
	Map<String, Integer> sentimentMap = InputUtils.readLinesToDictionary(uriAFINN);

	JavaRDD<String> rddReviews = context.textFile(uriReviewsers);

	JavaRDD<String> rddReviewsNoHeader = rddReviews
		.mapPartitionsWithIndex(DatasetUtils.RemoveHeader, false);

	JavaPairRDD<String[], String[]> rddReviewsText = rddReviewsNoHeader
		.mapToPair(row -> new Tuple2<String[], String[]>(
				Arrays.copyOfRange(row.split("	"), 0, 3),
				DatasetUtils.ExtractAndPreprocess(row.split("	")[3], stopwordsMap)
			));

	JavaPairRDD<String, Integer> rddReviewsTextAffinity = rddReviewsText
		.mapToPair(row -> new Tuple2<String, Integer>(
				row._1[2],
				DatasetUtils.IteratorSentiment(row._2, sentimentMap)
			));

	JavaPairRDD<Integer, String> rddReviewsTextAffinityByBusinesses = rddReviewsTextAffinity
		.reduceByKey((a, b) -> a + b)
		.mapToPair(row -> new Tuple2<Integer, String>(row._2, row._1))
		.sortByKey(false);

	JavaRDD<Tuple2<Integer,String>> rddReviewsTextAffinityByBusinessesTopK = context
		.parallelize(rddReviewsTextAffinityByBusinesses.take(K));

	rddReviewsTextAffinityByBusinessesTopK
		.repartition(1)
		.saveAsTextFile(output);

```

Similarly like in a case of a previous subtask, `JavaPairRDD` is formed representing first three columns for reviews which are `review_id`, `user_id` and `business_id` and a newly preprocessed and tokenized review text with the help from `DatasetUtils.ExtractAndPreprocess`. In a similar fashion, mapping is done for every element with a shared method `DatasetUtils.IteratorSentiment` which will summarise sentiment values for all words in a certain review.

Important information for this task is the `bussines_id` column of every review record together with the computed **`sentiment value`** associated to each and single review in the previous step. Another mapping to a `JavaPairRDD<Integer, String>` RDD is introduced which relies on the `Tuple2` object for every entry. __Reduction__ by key is used to **accumulate review sentiment values coming from the same business source** - identified by the `bussines_id` values. Lastly, sorting the resulting data is neccessary - whole RDD collection is sorted in descending order by accumulated sentiment value.

Last step involves repartitioning and the resulting RDD being written to an output file. Since only __top-k__ values are needed they are being extracted first, before outputting any records.

The result can be found in the directory `output-02`. Complete output data is shown below in the case of 'K = 10'. The first value in the pair represent a number as a result of an **accumulated sentiment analysis** for each business - based on an `business_id`. Top-K values are being extracted and shown in descending order where the second value in each pair is the mentioned `business_id`.

```json
	(7395,"4JNXUYY8wbaaDmk3BPzlWw")
	(6622,RESDUcs7fIiihp38-d6_6g)
	(5910,igHYkXZMLAc9UdV5VnR_AA)
	(5279,A5Rkh7UymKm0_Rxm9K2PJw)
	(5265,k1QpHAkzKTrFYfk6u--VgQ)
	(5211,"5LNZ67Yw9RD6nf4_UhXOjw")
	(5038,IMLrj2klosTFvPRLv56cng)
	(4924,"7sPNbCx7vGAaH7SbNPZ6oA")
	(4896,z6-reuC5BYf_Rth9gMBfgQ)
	(4892,PVTfzxu7of57zo1jZwEzkg)
```

## Jointed subtasks in the same runnable class

Both previously explained subtasks have been joined in a single runnable class called `Main.java`. Approaches to both subtasks have the analogy as explained in the previous two segmentes except that it unites both operations and __makes outputs for both analysis steps__. 

The output folders are named `output-main-reviews` and `output-main-topK`. Output files have small modifications compared to outputted records from previously explained data outputs. Similarly, repartitioning is done before each output and outputs have been separated in the lifeline of the class.

### Runnable JAR

The runnable `JAR` has been extracted and available on the repository as `SentimentAnalysys.jar`. 

`/data` directory should be placed in the same folder on filesystem before running the executable. In this directory the only neccessary file is the dataset file named `yelp_top_reviewers_with_reviews.csv`. Both `AFINN-111.txt` and `stopwords.txt` are used as internal resources and are pre-bundled.

One notable difference compared to previous subtask classes is that the `top-k` analysis takes an argument from the **command line** as the number of *__k elements__ to be shown on output*.

After supplying the neccessary data, the application can be run with the following command and the output directories will be created afterwards.

`$ java -jar SentimentAnalysys.jar [K]` in a general form.

`$ java -jar SentimentAnalysys.jar 10` is the command where top-10 businesses will be shown.
