package activity;
import com.rondo.ble.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;


public class BLEScanActivity extends Activity {
	
	private static final int REQUEST_ADAPTER_CODE = 0;
	private static final int SINGLE_SCAN_PERIOD = 15000;
	private boolean isScanning = false;
	
	private ListView device_list;
	private ImageButton btn_scan;
	
	private BLEDeviceAdapter ble_adapter;
	private BluetoothManager mManager;
	private BluetoothAdapter mAdapter;
	private Handler mHandler;
	
	/**
	 * Callback interface
	 * Invoke immediately when a BLE device is scanned
	 */
	final BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					ble_adapter.addDevice(device);
					ble_adapter.notifyDataSetChanged();					
				}
			});
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_ble);
		initView();
		setUpListeners();
		isBLEAvailable();
		initBLE();
		
		mHandler = new Handler();
		if(mAdapter == null || !mAdapter.isEnabled()){
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent,REQUEST_ADAPTER_CODE);   // if unable, request to enable the adapter
		}
	}
	
	private void initView(){
		device_list = (ListView) findViewById(R.id.ble_device_list);
		btn_scan = (ImageButton) findViewById(R.id.start_process);
		ble_adapter = new BLEDeviceAdapter(this);
		device_list.setAdapter(ble_adapter);
	} 
	
	private void setUpListeners(){
		btn_scan.setOnClickListener(new View.OnClickListener() {
		 @Override
		 public void onClick(View v) {
			 scanLeDevice(true);
		 }
		});
		device_list.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				BluetoothDevice _device = (BluetoothDevice)ble_adapter.getItem(position);
				Intent intent  = new Intent(BLEScanActivity.this,DataControlActivity.class);
				intent.putExtra(DataControlActivity.DEVICE_NAME,_device.getName());
				intent.putExtra(DataControlActivity.DEVICE_ADDR,_device.getAddress());
				startActivity(intent);
			}
		});
	}
	 
	private void initBLE(){
		if(mManager == null){
		   mManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
		   if(mManager == null){
			   Toast.makeText(getApplicationContext(), "Unable to init Bluetooth", 
					   Toast.LENGTH_SHORT).show();
			   Log.e("INIT_FAILED", "Unable to start BluetoothManager");
		   }
		}
		mAdapter = mManager.getAdapter();
		if(mAdapter == null){
			  Toast.makeText(getApplicationContext(), "Unable to start Bluetooth", 
					   Toast.LENGTH_SHORT).show();
			  Log.e("INIT_FAILED", "Unable to start BluetoothAdapter");
		}
	}
	
	/**
	 * Check whether the device supports BLE functionalities
	 * @return true if support
	 */
	private boolean isBLEAvailable(){
		if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
			Toast.makeText(getApplicationContext(), "Your device doesn't support bluetooth LE protocol", 
					Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	
	/**
	 * Callback 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_ADAPTER_CODE && resultCode == Activity.RESULT_CANCELED){
			Toast.makeText(getApplicationContext(), "You must open the bluetooth to enable this function", 
					Toast.LENGTH_SHORT).show();
			Log.e("USER_DENY", "The user denies to enable bluetooth functionalities");
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	

	/**
	 * Scan the possible LE devices, a single scan period is 15s
	 * @param enable
	 */
	private void scanLeDevice(final boolean enable){
		if(enable){
			mHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					isScanning = false;
					mAdapter.stopLeScan(callback);
				}
			}, SINGLE_SCAN_PERIOD);
			
			isScanning = true;
			mAdapter.startLeScan(callback);
		}else{
			isScanning = false;                   
			mAdapter.stopLeScan(callback);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		this.scanLeDevice(false);
		ble_adapter.clearDevice();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

}
