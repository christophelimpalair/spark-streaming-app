import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds,StreamingContext}

object StreamingExampleOne {
	def main(args: Array[String]) {
		// Change Log Level
		AdjustLogLevel.setStreamingLogLevels()
		// create a local StreamingContext with 2 working
		// threads and batch intervals of 5 seconds
		// master requires 2 cores to prevent from starvation
		val conf = new SparkConf().setMaster("local[2]").setAppName("streaming example one")
		// streaming context is the main entry point for streaming functionality        
		val ssc =  new StreamingContext(conf, Seconds(1))
        // using this context, we can create DStream that represents the data from a TCP source
        //DStreams
        val lines = ssc.socketTextStream("localhost", 9999) // IP, Port
        // split each line into words
        // words DStream
        val words = lines.flatMap(_.split(" "))
        // Counts each word in each batch
        // key is word, value is 1
        val pairs = words.map(word => (word, 1))
        // count each word in each batch
        val wordCounts = pairs.reduceByKey(_ + _)
        // print first ten elements of each RDD generated
        // in this DStream to the console.
        // wordCounts.print()
        // 
        ssc.checkpoint("./checkpoint/")
        // We want to maintain a running count of each word seen in a text data stream
        // running count is state, it is true integer.
        // The updateFunction will be called for each word, with newValues having a
        // sequence of 1's from the (word, 1)
        // and runningCount having the previous count
        val updateFunction = (values: Seq[Int], state: Option[Int]) => {
        	val currentCount = values.sum
        	val previousCount = state.getOrElse(0)
        	Some(currentCount + previousCount)
        }

        // update the cumulative count
        // This will give a DStream made of state (which is the cumulative count of words)
        val runningCounts = pairs.updateStateByKey[Int](updateFunction)
        // runningCounts.print()
        
        // Generate word counts over the last 30 seconds of data
        // every 10 seconds 
        val windowWordCounts = pairs.reduceByKeyAndWindow((a: Int,b: Int) => (a+b), Seconds(30), Seconds(10))
        windowWordCounts.print()
        // When these lines are executed, Spark Streaming
        // only set up the computation it will perform when 
        // it is started, and no real processing has started yet
        // To start the processing after all the transformations
        // have been set up, we finally call
        ssc.start() // start computation
        ssc.awaitTermination() // wait for the computation to terminate
    }
}

