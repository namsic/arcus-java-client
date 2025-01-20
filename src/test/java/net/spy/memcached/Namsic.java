package net.spy.memcached;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.auth.ScramSaslClientProvider;

public class Namsic {
  private static final String[] mechanism = {"SCRAM-SHA-256"};
  private static final String username = "plainuser";
  private static final String password = "plainpw";

  private ArcusClient newClient(boolean useCluster, boolean useBinaryProtocol) throws IOException {
    ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder()
        .setAuthDescriptor(new AuthDescriptor(mechanism, 
        new PlainCallbackHandler(username, password)));
    
    if (useBinaryProtocol) {
      cfb.setProtocol(Protocol.BINARY);
    }

    if (useCluster) {
      return ArcusClient.createArcusClient("ncp-2c4-001:40000", "sample", cfb);
    } else {
      return new ArcusClient(cfb.build(), AddrUtil.getAddresses("127.0.0.1:40010"));
    }

  }

  // mvn test -Dtest=Namsic#testSasl
  @Test
  public void testSasl() throws Exception {
    ScramSaslClientProvider.initialize();

    ArcusClient mc = newClient(true, true);
    Thread.sleep(10000);

    assertTrue(mc.set("namsic:kv01", 30, "value01").get());
    assertEquals("value01", mc.get("namsic:kv01"));
  }
  
  // mvn test -Dtest=Namsic#testAsciiSasl
  @Test
  public void testAsciiSasl() throws Exception {
    ScramSaslClientProvider.initialize();

    ArcusClient mc = newClient(true, false);

    Thread.sleep(10000);
    assertTrue(mc.set("namsic:kv01", 30, "value01").get());
    assertEquals("value01", mc.get("namsic:kv01"));
  }
}
