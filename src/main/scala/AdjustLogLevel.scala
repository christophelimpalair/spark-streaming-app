//Last updated 7/24/2015
import org.apache.spark.Logging
import org.apache.log4j.{Level, Logger}

/** Utility functions to adust Log level. */
object AdjustLogLevel extends Logging {

  /** Set reasonable logging levels for streaming if the user has not configured log4j. */
  def setStreamingLogLevels() {
    val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
    if (!log4jInitialized) {
      // We first log something to initialize Spark's default logging, then we override the
      // logging level.
      logInfo("Setting log level to [ERROR] for streaming example." +
        " To override add a custom log4j.properties to the classpath.")
      //Level.WARN

      Logger.getRootLogger.setLevel(Level.ERROR)
    }
  }
}
