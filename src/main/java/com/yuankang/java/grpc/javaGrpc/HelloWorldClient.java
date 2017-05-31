package com.yuankang.java.grpc.javaGrpc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import me.dinowernli.grpc.prometheus.MonitoringClientInterceptor;
import me.dinowernli.grpc.prometheus.Configuration;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Collector;

public class HelloWorldClient {
  private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;
  private static final Configuration CHEAP_METRICS = Configuration.cheapMetricsOnly();
  public static CollectorRegistry collectorRegistry = new CollectorRegistry();	
  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  public HelloWorldClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext(true)
        .intercept(MonitoringClientInterceptor.create(
        		CHEAP_METRICS.withCollectorRegistry(collectorRegistry))));
  }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  HelloWorldClient(ManagedChannelBuilder<?> channelBuilder) {
    channel = channelBuilder.build();
    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /** Say hello to server. */
  public void greet(String name) {
    logger.info("Will try to greet " + name + " ...");
    HelloRequest request = HelloRequest.newBuilder().setName(name).build();
    HelloReply response;
    try {
      response = blockingStub.sayHello(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Greeting: " + response.getMessage());
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws Exception {
	int grpcPort = 50051;
    HelloWorldClient client = new HelloWorldClient("localhost", grpcPort);
    try {
      /* Access a service running on the local machine on port 50051 */
      String user = "world";
      if (args.length > 0) {
        user = args[0]; /* Use the arg as the name to greet if provided */
      }
      client.greet(user);
      double d = client.extractMetricValue("grpc_client_started_total");
      int size1 = client.findRecordedMetricOrThrow("grpc_client_msg_received_total").samples.size();
      int size2 = client.findRecordedMetricOrThrow("grpc_client_msg_sent_total").samples.size();
      logger.info("d:"+d);
      logger.info("size1:"+size1);
      logger.info("size2:"+size2);
      Collector.MetricFamilySamples handled = client.findRecordedMetricOrThrow("grpc_client_completed");
      int size3 = handled.samples.size();
      String str = handled.samples.get(0).labelValues.get(0);
      double d1 = handled.samples.get(0).value;
      logger.info("size:="+size3);
      logger.info(str);
      logger.info("d1:"+d1);
    } finally {
      client.shutdown();
    }
  }
  private double extractMetricValue(String name) {
    return RegistryHelper.extractMetricValue(name, collectorRegistry);
  }

  private Collector.MetricFamilySamples findRecordedMetricOrThrow(String name) {
    return RegistryHelper.findRecordedMetricOrThrow(name, collectorRegistry);
  }

  private int countSamples(String metricName, String sampleName) {
    return RegistryHelper.countSamples(metricName, sampleName, collectorRegistry);
  }
}
