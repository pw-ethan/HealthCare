package com.pw.ethan.healthcare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;


import com.pw.ethan.lib.service.Client;
import com.pw.ethan.lib.service.ISocketResponse;
import com.pw.ethan.lib.service.Packet;
import com.pw.ethan.lib.util.NativeKitUtil;
import com.pw.ethan.lib.vtree.VerifierTree;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class RCUInteractionActivity extends Activity {

    private VerifierTree vtree = null;
    private Client user = null;
    private String IP = "";
    private String PORT = "";

    private final String TAG = "HealthCare";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcuinteraction);

        findViewById(R.id.send).setOnClickListener(listener);
        findViewById(R.id.hash).setOnClickListener(listener);

        Bundle bundle = this.getIntent().getExtras();
        IP = bundle.getString("IP");
        PORT = bundle.getString("PORT");
        user = new Client(this.getApplicationContext(), socketListener);
        user.open(IP, Integer.valueOf(PORT));

        vtree = new VerifierTree();
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

    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.send:
                    JSONObject append_elements_info = new JSONObject();
                    try {
                        if (vtree.IsFull()) {
                            vtree.updateVTree(getWeights(vtree.getDepth()));
                        }
                        vtree.appendValue(1);

                        int root = vtree.getEvidence();

                        append_elements_info.put("type", "append_elements");
                        append_elements_info.put("element", 1);
                        append_elements_info.put("root", root);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Packet packet = new Packet();
                    packet.pack(append_elements_info.toString());
                    user.send(packet);
                    break;

                case R.id.hash:
                    Log.i(TAG, "c++: " + NativeKitUtil.StringFromJNI());
                    break;
            }
        }
    };

    public void ProcessResponse(String strResponse) {
        try {
            JSONObject response = new JSONObject(strResponse);
            if (response.getString("type").equals("append_elements")) {
                Log.i(TAG, "recv: " + strResponse);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Append Result")
                        .setMessage("Failed : The error code : " + response.getInt("state_code"))
                        .setPositiveButton("I know.", null)
                        .show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
            mHomeIntent.addCategory(Intent.CATEGORY_HOME);
            mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            RCUInteractionActivity.this.startActivity(mHomeIntent);
        }
        return true;
    }

    private ArrayList<Integer> getWeights(int n) {
        ArrayList<Integer> weights = new ArrayList<>();
        for (int i = 0; i < PowerTwo(n); ++i) {
            weights.add(i + 1);
        }
        return weights;
    }

    private int PowerTwo(int n) {
        return 1 << n;
    }
}
