package helpers;

import org.apache.spark.sql.types.StructType;

public class Schemas {

	public static StructType schemaReviewers = new StructType()
			.add("review_id", "string")
		    .add("user_id", "string")
		    .add("business_id", "string")
		    .add("review_text", "string")
		    .add("review_date", "timestamp");
	
}
