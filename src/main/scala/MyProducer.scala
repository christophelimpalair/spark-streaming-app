import java.io.PrintWriter
import java.net.ServerSocket
import java.text.{SimpleDateFormat, DateFormat}
import java.util.Date
import org.apache.spark.SparkContext._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import scala.util.Random

// our producer needs to create a network connection
// and generate random purchase event data to send over this connection
object MyProducer {
	def main(args: Array[String]) {
		val random = new Random()
		// Maximum number of events per second
		val MaxEvents = 6
		// read a list of possible names
		val namesResource = this.getClass.getResourceAsStream("/names.csv")

		val names = scala.io.Source.fromInputStream(namesResource).getLines().toList.head.split(",").toSeq

		// generate a sequence of possible products
		val products = Seq(
			"iphone Cover" -> 9.99,
			"Headphones" -> 5.49,
			"Samsung Galaxy Cover" -> 8.95,
			"ipad Cover" -> 7.49
		)

		// Using the list of names and map of product name to price,
		// we will create a function that will randomly pick a product and name from these sources
		// and generate a specified number of product events
		def generateProductEvents(n: Int) = {
			(1 to n).map { i =>
				val (product, price) = products(random.nextInt(products.size))
				val user = random.shuffle(names).head
				(user, product, price)
			}
		}

		// create a network producer
		val listener = new ServerSocket(9999)
		println("Listening on port: 9999")
		while(true) {
			val socket = listener.accept()
			new Thread() {
				override def run = {
					println("Got client connected from: " + socket.getInetAddress)
					val out = new PrintWriter(socket.getOutputStream(), true)
					while(true) {
						// one second
						Thread.sleep(1000)
						val num = random.nextInt(MaxEvents)
						val productEvents = generateProductEvents(num)
						productEvents.foreach { event =>
							// productIterator convert tuple into iterator
							out.write(event.productIterator.mkString(","))
							out.write("\n")
						}
						out.flush()
						println(s"Created $num events..")
					} // end of inner while loop
				socket.close()
				}
			}.start() // thread
		} // end of outer while loop
	}
}

object StreamingAnalyticsApp {
	def main(args: Array[String]) {
		// change log level
		AdjustLogLevel.setStreamingLogLevels()

		val ssc = new StreamingContext("local[2]", "streaming Analytics App", Seconds(10))

		val stream = ssc.socketTextStream("localhost", 9999)

		// print out the first few elements of each batch
		stream.print()

		ssc.start()
		ssc.awaitTermination()

		// We want to compute the following metrics on our stream of purchase events
		// 1) Total number of purchases
		// 2) The number of unique users
		// 3) total revenue
		// 4) Most popular product with its number of purchases and total revenue
		// these metrics will be computer per batch and printed out

		// Create stream of events from raw text elements
		// Make sure event(s) is converted to Double
		val events = stream.map { record =>
			val event = record.split(",")
			(event(0), event(1), event(2).toDouble)
		}

		// Compute and print out stats for each batch. Since each batch is RDD, we call forEachRDD on the DStream
		// ForeachRDD can apply arbitrary processing on each RDD in the stream to compute our desired metrics
		events.forEachRDD { (rdd, time) => 
			val numPurchases = rdd.count()
			val uniqueUsers = rdd.map {
				case (user,_,_) => user 
			}.distinct().count()
			val totalRevenue = rdd.map {
				case (_,_,price) => price.toDouble 
			}.sum()
			val productByPopularity = rdd.map {
				case (user, product, price) => (product, 1)
			}.reduceByKey(_+_)
				.collect()
				.sortBy(-_._2) // sort by value (number of purchases) in reverse order
		}
	}
}