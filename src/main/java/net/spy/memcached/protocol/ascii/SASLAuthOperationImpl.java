package net.spy.memcached.protocol.ascii;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.security.sasl.SaslClient;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.OperationType;
import net.spy.memcached.ops.SASLAuthOperation;
import net.spy.memcached.ops.SASLStepOperation;
import net.spy.memcached.ops.StatusCode;

public class SASLAuthOperationImpl extends OperationImpl
        implements SASLAuthOperation, SASLStepOperation {

  private byte[] data = null;
  private int readOffset = 0;
  private byte lookingFor = '\0';

  protected final byte[] challenge;
  protected final SaslClient sc;

  public SASLAuthOperationImpl(SaslClient sc, byte[] ch,
                               OperationCallback cb) {
    super(cb);
    challenge = ch;
    this.sc = sc;
    setOperationType(OperationType.READ);
  }

  @Override
  public final void handleLine(String line) {
    if (line.equals("END")) {
      String msg = "";
      if (data != null) {
        msg = new String(data);
      }
      getCallback().receivedStatus(new OperationStatus(true,
              msg, StatusCode.SUCCESS));
      transitionState(OperationState.COMPLETE);
      data = null;
    } else if (line.startsWith("VALUE ")) {
      String[] stuff = line.split(" ");
      data = new byte[Integer.parseInt(stuff[3])];
      readOffset = 0;
      setReadType(OperationReadType.DATA);
    }
  }

  @Override
  public final void handleRead(ByteBuffer bb) {
    if (lookingFor == '\0') {
      int toRead = data.length - readOffset;
      int available = bb.remaining();
      toRead = Math.min(toRead, available);
      bb.get(data, readOffset, toRead);
      readOffset += toRead;
    }

    if (readOffset == data.length && lookingFor == '\0') {
      lookingFor = '\r';
    }

    if (lookingFor != '\0' && bb.hasRemaining()) {
      do {
        byte tmp = bb.get();
        switch (lookingFor) {
          case '\r':
            lookingFor = '\n';
            break;
          case '\n':
            lookingFor = '\0';
            break;
        }
      } while (lookingFor != '\0' && bb.hasRemaining());

      if (lookingFor == '\0') {
        readOffset = 0;
        setReadType(OperationReadType.LINE);
      }
    }
  }

  @Override
  public void initialize() {
    StringBuilder commandBuilder = new StringBuilder();

    byte[] clientData = null;
    try {
      clientData = sc.evaluateChallenge(challenge);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    commandBuilder.append("sasl auth");
    if (challenge == null) {
      commandBuilder.append(' ');
      commandBuilder.append(sc.getMechanismName());
    }
    commandBuilder.append(' ');
    commandBuilder.append(clientData.length);
    commandBuilder.append("\r\n");
    commandBuilder.append(new String(clientData));
    commandBuilder.append("\r\n");

    String commandStr = commandBuilder.toString();
    byte[] commandLine = commandStr.getBytes();
    int size = commandLine.length;
    ByteBuffer bb = ByteBuffer.allocate(size);
    bb.put(commandLine);
    ((Buffer) bb).flip();
    setBuffer(bb);
  }

  @Override
  public boolean isBulkOperation() {
    return false;
  }

  @Override
  public boolean isPipeOperation() {
    return false;
  }

  @Override
  public boolean isIdempotentOperation() {
    return false;
  }
}
