package com.pw.ethan.healthcare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;


import com.pw.ethan.lib.service.Client;
import com.pw.ethan.lib.service.ISocketResponse;
import com.pw.ethan.lib.service.Packet;
import com.pw.ethan.lib.util.NativeKitUtil;

import org.json.JSONException;
import org.json.JSONObject;


public class CloudSerSendActivity extends Activity {

    private Client user = null;
    private EditText sendContent;
    private String IP = "";
    private String PORT = "";

    private final String TAG = "HealthCare";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloudsersendactivity);

        Bundle bundle = this.getIntent().getExtras();
        IP = bundle.getString("IP");
        PORT = bundle.getString("PORT");

        findViewById(R.id.send).setOnClickListener(listener);
        findViewById(R.id.hash).setOnClickListener(listener);

        sendContent = findViewById(R.id.sendContent);


        user = new Client(this.getApplicationContext(), socketListener);
    }

    private ISocketResponse socketListener = new ISocketResponse() {

        @Override
        public void onSocketResponse(final String txt) {
            runOnUiThread(new Runnable() {
                public void run() {
                    JudgeRegisterResult(txt);
                }
            });
        }
    };

    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.send:
                    JSONObject json = new JSONObject();
                    try {
                        json.put("type", "append_elements");
                        json.put("element", 12);
                        json.put("root", "xxx");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Packet packet = new Packet();
                    packet.pack(json.toString());

                    user.open(IP, Integer.valueOf(PORT));
                    user.send(packet);
                    break;
                case R.id.hash:
                    Log.i(TAG, "onClick: hash");
                    //sendContent.setText(NativeKitUtil.StringFromJNI());
                    Log.i(TAG, "c++: " + NativeKitUtil.StringFromJNI());
                    break;
            }
        }
    };

    public void JudgeRegisterResult(String respond) {
        try {
            JSONObject resp = new JSONObject(respond);
            if (resp.getString("type").equals("reply_sign_in") && resp.getInt("state_code") == 0) {
                sendContent.setText(sendContent.getText() + "\nresult : " + respond);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Login Result")
                        .setMessage("Failed : the cloud error\n Error code : " + resp.getInt("state_code"))
                        .setPositiveButton("I know", null)
                        .show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //客户自己发送信息与否是自己控制的，所以在这里如果返回的话就不必在开着后台的进程，直接杀死即可
//	public boolean onKeyDown ( int keyCode, KeyEvent event){
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			user.close();
//			android.os.Process.killProcess(android.os.Process.myPid());
//		}
//		return super.onKeyDown(keyCode, event);
//	}
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
            mHomeIntent.addCategory(Intent.CATEGORY_HOME);
            mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            CloudSerSendActivity.this.startActivity(mHomeIntent);
        }
        return true;
    }
}
