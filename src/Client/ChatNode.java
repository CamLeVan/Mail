package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatNode {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Chọn chế độ: unicast / multicast / broadcast");
        String mode = sc.nextLine();

        switch (mode) {
            case "unicast" -> startUnicast();
            case "multicast" -> startMulticast();
            case "broadcast" -> startBroadcast();
            default -> System.out.println("Sai chế độ!");
        }
    }

    static void startUnicast() throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Bạn là server (s) hay client (c)?");
        String role = sc.nextLine();

        if (role.equalsIgnoreCase("s")) {
            ServerSocket server = new ServerSocket(5001);
            System.out.println("Server unicast đang chờ client...");
            Socket client = server.accept();
            System.out.println("Client kết nối: " + client.getInetAddress().getHostAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null)
                        System.out.println("[Client] " + msg);
                } catch (Exception ignored) {}
            }).start();

            while (true) {
                String msg = sc.nextLine();
                out.println(msg);
            }

        } else {
            Socket socket = new Socket("localhost", 5001);
            System.out.println("Đã kết nối server (unicast)");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null)
                        System.out.println("[Server] " + msg);
                } catch (Exception ignored) {}
            }).start();

            while (true) {
                String msg = sc.nextLine();
                out.println(msg);
            }
        }
    }

    static void startMulticast() throws Exception {
        InetAddress group = InetAddress.getByName("230.0.0.0");
        int port = 4446;

        MulticastSocket socket = new MulticastSocket(port);
        socket.setReuseAddress(true);
        socket.joinGroup(group);

        String localIP = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Multicast chat tại nhóm " + group.getHostAddress() + ":" + port);

        new Thread(() -> {
            byte[] buf = new byte[512];
            while (true) {
                try {
DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(msg);
                } catch (Exception ignored) {}
            }
        }).start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            String msg = sc.nextLine();
            String fullMsg = "[" + localIP + "] " + msg;
            byte[] data = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            socket.send(packet);
        }
    }

    static void startBroadcast() throws Exception {
        int port = 6007;
        DatagramSocket socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.setBroadcast(true);
        socket.bind(new InetSocketAddress(port));

        String localIP = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Broadcast chat tại port " + port);

        new Thread(() -> {
            byte[] buf = new byte[512];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(msg);
                } catch (Exception ignored) {}
            }
        }).start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            String msg = sc.nextLine();
            String fullMsg = "[" + localIP + ":" + port + "] " + msg;
            byte[] data = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    data, data.length,
                    InetAddress.getByName("255.255.255.255"), port
            );
            socket.send(packet);
        }
    }
}