package com.lulan.shincolle.gametest;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Source RCON client (the protocol vanilla 1.12 uses). Dev harness only.
 * Blocking, short timeouts; the harness calls it from the client tick for a few small commands.
 */
final class RconClient implements Closeable {

    private final Socket socket = new Socket();
    private final DataInputStream in;
    private int nextId = 1;

    RconClient(String host, int port, String password) throws IOException {
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(10000);
        in = new DataInputStream(socket.getInputStream());

        int id = send(3, password);
        int[] reply = read();
        if (reply[0] == -1 || reply[0] != id) throw new IOException("RCON authentication failed");
    }

    String command(String cmd) throws IOException {
        int id = send(2, cmd);
        StringBuilder sb = new StringBuilder();
        int[] reply = read();
        if (reply[0] != id) throw new IOException("RCON unexpected reply id " + reply[0]);
        sb.append(lastPayload);
        return sb.toString();
    }

    private String lastPayload = "";

    private int send(int type, String payload) throws IOException {
        int id = nextId++;
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 4 + body.length + 2).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(4 + 4 + body.length + 2);
        buf.putInt(id);
        buf.putInt(type);
        buf.put(body);
        buf.put((byte) 0).put((byte) 0);
        socket.getOutputStream().write(buf.array());
        socket.getOutputStream().flush();
        return id;
    }

    /** @return {id, type}; payload goes to lastPayload */
    private int[] read() throws IOException {
        byte[] head = new byte[12];
        in.readFully(head);
        ByteBuffer h = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
        int len = h.getInt();
        int id = h.getInt();
        int type = h.getInt();
        byte[] rest = new byte[len - 4 - 4 - 2 + 2];
        in.readFully(rest);
        int payloadLen = Math.max(0, len - 10);
        lastPayload = new String(rest, 0, Math.min(payloadLen, rest.length), StandardCharsets.UTF_8);
        return new int[] {id, type};
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
