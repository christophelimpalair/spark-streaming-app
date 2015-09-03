### Major steps for streaming app
1. Define the input sources by creating input DStreams
2. Define the streaming computations by applying transformation and output operations to DStream
3. Start receiving data and processing it using StreamingContext.start()

#### Discretized Streams (DStreams)
Represented by a continuous series of RDD, is Spark's abstraction of immutable, distributed dataset. Each RDD in a DStream contains data from a certain interval.

Every input stream (DStream) is associated with a Receiver object which receives the data from a source and stores it in Spark's memory for processing.

#### UpdateStateByKey operation
Maintain arbitrary state while continuously updating it with new info. To use this, you will have to do two steps.

1) Define the state - The state can be of arbitrary data type.
2) Define the state update function - Specify with a function how to update the state using the previous state and the new values from input stream.