# SPARK: Yelp dataset sentiment analysis

## Project structure

Task is located in a **separate runnable java class** and all associted code is located in the `.java` files which will be explained. Dataset files and helper text files are placed in the `resources` folder and loaded locally.

The location of **output files** is the `outputs/` folder. Output files are named accordingly to the coresponding task.

```
/
  src/
    ...
    helpers/
        DatasetUtils.java
        OutputUtils.java
	Schemas.java
    <default_package>/
        Main.java
        Task01RowCount.java
        Task02ReviewTable.java
        Task03BusinessTable.java
        Task04GraphTable.java
        Task05DBase.java
        Task06DBase.java
  target/
  outputs/
  .cache-main
  .classpath
  .project
  pom.xml
```

For example, the class which could be found in `Main.java` operates everything explained in the subtasks. All other task-related classes are in the `<default_package>`, on the other hand the `helpers` package has three utility classes with shared behaviours.

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

In this report only signatures of mentioned methods are shown. Source code could be inspected for a detailed look.

```java
package helpers;

public class DatasetUtils {
	
	private static DatasetUtils single_instance = null;
	
	public static DatasetUtils getInstance();
	
	public static String decodeBase64(String bytes);
	
	public static Long ExtractYear(String stamp);
	
	public static String ExtractTimestamp(String stamp);
	
	public static String ExtractDate(Long timestamp);
	
	public static Double IteratorAverage(Iterable<Long> iter);
	
	public static Tuple2<Double, Double> IteratorGeographicalCentroid(Iterable<Tuple2<Double, Double>> iter);
	
	public static Function2<Integer, Iterator<String>, Iterator<String>> RemoveHeader = new Function2<Integer, Iterator<String>, Iterator<String>>();
	
	public static FlatMapFunction<String, String> RemoveSpaces = new FlatMapFunction<String, String>();
}
```

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
