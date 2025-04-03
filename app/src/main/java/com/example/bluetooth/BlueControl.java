package com.example.bluetooth; // Hoặc package của bạn

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Thêm import cho Button
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class BlueControl extends AppCompatActivity {

    ImageButton btnTb1, btnTb2, btnDis;
    Button btnAllOn, btnAllOff; // Khai báo nút mới
    TextView txtStatus, txtMAC;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    int flaglamp1 = 0;
    int flaglamp2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sửa tên layout nếu file của bạn tên khác
        setContentView(R.layout.activity_blue_control);

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);

        // Ánh xạ view
        btnTb1 = findViewById(R.id.btnTb1);
        btnTb2 = findViewById(R.id.btnTb2);
        btnDis = findViewById(R.id.btnDisc);
        txtStatus = findViewById(R.id.textV1);
        txtMAC = findViewById(R.id.textViewMAC);
        btnAllOn = findViewById(R.id.btnAllOn);   // Ánh xạ nút mới
        btnAllOff = findViewById(R.id.btnAllOff); // Ánh xạ nút mới

        if (address != null) {
            txtMAC.setText(address);
        } else {
            txtMAC.setText("Không nhận được địa chỉ");
        }

        new ConnectBT().execute(); // Kết nối Bluetooth

        // --- Listener cho các nút ---
        btnTb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gửi lệnh BẬT ('1') hoặc TẮT ('A') cho riêng đèn 1
                sendSingleSignal("1", "A", btnTb1, 1);
            }
        });

        btnTb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gửi lệnh BẬT ('7') hoặc TẮT ('G') cho riêng đèn 2
                sendSingleSignal("7", "G", btnTb2, 2);
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });

        // Listener cho nút Bật Cả Hai
        btnAllOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnBothOn(); // Gọi hàm bật cả hai
            }
        });

        // Listener cho nút Tắt Cả Hai
        btnAllOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnBothOff(); // Gọi hàm tắt cả hai
            }
        });
    }

    // Hàm gửi tín hiệu cho MỘT đèn (tái cấu trúc hàm cũ)
    private void sendSingleSignal(String signalOn, String signalOff, ImageButton button, int lampIndex) {
        if (btSocket != null && isBtConnected) {
            try {
                String signalToSend;
                int currentFlag = (lampIndex == 1) ? flaglamp1 : flaglamp2;

                if (currentFlag == 0) { // Nếu đang tắt -> Bật
                    signalToSend = signalOn;
                    btSocket.getOutputStream().write(signalToSend.getBytes());
                    txtStatus.setText("Thiết bị " + signalOn + " đang BẬT");
                    if (lampIndex == 1) {
                        button.setBackgroundResource(R.drawable.tb1on);
                        flaglamp1 = 1;
                    } else {
                        button.setBackgroundResource(R.drawable.tb2on);
                        flaglamp2 = 1;
                    }
                } else { // Nếu đang bật -> Tắt
                    signalToSend = signalOff;
                    btSocket.getOutputStream().write(signalToSend.getBytes());
                    txtStatus.setText("Thiết bị " + signalOn + " đang TẮT"); // Hiển thị theo signalOn cho dễ nhận biết
                    button.setBackgroundResource(R.drawable.tbloff);
                    if (lampIndex == 1) {
                        flaglamp1 = 0;
                    } else {
                        flaglamp2 = 0;
                    }
                }
                // msg("Đã gửi: " + signalToSend);

            } catch (IOException e) {
                msg("Lỗi khi gửi tín hiệu: " + e.getMessage());
                Disconnect();
            }
        } else {
            msg("Chưa kết nối Bluetooth.");
        }
    }

    // Hàm để BẬT cả hai đèn bằng cách gửi hai lệnh riêng biệt
    private void turnBothOn() {
        if (btSocket != null && isBtConnected) {
            try {
                // Gửi lệnh bật đèn 1
                btSocket.getOutputStream().write("1".getBytes());
                msg("Đã gửi lệnh bật 1"); // Thông báo (có thể xóa)

                // Thêm một khoảng dừng nhỏ (ví dụ: 100ms)
                // để đảm bảo Arduino có thời gian xử lý lệnh trước đó
                try {
                    Thread.sleep(100); // 100 mili giây
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Khôi phục trạng thái ngắt
                }

                // Gửi lệnh bật đèn 2
                btSocket.getOutputStream().write("7".getBytes());
                msg("Đã gửi lệnh bật 7"); // Thông báo (có thể xóa)

                // Cập nhật trạng thái và UI sau khi gửi cả hai lệnh
                flaglamp1 = 1;
                flaglamp2 = 1;
                btnTb1.setBackgroundResource(R.drawable.tb1on);
                btnTb2.setBackgroundResource(R.drawable.tb2on);
                txtStatus.setText("Cả hai thiết bị đang BẬT");

            } catch (IOException e) {
                msg("Lỗi khi BẬT cả hai: " + e.getMessage());
                Disconnect();
            }
        } else {
            msg("Chưa kết nối Bluetooth.");
        }
    }

    // Hàm để TẮT cả hai đèn bằng cách gửi hai lệnh riêng biệt
    private void turnBothOff() {
        if (btSocket != null && isBtConnected) {
            try {
                // Gửi lệnh tắt đèn 1
                btSocket.getOutputStream().write("A".getBytes());
                msg("Đã gửi lệnh tắt A"); // Thông báo (có thể xóa)

                // Khoảng dừng nhỏ
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Gửi lệnh tắt đèn 2
                btSocket.getOutputStream().write("G".getBytes());
                msg("Đã gửi lệnh tắt G"); // Thông báo (có thể xóa)

                // Cập nhật trạng thái và UI
                flaglamp1 = 0;
                flaglamp2 = 0;
                btnTb1.setBackgroundResource(R.drawable.tbloff);
                btnTb2.setBackgroundResource(R.drawable.tbloff);
                txtStatus.setText("Cả hai thiết bị đang TẮT");

            } catch (IOException e) {
                msg("Lỗi khi TẮT cả hai: " + e.getMessage());
                Disconnect();
            }
        } else {
            msg("Chưa kết nối Bluetooth.");
        }
    }


    // --- Các hàm còn lại giữ nguyên ---
    private void Disconnect() {
        if (btSocket != null) {
            try {
                isBtConnected = false;
                btSocket.close();
                msg("Đã ngắt kết nối Bluetooth");
            } catch (IOException e) {
                msg("Lỗi khi đóng Socket: " + e.getMessage());
            }
        }
        finish();
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show(); // Đổi sang SHORT cho đỡ vướng
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(BlueControl.this, "Đang kết nối...", "Xin vui lòng đợi!!!");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(BlueControl.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            ConnectSuccess = false;
                            return null;
                        }
                    }
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }

            if (!ConnectSuccess) {
                msg("Kết nối thất bại! Kiểm tra lại thiết bị.");
                finish();
            } else {
                msg("Kết nối thành công.");
                isBtConnected = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Disconnect();
    }
}