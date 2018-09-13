package com.example.derik.connectingdevices;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;

public class NSDActivity extends AppCompatActivity {

    private static final String TAG = "NSDActivity";
    private ServerSocket mServerSocket;
    private int mLocalPort;
    private Button register;
    private Button discover;
    private NsdManager.RegistrationListener mRegistrationListener;
    private String mServiceName;
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private String SERVICE_TYPE = "_http._tcp.";
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mService;
    private EditText registerName;
    private EditText discoverName;
    private boolean isRegistered = false;
    private String mDiscoverName;
    private Button tearDown;
    private boolean isDiscoverStarted;
    private ListView mListView;
    private ArrayList<NsdServiceInfo> devicesList = new ArrayList<>();
    private HashSet<String> mResultsSet = new HashSet();
    private MyAdapter adapter;

    private static final int CONSTANT_UPDATE_RESULT = 0x001;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONSTANT_UPDATE_RESULT:
                    adapter.notifyDataSetChanged();
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nsd);
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        initializeRegistrationListener();
        initializeDiscoveryListener();
        initializeResolveListener();
        initializeServerSocket();
        initView();

    }

    private void initView() {
        register = findViewById(R.id.register);
        discover = findViewById(R.id.discover);
        registerName = findViewById(R.id.input_register);
        discoverName = findViewById(R.id.input_discover);
        tearDown = findViewById(R.id.tear_down);
        mListView = findViewById(R.id.result_list);
        adapter = new MyAdapter();
        mListView.setAdapter(adapter);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = registerName.getText().toString().trim();
                if (!TextUtils.isEmpty(name)) {
                    if (!isRegistered) {
                        registerNsdService(name, mLocalPort);
                    } else {
                        Log.d(TAG, "onClick: has been registered");
                    }

                }
            }
        });

        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = discoverName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(NSDActivity.this, "discover name is empty", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    mDiscoverName = name;
                }
                if (!isDiscoverStarted) {
                    mNsdManager.discoverServices(
                            SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                    isDiscoverStarted = true;
                } else {
                    Log.d(TAG, "onClick: discovery has been started");
                }

            }
        });

        tearDown.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                tearDown();
            }
        });
    }

    //使用NSD的Service都可以看得到这个服务
    private void registerNsdService(String name, int port) {
        Log.d(TAG, "registerNsdService: port=" + port);
        NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
        nsdServiceInfo.setServiceName(name);
        nsdServiceInfo.setServiceType(SERVICE_TYPE);
        nsdServiceInfo.setPort(port);

        mNsdManager.registerService(
                nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        isRegistered = true;

    }

    public void initializeServerSocket() {
        try {
            // Initialize a server socket on the next available port.
            mServerSocket = new ServerSocket(0);
            // Store the chosen port.
            mLocalPort = mServerSocket.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
                Log.d(TAG, "onServiceRegistered: " + mServiceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                Log.d(TAG, "onRegistrationFailed: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                Log.d(TAG, "onServiceUnregistered: ");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                Log.d(TAG, "onUnregistrationFailed: " + errorCode);
            }
        };
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success, " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains(mDiscoverName)) {
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();
                Log.d(TAG, "onServiceResolved: host=" + host + ", port = " + port);
                if (!mResultsSet.contains(host.getHostAddress())) {
                    mResultsSet.add(host.getHostAddress());
                } else {
                    return;
                }

                if (mResultsSet.size() > devicesList.size()){
                    devicesList.add(serviceInfo);
                    mHandler.sendEmptyMessage(CONSTANT_UPDATE_RESULT);
                }
            }
        };
    }

    // NsdHelper's tearDown method
    public void tearDown() {
        if (isRegistered) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
        if (isDiscoverStarted) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

        isRegistered = false;
        isDiscoverStarted = false;
    }

    public class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return devicesList.size();
        }

        @Override
        public Object getItem(int position) {
            return devicesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyHolder holder;
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.service_list_item, null);
                holder = new MyHolder();
                holder.host = convertView.findViewById(R.id.tv_host);
                holder.port = convertView.findViewById(R.id.tv_port);
                convertView.setTag(holder);
            } else {
                holder = (MyHolder) convertView.getTag();
            }
            holder.host.setText(devicesList.get(position).getHost().getHostAddress());
            holder.port.setText(""+devicesList.get(position).getPort());
            return convertView;
        }
    }

    class MyHolder {
        TextView host;
        TextView port;
    }
}
