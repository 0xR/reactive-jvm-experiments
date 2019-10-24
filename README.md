Set of experiments showcasing backpressure using different Reactive technology on the JVM.

# Running Reactor example

Start the main function in `reactor-experiment/src/main/kotlin/Application.kt`.
A TCP server should have started at port `8080` which throttles consumption and aggregates input.

You can test the effects of backpressure on this stream with the following command line utilities `yes`, `nc` and `pv` (they should be available on brew if on OS X).

If you pipe the output of `yes` through `pv` to netcat you can see at which rate the pipe is sending through data:

```bash
yes | pv | nc localhost 8080
```

Now you can play around with the throttling on the JVM side, by for instance by changing the `Flux.interval(200L)`. When running the pipe again you will see the jvm will consume at a higher rate.
