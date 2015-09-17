package activity;
import com.rondo.ble.R;

import service.BLEService;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class DataControlActivity extends Activity {
	
	private TextView status,data,device,serial;
	private Button send_btn;
	private boolean isConnected = false;
	private boolean isSerialReady = false;
	
	private String mDeviceName,mDeviceAddr;
	
	public static final String DEVICE_NAME = "ble.device_name";
	public static final String DEVICE_ADDR = "ble.device_addr";
	
	private BluetoothGattCharacteristic characteristicTX, characteristicRX;
	private BLEService mService;
	
	private final BroadcastReceiver mReciever = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(BLEService.ACTION_GATT_CONNECTED)){
				updateStatus();
			}else if(action.equals(BLEService.ACTION_GATT_DISCONNECTED)){
				updateStatus();
			}else if(action.equals(BLEService.ACTION_DATA_AVAILABLE)){
				displayData(intent.getStringExtra(BLEService.EXTRA_DATA));
			}else if(action.equals(BLEService.ACTION_GATT_SERVICES_DISCOVERED)){
				 BluetoothGattService gattService = mService.getSoftSerialService();
				 Toast.makeText(DataControlActivity.this, "SERIAL_SERVICE_DISCOVERED", Toast.LENGTH_SHORT).show();
	             if (gattService == null) {
	                 Toast.makeText(DataControlActivity.this, "SERIAL_SERVICE_UNAVAILABLE", Toast.LENGTH_SHORT).show();
	                 return;
	              }
	              if(mDeviceName.startsWith("Microduino")) {
	                 characteristicTX = gattService.getCharacteristic(BLEService.UUID_MD_RX_TX);
	              }else if(mDeviceName.startsWith("EtOH")) {
	                 characteristicTX = gattService.getCharacteristic(BLEService.UUID_ETOH_RX_TX);
	              }
	              characteristicRX = characteristicTX;
	              if (characteristicTX != null) {
	                   serial.setText("Serial ready");
	                   isSerialReady = true;
	                   send_btn.setText("Start Collect data");
	               } else {
	                   serial.setText("Serial can't be found");
	               }
			}
		}
	};
	
	/**
	 * ServiceConnection is used for manage the lifecycle of a service
	 */
	private final ServiceConnection mConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((BLEService.LocalBinder) service).getService();
			if(!mService.initBLEService()){
				Log.e("INIT_SERVICE_ERROR", "Unable to initialize  the BLEService");
			}
			mService.connect(mDeviceAddr);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			Log.e("SERVICE_DISCONECTED", "---");
		}
		
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data_layout);
		mDeviceName = getIntent().getStringExtra(DEVICE_NAME);
		mDeviceAddr = getIntent().getStringExtra(DEVICE_ADDR);
		Intent start_service = new Intent(this,BLEService.class);
		this.bindService(start_service, mConnection, BIND_AUTO_CREATE);
		initViews();
//		sendMessage(BLEService.EXTRA_DATA);
	}
	
	
	 private static IntentFilter makeGattUpdateIntentFilter() {
	     final IntentFilter intentFilter = new IntentFilter();
	     intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
	     intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
	     intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
	     intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
	     return intentFilter;
	}
	 
	private void initViews(){
		status = (TextView) findViewById(R.id.conn_status);
		data = (TextView) findViewById(R.id.data_field);
		device = (TextView) findViewById(R.id.device_name);
		device.setText(mDeviceName);
		serial = (TextView) findViewById(R.id.serial);
		send_btn = (Button) findViewById(R.id.start_collect);
		send_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isSerialReady){
					sendMessage("");
				}else{
					Toast.makeText(getApplicationContext(), "Please Wait for serial complete", 
							Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	private void updateStatus(){
		if(isConnected){
			status.setText("Disconnected");
			isConnected = false;
		}else{
			status.setText("Connected");
			isConnected = true;
		}
	}
	
	private void displayData(String _data){
		data.setText(_data);
	}

	@Override
	protected void onDestroy() {
		this.unbindService(mConnection);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		this.unregisterReceiver(mReciever);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.registerReceiver(mReciever, makeGattUpdateIntentFilter());
		if(mService != null){
			final boolean result = mService.connect(mDeviceAddr);
			Log.i("CONN_RESULT", String.valueOf(result));
		}
	}
	
    private void sendMessage(String msg) {
        if ( isSerialReady && 
        		(mService != null) && (characteristicTX != null) && (characteristicRX != null)) {
            characteristicTX.setValue(msg);
            mService.writeCharacteristic(characteristicTX);
            mService.setCharacteristicNotification(characteristicRX, true);
        } else {
            Toast.makeText(DataControlActivity.this, "BLE Disconnected", Toast.LENGTH_SHORT).show();
        }
    }
    
    
}
