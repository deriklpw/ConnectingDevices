package com.example.derik.connectingdevices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WifiP2PActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = WifiP2PActivity.class.getSimpleName();
    private WifiP2pManager mP2PManager;
    private WifiP2pManager.Channel mChannel;
    private P2PBroadcastReceiver mReceiver = new P2PBroadcastReceiver();
    private Button mBtnEnable;
    private Button mBtnDiscovery;
    private IntentFilter intentFilter;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private ListView listView;
    private WiFiPeerListAdapter mAdapter = new WiFiPeerListAdapter();


    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            // Out with the old, in with the new.
            peers.clear();
            peers.addAll(peerList.getDeviceList());

            // If an AdapterView is backed by this data, notify it
            // of the change.  For instance, if you have a ListView of available
            // peers, trigger an update.
            mAdapter.notifyDataSetChanged();
            if (peers.size() == 0) {
                Log.d(TAG, "No devices found");
                return;
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_p2p);
        mP2PManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        mChannel = mP2PManager.initialize(this, getMainLooper(), new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                Log.d(TAG, "onChannelDisconnected: ");
            }
        });

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        initView();
    }

    private void initView() {
        mBtnEnable = findViewById(R.id.bt_enable);
        mBtnEnable.setOnClickListener(this);
        mBtnDiscovery = findViewById(R.id.bt_discovery);
        mBtnDiscovery.setOnClickListener(this);
        listView = findViewById(R.id.p2p_list);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice device = peers.get(position);

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                mP2PManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(WifiP2PActivity.this, "Connect failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.bt_enable:
                break;
            case R.id.bt_discovery:
                // discovery process 初始化成功，则返回Success
                mP2PManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Code for when the discovery initiation is successful goes here.
                        // No services have actually been discovered yet, so this method
                        // can often be left blank.  Code for peer discovery goes in the
                        // onReceive method, detailed below.
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        // Code for when the discovery initiation fails goes here.
                        // Alert the user that something went wrong.
                    }
                });
                break;
            default:
                break;
        }
    }

    public class WiFiPeerListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return peers.size();
        }

        @Override
        public Object getItem(int position) {
            return peers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.service_list_item, parent, false);
                holder = new Holder();
                holder.host = convertView.findViewById(R.id.tv_host);
                holder.port = convertView.findViewById(R.id.tv_port);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            if (peers != null && peers.size() > 0) {
                holder.host.setText(peers.get(position).deviceName + ", address=" + peers.get(position).deviceAddress);
                holder.port.setText("null");
            }
            return convertView;
        }

        class Holder {
            TextView host;
            TextView port;
        }
    }

    public class P2PBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
//                    activity.setIsWifiP2pEnabled(true);
                    mBtnEnable.setEnabled(true);
                } else {
//                    activity.setIsWifiP2pEnabled(false);
                    mBtnEnable.setEnabled(false);
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                // The peer list has changed!  We should probably do something about
                // that.
                if (mP2PManager != null) {
                    mP2PManager.requestPeers(mChannel, peerListListener);
                }
                Log.d(WifiP2PActivity.TAG, "P2P peers changed");

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // Connection state changed!  We should probably do something about
                // that.

                if (mP2PManager == null) {
                    return;
                }

                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    // We are connected with the other device, request connection
                    // info to find group owner IP
                    mP2PManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            // InetAddress from WifiP2pInfo struct.
                            final String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
                            Log.d(TAG, "groupOwnerAddress: " + groupOwnerAddress);
                            // After the group negotiation, we can determine the group owner.
                            if (info.groupFormed && info.isGroupOwner) {
                                // Do whatever tasks are specific to the group owner.
                                // One common case is creating a server thread and accepting
                                // incoming connections.

                            } else if (info.groupFormed) {
                                // The other device acts as the client. In this case,
                                // you'll want to create a client thread that connects to the group
                                // owner.
                            }

                        }
                    });
                }


            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
