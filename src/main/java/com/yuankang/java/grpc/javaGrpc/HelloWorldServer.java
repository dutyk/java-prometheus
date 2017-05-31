package com.yuankang.java.grpc.javaGrpc;

import java.io.IOException;
import java.util.logging.Logger;

import com.yuankang.java.grpc.javaGrpc.GreeterGrpc.GreeterBlockingStub;
import com.yuankang.java.jetty.ServletServer;

import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import me.dinowernli.grpc.prometheus.Configuration;
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor;

public class HelloWorldServer {
  private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

  private Server server;
  private int grpcPort = 50051;
  private static final String Name = "HELLO DAVE";
  private static final HelloRequest REQUEST = HelloRequest.newBuilder()
      .setName(Name)
      .build();
  private CollectorRegistry collectorRegistry = new CollectorRegistry();
  private static final Configuration CHEAP_METRICS = Configuration.cheapMetricsOnly();
  
  private void start() throws IOException {
    /* The port on which the server should run */
	MonitoringServerInterceptor interceptor = MonitoringServerInterceptor.create(
			CHEAP_METRICS.withCollectorRegistry(collectorRegistry));
    server = ServerBuilder.forPort(grpcPort)
        .addService(ServerInterceptors.intercept(new GreeterImpl().bindService(),interceptor))
        .build()
        .start();
    logger.info("Server started, listening on " + grpcPort);
    //expose metric using http
    ServletServer server1 = new ServletServer(collectorRegistry);
    try {
		server1.startServer();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
    
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        HelloWorldServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final HelloWorldServer server = new HelloWorldServer();
    server.start();
    //server.createGrpcBlockingStub().sayHello(REQUEST);
    int size1 = server.findRecordedMetricOrThrow("grpc_server_started_total").samples.size();
    int size2 = server.findRecordedMetricOrThrow("grpc_server_msg_received_total").samples.size();
    int size3 = server.findRecordedMetricOrThrow("grpc_server_msg_sent_total").samples.size();
    logger.info("size1="+size1);
    logger.info("size3="+size2);
    logger.info("size3="+size3);

    MetricFamilySamples handled = server.findRecordedMetricOrThrow("grpc_server_handled_total");
    int size4 = handled.samples.size();
    String str = handled.samples.get(0).labelValues.get(0);
    double d = handled.samples.get(0).value;
    logger.info("size4="+size4);
    logger.info(str);
    logger.info("d="+d);

    server.blockUntilShutdown();
  }
  private MetricFamilySamples findRecordedMetricOrThrow(String name) {
	return RegistryHelper.findRecordedMetricOrThrow(name, collectorRegistry);
  }
  private GreeterBlockingStub createGrpcBlockingStub() {
	return GreeterGrpc.newBlockingStub(createGrpcChannel());
  }
  private Channel createGrpcChannel() {
    return NettyChannelBuilder.forAddress("localhost", grpcPort)
    		.negotiationType(NegotiationType.PLAINTEXT)
    		.build();
  }
  static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }
}
