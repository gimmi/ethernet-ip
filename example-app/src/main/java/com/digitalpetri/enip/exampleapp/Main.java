package com.digitalpetri.enip.exampleapp;

import com.digitalpetri.enip.EtherNetIpClient;
import com.digitalpetri.enip.EtherNetIpClientConfig;
import com.digitalpetri.enip.cip.CipClient;
import com.digitalpetri.enip.cip.CipConnectionPool;
import com.digitalpetri.enip.cip.epath.DataSegment;
import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment;
import com.digitalpetri.enip.cip.epath.PortSegment;
import com.digitalpetri.enip.cip.services.GetAttributeListService;
import com.digitalpetri.enip.logix.services.ReadTagService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCountUtil;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        EtherNetIpClientConfig config = EtherNetIpClientConfig.builder("192.168.1.140")
                .setSerialNumber(0x00)
                .setVendorId(0x00)
                .setTimeout(Duration.ofSeconds(2))
                .build();

/*
        cipServiceExample(config);
        etherNetIpExample(config);
*/

        logixReadTag(config);
    }

    private static void logixReadTag(EtherNetIpClientConfig config) throws InterruptedException, ExecutionException {
        EPath.PaddedEPath connectionPath = new EPath.PaddedEPath(new PortSegment(1, new byte[]{(byte) 0}));

        CipClient client = new CipClient(config, connectionPath);

        client.connect().get();

        CipConnectionPool pool = new CipConnectionPool(2, client, connectionPath, 500);

        // the tag we'll use as an example
        EPath.PaddedEPath requestPath = new EPath.PaddedEPath(new DataSegment.AnsiDataSegment("ArrayOfFloat"));

        ReadTagService service = new ReadTagService(requestPath);

        pool.acquire().whenComplete((connection, ex) -> {
            if (connection != null) {
                CompletableFuture<ByteBuf> f = client.invokeConnected(connection.getO2tConnectionId(), service);

                f.whenComplete((data, ex2) -> {
                    if (data != null) {
                        System.out.println("Tag data: " + ByteBufUtil.hexDump(data));
                    } else {
                        ex2.printStackTrace();
                    }
                    pool.release(connection);
                });
            } else {
                ex.printStackTrace();
            }
        });
    }

    private static void cipServiceExample(EtherNetIpClientConfig config) throws InterruptedException, ExecutionException {
        // backplane, slot 0
        EPath.PaddedEPath connectionPath = new EPath.PaddedEPath(new PortSegment(1, new byte[]{(byte) 0}));

        CipClient client = new CipClient(config, connectionPath);

        client.connect().get();

        GetAttributeListService service = new GetAttributeListService(
                new EPath.PaddedEPath(new LogicalSegment.ClassId(0x01), new LogicalSegment.InstanceId(0x01)),
                new int[]{4},
                new int[]{2}
        );

        client.invokeUnconnected(service).whenComplete((as, ex) -> {
            if (as != null) {
                try {
                    ByteBuf data = as[0].getData();
                    int major = data.readUnsignedByte();
                    int minor = data.readUnsignedByte();

                    System.out.println(String.format("firmware v%s.%s", major, minor));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    Arrays.stream(as).forEach(a -> ReferenceCountUtil.release(a.getData()));
                }
            } else {
                ex.printStackTrace();
            }
        });
    }

    private static void etherNetIpExample(EtherNetIpClientConfig config) throws InterruptedException, ExecutionException {

        EtherNetIpClient client = new EtherNetIpClient(config);

        client.connect().get();

        client.listIdentity().whenComplete((li, ex) -> {
            if (li != null) {
                li.getIdentity().ifPresent(id -> {
                    System.out.println("productName=" + id.getProductName());
                    System.out.println("revisionMajor=" + id.getRevisionMajor());
                    System.out.println("revisionMinor=" + id.getRevisionMinor());
                });
            } else {
                ex.printStackTrace();
            }
        });
    }
}
