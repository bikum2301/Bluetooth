package com.example.bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button btnFindDevices; // Đổi tên từ btnPaired cho khớp layout của bạn
    ListView listViewDevices; // Đổi tên từ listDanhSach
    TextView textViewTitle;

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";

    // Request Code cho Intent và Permissions
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100; // Mã tự chọn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Đảm bảo đúng tên layout của bạn

        // Ánh xạ View từ layout của bạn
        btnFindDevices = findViewById(R.id.btnFindDevices);
        listViewDevices = findViewById(R.id.listViewDevices);
        textViewTitle = findViewById(R.id.textViewTitle); // Thêm nếu bạn muốn tương tác

        // Lấy Bluetooth adapter mặc định
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        // Kiểm tra thiết bị có hỗ trợ Bluetooth không
        if (myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "Thiết bị không hỗ trợ Bluetooth", Toast.LENGTH_LONG).show();
            finish(); // Thoát ứng dụng
            return; // Dừng thực thi tiếp
        }

        // Kiểm tra và yêu cầu quyền (Quan trọng cho Android 6+)
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions();
        } else {
            // Nếu đã có quyền, kiểm tra Bluetooth đã bật chưa
            checkBluetoothState();
        }


        // Xử lý sự kiện click nút tìm thiết bị
        btnFindDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBluetoothPermissions()) { // Kiểm tra lại quyền trước khi list
                    pairedDevicesList(); // Gọi hàm hiển thị danh sách thiết bị đã ghép đôi
                } else {
                    Toast.makeText(MainActivity.this, "Cần cấp quyền Bluetooth để tìm thiết bị", Toast.LENGTH_SHORT).show();
                    requestBluetoothPermissions(); // Yêu cầu lại quyền nếu chưa có
                }
            }
        });
    }

    // Hàm kiểm tra trạng thái Bluetooth (đã bật hay chưa)
    private void checkBluetoothState() {
        if (!myBluetooth.isEnabled()) {
            // Nếu Bluetooth chưa bật, yêu cầu người dùng bật
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // Cần kiểm tra quyền BLUETOOTH_CONNECT trước khi gọi startActivityForResult trên Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Nếu chưa có quyền CONNECT, phải yêu cầu trước
                    requestBluetoothPermissions(); // Yêu cầu các quyền cần thiết
                    Toast.makeText(this, "Cần quyền BLUETOOTH_CONNECT để bật Bluetooth", Toast.LENGTH_SHORT).show();
                    return; // Dừng lại, chờ người dùng cấp quyền
                }
            }
            // Nếu đã có quyền (hoặc phiên bản Android < 12), tiến hành yêu cầu bật Bluetooth
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    // Hàm kiểm tra các quyền Bluetooth cần thiết
    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else { // Android < 12
            // Quyền BLUETOOTH và BLUETOOTH_ADMIN được cấp lúc cài đặt nếu đã khai báo trong manifest
            // Nhưng vẫn nên kiểm tra Adapter có null không và có bật không
            return myBluetooth != null; // Chỉ cần adapter tồn tại là đủ ở phiên bản cũ
        }
    }

    // Hàm yêu cầu các quyền Bluetooth
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            // Đối với Android < 12, quyền được cấp qua manifest, chỉ cần yêu cầu bật Bluetooth nếu cần
            checkBluetoothState();
        }
    }

    // Xử lý kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Đã cấp quyền Bluetooth", Toast.LENGTH_SHORT).show();
                // Sau khi có quyền, kiểm tra xem Bluetooth đã bật chưa
                checkBluetoothState();
            } else {
                Toast.makeText(this, "Không thể sử dụng Bluetooth nếu không cấp quyền", Toast.LENGTH_LONG).show();
                // Có thể hiển thị thông báo giải thích hoặc vô hiệu hóa tính năng Bluetooth
            }
        }
    }


    // Xử lý kết quả từ Intent yêu cầu bật Bluetooth
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth đã được bật", Toast.LENGTH_SHORT).show();
                // Sau khi bật bluetooth, có thể tự động list thiết bị nếu muốn
                // pairedDevicesList();
            } else {
                Toast.makeText(getApplicationContext(), "Bạn cần bật Bluetooth để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                // Có thể thoát app hoặc vô hiệu hóa nút
                // finish();
            }
        }
    }


    // Hàm lấy và hiển thị danh sách thiết bị đã ghép đôi
    private void pairedDevicesList() {
        // Cần kiểm tra quyền BLUETOOTH_CONNECT trước khi gọi getBondedDevices trên Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Cần quyền BLUETOOTH_CONNECT để lấy danh sách thiết bị", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions(); // Yêu cầu lại nếu chưa có
                return; // Dừng lại
            }
        }

        // Nếu đã có quyền (hoặc Android < 12), tiếp tục lấy danh sách
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList<String> list = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress()); // Lấy tên và địa chỉ MAC
            }
        } else {
            Toast.makeText(getApplicationContext(), "Không tìm thấy thiết bị đã ghép đôi.", Toast.LENGTH_LONG).show();
        }

        // Hiển thị danh sách lên ListView
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        listViewDevices.setAdapter(adapter);
        listViewDevices.setOnItemClickListener(myListClickListener); // Gán listener khi click vào item
    }

    // Listener xử lý khi một item trong ListView được click
    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Lấy thông tin của item được click (Tên + MAC address)
            String info = ((TextView) v).getText().toString();
            // Trích xuất địa chỉ MAC (là 17 ký tự cuối cùng)
            String address = info.substring(info.length() - 17);

            // Tạo Intent để chuyển sang Activity điều khiển (BlueControl)
            Intent i = new Intent(MainActivity.this, BlueControl.class); // Đảm bảo BlueControl.class tồn tại

            // Đính kèm địa chỉ MAC vào Intent
            i.putExtra(EXTRA_ADDRESS, address);
            startActivity(i); // Bắt đầu Activity mới
        }
    };
}