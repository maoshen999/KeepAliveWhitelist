package com.example.keepalive;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity; // 如果你的工程没引入appcompat，请改成 android.app.Activity
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private EditText etPackageName;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> packageList = new ArrayList<>();
    private final String CONFIG_PATH = "/data/adb/modules/keepalive_whitelist/whitelist.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPackageName = findViewById(R.id.etPackageName);
        Button btnAdd = findViewById(R.id.btnAdd);
        listView = findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, packageList);
        listView.setAdapter(adapter);

        // 加载已有数据
        loadWhitelist();

        // 添加按钮点击事件
        btnAdd.setOnClickListener(v -> {
            String pkg = etPackageName.getText().toString().trim();
            if (!pkg.isEmpty() && !packageList.contains(pkg)) {
                packageList.add(pkg);
                adapter.notifyDataSetChanged();
                etPackageName.setText("");
                saveWhitelist();
            }
        });

        // 长按删除
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            packageList.remove(position);
            adapter.notifyDataSetChanged();
            saveWhitelist();
            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void loadWhitelist() {
        packageList.clear();
        try {
            Process process = Runtime.getRuntime().exec("su -c cat " + CONFIG_PATH);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    packageList.add(line);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            Toast.makeText(this, "读取失败，请确保已授予Root权限", Toast.LENGTH_SHORT).show();
        }
        adapter.notifyDataSetChanged();
    }

    private void saveWhitelist() {
        StringBuilder sb = new StringBuilder();
        for (String pkg : packageList) {
            sb.append(pkg).append("\n");
        }
        
        try {
            // 1. 确保目录存在
            Runtime.getRuntime().exec("su -c mkdir -p /data/adb/modules/keepalive_whitelist");
            
            // 2. 写入文件
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("echo '" + sb.toString() + "' > " + CONFIG_PATH + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            
            Toast.makeText(this, "配置已保存，重启手机生效", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
