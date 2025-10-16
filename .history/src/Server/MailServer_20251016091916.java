package Server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MailServer {
    private static final int SERVER_PORT = 1233;
    private static final String BASE_DIR = "mail_data";

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(SERVER_PORT);
            System.out.println("Mail Server đang chạy...");

            Files.createDirectories(Paths.get(BASE_DIR));

            while (true) {
                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("IP Client:" + packet.getAddress().getHostAddress() + ":" + packet.getPort());
                
                String request = new String(packet.getData(), 0, packet.getLength());
                String[] parts = request.split("\\|");
                String command = parts[0];
                String response = "";

                switch (command) {
                    case "REGISTER": {
                        String username = parts[1];
                        Path userDir = Paths.get(BASE_DIR, username);
                        if (Files.exists(userDir)) {
                            response = "Tài khoản đã tồn tại!";
                        } else {
                            Files.createDirectories(userDir);

                            LocalDateTime now = LocalDateTime.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            String formatDateTime = now.format(formatter);
                            response = "Đăng ký thành công! lúc " + formatDateTime;

                            Files.writeString(userDir.resolve("new_email.txt"), "Cảm ơn bạn đã sử dụng dịch vụ!");
                            Files.writeString(userDir.resolve("new_email.txt"), formatDateTime);
                        }
                        break;
                    }

                    case "LOGIN": {
                        String username = parts[1];
                        Path userDir = Paths.get(BASE_DIR, username);
                        if (Files.exists(userDir)) {
                            File[] files = userDir.toFile().listFiles();
                            StringBuilder sb = new StringBuilder();
                            sb.append("USER_OK\n"); // báo client biết login thành công
                            for (File f : files) {
                                sb.append(f.getName()).append("\n");
                            }
                            response = sb.toString();
                        } else {
                            response = "Account không tồn tại!";
                        }
                        break;
                    }

                    case "SEND": {
                        String receiver = parts[1];
                        String message = parts[2];
                        Path receiverDir = Paths.get(BASE_DIR, receiver);
                        if (Files.exists(receiverDir)) {
                            String filename = "mail_" + System.currentTimeMillis() + ".txt";
                            Files.writeString(receiverDir.resolve(filename), message);
                            response = "Gửi mail thành công!";
                        } else {
                            response = "Người nhận không tồn tại!";
                        }
                        break;
                    }

                    case "READ": {
                        String username = parts[1];
                        String filename = parts[2];
                        Path filePath = Paths.get(BASE_DIR, username, filename);
                        if (Files.exists(filePath)) {
                            response = Files.readString(filePath);
                        } else {
                            response = "Mail không tồn tại!";
                        }
                        break;
                    }

                    default:
                        response = "Lệnh không hợp lệ!";
                }

                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, packet.getAddress(), packet.getPort());
                socket.send(sendPacket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
