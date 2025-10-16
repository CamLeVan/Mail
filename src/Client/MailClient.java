package Client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

public class MailClient extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 1233;
    private DatagramSocket socket;

        private JTextField txtUsername, txtReceiver, txtMessage, txtSubject;
    private JTextArea txtMailContent;
    private JTable tblMails;
    private DefaultTableModel mailModel;
    private JLabel lblSession;
    private String currentUser = null;
    private JButton btnRefresh;

    public MailClient() {
        setTitle("UDP Mail Client");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể khởi tạo socket!");
            System.exit(0);
        }

        // ====== PANEL TRÊN ======
        JPanel topPanel = new JPanel();
        txtUsername = new JTextField(10);
        JButton btnRegister = new JButton("Tạo Account");
        JButton btnLogin = new JButton("Đăng nhập");
        lblSession = new JLabel("Chưa đăng nhập");
        btnRefresh = new JButton("Refresh");
        btnRefresh.setEnabled(false);

        topPanel.add(new JLabel("Username:"));
        topPanel.add(txtUsername);
        topPanel.add(btnRegister);
        topPanel.add(btnLogin);
        topPanel.add(btnRefresh);
        topPanel.add(lblSession);

        // ====== DANH SÁCH MAIL ======
        mailModel = new DefaultTableModel(new String[]{"Tên email"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa cell
            }
        };
        tblMails = new JTable(mailModel);
        tblMails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollMailList = new JScrollPane(tblMails);

        // ====== NỘI DUNG MAIL ======
        txtMailContent = new JTextArea();
        txtMailContent.setEditable(false); //Không cho chỉnh sửa
        txtMailContent.setLineWrap(true);
        txtMailContent.setWrapStyleWord(true);
        txtMailContent.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtMailContent.setBackground(new Color(245, 245, 245));
        txtMailContent.setBorder(BorderFactory.createTitledBorder("Nội dung email"));
        JScrollPane scrollMailContent = new JScrollPane(txtMailContent);
        scrollMailContent.setPreferredSize(new Dimension(350, 0));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollMailList, scrollMailContent);
        centerSplit.setDividerLocation(400);

        // ====== GỬI MAIL ======
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Soạn email"));

        JPanel fieldsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        txtReceiver = new JTextField();
        txtSubject = new JTextField();
        txtMessage = new JTextField();
        fieldsPanel.add(new JLabel("Người nhận:"));
        fieldsPanel.add(txtReceiver);
        fieldsPanel.add(new JLabel("Tiêu đề:"));
        fieldsPanel.add(txtSubject);
        fieldsPanel.add(new JLabel("Nội dung:"));
        fieldsPanel.add(txtMessage);

        JButton btnSend = new JButton("Gửi Email");

        bottomPanel.add(fieldsPanel, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerSplit, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // ====== SỰ KIỆN ======
        btnRegister.addActionListener(e -> registerAccount());
        btnLogin.addActionListener(e -> loginAccount());
        btnSend.addActionListener(e -> sendMail());
        btnRefresh.addActionListener(e -> refreshMails());

        // Khi double-click vào 1 file => đọc nội dung mail
        tblMails.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentUser != null) {
                    int row = tblMails.getSelectedRow();
                    if (row != -1) {
                        String filename = (String) mailModel.getValueAt(row, 0);
                        readMail(filename);
                    }
                }
            }
        });
    }

    private void refreshMails() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập trước!");
            return;
        }
        String response = sendRequest("LOGIN|" + currentUser);
        mailModel.setRowCount(0);
        if (response.startsWith("USER_OK")) {
            String[] lines = response.split("\n");
            for (int i = 1; i < lines.length; i++) {
                if (!lines[i].isBlank()) {
                    mailModel.addRow(new Object[]{lines[i]});
                }
            }
            txtMailContent.setText("Danh sách email đã được cập nhật!\n\nHãy chọn một email để đọc nội dung.");
        } else {
            JOptionPane.showMessageDialog(this, response);
        }
    }

    private void registerAccount() {
        String username = txtUsername.getText().trim();
        String response = sendRequest("REGISTER|" + username);
        JOptionPane.showMessageDialog(this, response);
    }

    private void loginAccount() {
        String username = txtUsername.getText().trim();
        String response = sendRequest("LOGIN|" + username);

        mailModel.setRowCount(0);
        if (response.startsWith("USER_OK")) {
            currentUser = username;
            lblSession.setText("Đang đăng nhập: " + username);
            btnRefresh.setEnabled(true);
            String[] lines = response.split("\n");
            for (int i = 1; i < lines.length; i++) {
                if (!lines[i].isBlank()) mailModel.addRow(new Object[]{lines[i]});
            }
            txtMailContent.setText("Chào mừng " + username + "!\n\nHãy chọn một email để đọc nội dung.");
        } else {
            JOptionPane.showMessageDialog(this, response);
        }
    }

    private void sendMail() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập trước!");
            return;
        }

        String receiver = txtReceiver.getText().trim();
        String subject = txtSubject.getText().trim();
        String msgContent = txtMessage.getText().trim();

        
        if (receiver.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập người nhận!");
            return;
        }

        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tiêu đề email!");
            return;
        }

        if (msgContent.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung trước khi gửi email!");
            return;
        }

        String msg = "From: " + currentUser + "\n" + "Subject: " + subject + "\n\n" + msgContent;
        String response = sendRequest("SEND|" + receiver + "|" + msg);
        JOptionPane.showMessageDialog(this, response);

        // Nếu người nhận là chính mình thì refresh danh sách mail ngay
        if (receiver.equals(currentUser)) {
            refreshMails();
        }

        // Clear fields sau khi gửi
        txtReceiver.setText("");
        txtSubject.setText("");
        txtMessage.setText("");
    }

    private void readMail(String filename) {
        if (currentUser == null) {
            return;
        }
        String response = sendRequest("READ|" + currentUser + "|" + filename);
        if (response.equals("Mail không tồn tại!")) {
            JOptionPane.showMessageDialog(this, response);
            return;
        }
        // Hiển thị nội dung trong vùng text (read-only)
        txtMailContent.setEditable(false);
        txtMailContent.setText("Xin chào bạn có lời nhắn từ " + filename + "\n\n" + response);
        txtMailContent.setCaretPosition(0); // Cuộn lên đầu
    }

    private String sendRequest(String req) {
        try {
            byte[] sendData = req.getBytes();
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length,
                    InetAddress.getByName(SERVER_IP), SERVER_PORT);
            socket.send(packet);

            byte[] receiveData = new byte[2048];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (Exception e) {
            return "Lỗi khi gửi yêu cầu!" + e.getMessage();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MailClient().setVisible(true));
    }
}