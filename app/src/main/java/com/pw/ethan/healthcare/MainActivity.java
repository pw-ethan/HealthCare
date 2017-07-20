package com.pw.ethan.healthcare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.pw.ethan.lib.service.Client;
import com.pw.ethan.lib.service.ISocketResponse;
import com.pw.ethan.lib.service.Packet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements OnClickListener {
    private String IP = "";
    private String PORT = "";

    private EditText et_email, et_pass, et_port;
    private String email, psd;
    private Button mLoginButton, mLoginError, mRegister;
    private Client user;


    private final String TAG = "HealthCare";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_email = findViewById(R.id.email);
        et_pass = findViewById(R.id.password);
        et_port = findViewById(R.id.editText);

        mLoginButton = findViewById(R.id.login);
        mLoginError = findViewById(R.id.forget_psd);
        mRegister = findViewById(R.id.register);
        mLoginButton.setOnClickListener(this);
        mLoginError.setOnClickListener(this);
        mRegister.setOnClickListener(this);


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login:
                IP = "219.216.65.174";
                PORT = et_port.getText().toString();
                user = new Client(this.getApplicationContext(), socketListener);
                user.open(IP, Integer.valueOf(PORT));

                email = et_email.getText().toString();
                psd = et_pass.getText().toString();

                // Use regular expressions to determine user input formats
                String regEx = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+";
                Pattern pattern = Pattern.compile(regEx);
                Matcher matcher = pattern.matcher(email);
                boolean rs = matcher.matches();

                if (!rs) {
                    // Pop up the warning box if the input format is incorrect
                    new AlertDialog.Builder(this)
                            .setTitle("Login Result")
                            .setMessage("Failed : the format of email is wrong!")
                            .setPositiveButton("I know.", null)
                            .show();
                } else {
                    JSONObject sign_in_info = new JSONObject();
                    try {
                        sign_in_info.put("type", "sign_in");
                        sign_in_info.put("username", email);
                        sign_in_info.put("password", psd);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Packet packet = new Packet();
                    packet.pack(sign_in_info.toString());
                    user.send(packet);
                }
                break;
        }
    }

    private ISocketResponse socketListener = new ISocketResponse() {
        @Override
        public void onSocketResponse(final String txt) {
            runOnUiThread(new Runnable() {
                public void run() {
                    ProcessResponse(txt);
                }
            });
        }
    };

    //
    public void ProcessResponse(String strResponse) {
        Log.i(TAG, "recv: " + strResponse);
        try {
            JSONObject response = new JSONObject(strResponse);
            if (response.getString("type").equals("reply_sign_in") && response.getInt("state_code") == 0) {
                SendExit();
            } else if(response.getString("type").equals("reply_exit") && response.getInt("state_code") == 0) {
                user.close();
                Bundle bundle = new Bundle();
                bundle.putString("IP", IP);
                bundle.putString("PORT", PORT);
                Intent intent = new Intent(MainActivity.this, RCUInteractionActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
            else {
                new AlertDialog.Builder(this)
                        .setTitle("Login Result")
                        .setMessage("Failed : The error code : " + response.getInt("state_code"))
                        .setPositiveButton("I know.", null)
                        .show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void SendExit() {
        JSONObject exit_info = new JSONObject();
        try {
            exit_info.put("type", "exit");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet packet = new Packet();
        packet.pack(exit_info.toString());
        user.send(packet);
    }
}