package com.example.derik.connectingdevices;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity {

    private static final String TAG = MainActivity.class.getName();

    private String[] datas = {
            "Using Network Service Discovery (NSD)",
            "Creating P2P Connections with Wi-Fi (Create P2P)",
            "Using Wi-Fi P2P for Service Discovery (Use P2P)"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, datas);
        setListAdapter(arrayAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "onListItemClick: " + position);
        super.onListItemClick(l, v, position, id);
        switch (position){
            case 0:
                startActivity(new Intent(this, NSDActivity.class));
                break;
            case 1:
                startActivity(new Intent(this, WifiP2PActivity.class));
                break;
            case 2:
                break;
            default:
        }
    }
}
