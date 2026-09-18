package wtf.opal.client.socket.packet.impl.s2c;

import wtf.opal.client.ReleaseInfo;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.impl.c2s.C2SKeepAlivePacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.Callables;
import wtf.opal.utility.socket.buffer.BufferReader;

@NativeInclude
public final class S2CKeepAlivePacket implements S2CPacket {

    private final BufferReader reader;

    public S2CKeepAlivePacket(final BufferReader reader) {
        this.reader = reader;
    }

    @Override
    public void handle() throws Exception {
        final ClientSocket socket = ClientSocket.getInstance();
        final int startConnectionCount = ClientSocket.getConnectionCount();

        final int recvServerIdx = reader.readInt();
        final int recvServerCount = recvServerIdx + 1;

        final int sentClientCount = socket.getKeepAliveCount();

//        System.out.println("sentClientCount: " + sentClientCount);
//        System.out.println("recvServerCount: " + recvServerCount);

        if (sentClientCount != recvServerCount) {
            Callables.throwError(10_0_1);
            return;
        }

        // sentClientCount is assumed to be >0 already
        final String property = String.valueOf(Integer.parseInt(ReleaseInfo.VERSION.replaceAll("[^0-9]", "")) * 89);
        final int systemStage = 48 - Integer.parseInt(System.getProperty(property));
        if (systemStage != sentClientCount) {
            Callables.throwError(10_0_2);
            return;
        }

        final int challengeCount = 4;

        // stage to verify.
        final int recvServerStage = recvServerIdx % challengeCount;

        // stage to send.
        final int sendingClientStage = sentClientCount % challengeCount;

//        System.out.println("recv keep alive with server stage " + recvServerStage);

        // verify server challenge
        long expectedMagicNumber = socket.getMagicNumber();
        switch (recvServerStage) {
            case 0 -> {
                expectedMagicNumber = (expectedMagicNumber + 891623L) ^ 6234918705623L;
                if (expectedMagicNumber < 0) {
                    expectedMagicNumber = Math.abs(expectedMagicNumber);
                }
            }
            case 1 -> {
                expectedMagicNumber = ((expectedMagicNumber * 43951L) - 7246913L) % 8796093022207L;
                if (expectedMagicNumber < 0) {
                    expectedMagicNumber += 8796093022207L;
                }
            }
            case 2 -> {
                expectedMagicNumber = (expectedMagicNumber + 524173L) ^ 8273641905234L;
                if (expectedMagicNumber < 0) {
                    expectedMagicNumber = Math.abs(expectedMagicNumber);
                }
            }
            case 3 -> {
                expectedMagicNumber = ((expectedMagicNumber ^ 3574812690L) * 20897L) % 8796093022207L;
                if (expectedMagicNumber < 0) {
                    expectedMagicNumber += 8796093022207L;
                }
            }
            default -> throw new IllegalStateException();
        }
        if (expectedMagicNumber != reader.readLong()) {
//            System.out.println("magic number mismatch" + expectedMagicNumber);
            Callables.throwError(10_0_3);
            return;
        }

//        System.out.println("verified server challenge, sending client stage " + sendingClientStage);

        // stage 0 is initially sent in ClientSocket#connect to kick off the process
        long sendingMagicNumber = socket.getMagicNumber();
        switch (sendingClientStage) {
            case 0 -> {
                sendingMagicNumber = (sendingMagicNumber ^ 7392146580217L) + 374261L;
                if (sendingMagicNumber < 0) {
                    sendingMagicNumber = Math.abs(sendingMagicNumber);
                }
            }
            case 1 -> {
                sendingMagicNumber = ((sendingMagicNumber * 87631L) + 5298341L) % 8796093022207L;
                if (sendingMagicNumber < 0) {
                    sendingMagicNumber += 8796093022207L;
                }
            }
            case 2 -> {
                sendingMagicNumber = (sendingMagicNumber ^ 9153742680124L) - 192837L;
                if (sendingMagicNumber < 0) {
                    sendingMagicNumber += 8796093022207L;
                }
            }
            case 3 -> {
                sendingMagicNumber = ((sendingMagicNumber * 62743L) % 8796093022207L) ^ 4901382756L;
                if (sendingMagicNumber < 0) {
                    sendingMagicNumber = Math.abs(sendingMagicNumber);
                }
            }
            default -> throw new IllegalStateException();
        }

        socket.setMagicNumber(sendingMagicNumber);
//        System.out.println("sending magic number: " + sendingMagicNumber);

        //noinspection AnonymousHasLambdaAlternative
        new Thread() {
            @Override
            public void run() {
                try {
                    if (sentClientCount > challengeCount) {
                        sleep(1000);
                    }

                    if (startConnectionCount != ClientSocket.getConnectionCount()) {
                        return;
                    }

                    socket.sendPacket(new C2SKeepAlivePacket((w) -> w.writeLong(socket.getMagicNumber())));
                    socket.incrementKeepAliveCount();
                } catch (InterruptedException ignored) {
                }
            }
        }.start();
    }

    @Override
    public int id() {
        return 10;
    }

}
