package service;

import java.util.UUID;

import activity.SampleGattAttributes;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BLEService extends Service {
	
	private static final int STATE_CONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_DISCONNECTED = 2;
	private int CURRENT_STATE = STATE_DISCONNECTED;
	private String currentAddr;
	
	public final static String ACTION_GATT_CONNECTED =
            "ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "ble.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "ble.EXTRA_DATA";
    public final static UUID UUID_SOFT_SERIAL_SERVICE = UUID.fromString(SampleGattAttributes.SOFT_SERIAL_SERVICE);
    public final static UUID UUID_MD_RX_TX = UUID.fromString(SampleGattAttributes.MD_RX_TX);
    public final static UUID UUID_ETOH_RX_TX = UUID.fromString(SampleGattAttributes.ETOH_RX_TX);
    
    private BluetoothManager mManager;
    private BluetoothAdapter mAdapter;
    private final IBinder mBinder = new LocalBinder();
    private BluetoothGatt mGatt;
    private final BluetoothGattCallback mCallback = new BluetoothGattCallback(){
        
    	
    	//The characteristic is changed
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			 updateBroadCast(ACTION_DATA_AVAILABLE,characteristic);
		}
        
		//Each time a characteristic is read
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if(status == BluetoothGatt.GATT_SUCCESS){
				 updateBroadCast(ACTION_DATA_AVAILABLE,characteristic);
				 Log.i("DATA_READ","receieved data");
			}
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			String broadcast_action;
			if(newState == BluetoothProfile.STATE_CONNECTED){
				CURRENT_STATE = STATE_CONNECTED;
				broadcast_action = ACTION_GATT_CONNECTED;
				updateBroadCast(broadcast_action);
				Log.i("CONN_INFO", "Connected to GATT server.");
	            mGatt.discoverServices();
			}
			if(newState == BluetoothProfile.STATE_DISCONNECTED){
				broadcast_action = ACTION_GATT_DISCONNECTED;
				CURRENT_STATE = STATE_DISCONNECTED;
				updateBroadCast(broadcast_action);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			 if (status == BluetoothGatt.GATT_SUCCESS) {
	                updateBroadCast(ACTION_GATT_SERVICES_DISCOVERED);
	                Log.w("Service_Discovered", "onServicesDiscovered received: " + status);
	            } else {
	                Log.w("Service_Discovered", "onServicesDiscovered received: " + status);
	            }
		}
    	
    };
    
    public boolean connect(final String address){
    	if(address == null){
    		Log.e("CONNECTION_ERROR", "You must specify an address");
    		return false;
    	}
    	if(mAdapter == null){
    		Log.e("CONNECTION_ERROR", "Adapter unavailable");
    	}
    	
    	if(currentAddr != null && mGatt != null 
    			&& currentAddr.equals(address) ){
    		if(mGatt.connect()){
    			Log.i("CONNECTION", "Trying to reconnect");
    			CURRENT_STATE = STATE_CONNECTING;
    			return true;
    		}else{
    			final BluetoothDevice device = mAdapter.getRemoteDevice(address);
                mGatt = device.connectGatt(this, false, mCallback);
                currentAddr = address;
                return false;
    		}
    	}
    	
    	final BluetoothDevice device = mAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w("CONN_ERROR","Device out of range");
            return false;
        }
        mGatt = device.connectGatt(this, false, mCallback);
        Log.d("CONN_INFO", "Trying to create a new connection.");
        currentAddr = address;
        CURRENT_STATE = STATE_CONNECTING;
        return true;
    }
	
    
    /**
     * Initialize the bluetooth adapter
     */
    public boolean initBLEService(){
		if(mManager == null){
		   mManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
		   if(mManager == null){
			   Log.e("INIT_FAILED", "Unable to start BluetoothManager");
			   return false;
		   }
		}
		mAdapter = mManager.getAdapter();
		if(mAdapter == null){
			  Log.e("INIT_FAILED", "Unable to start BluetoothAdapter");
			  return false;
		}
		return true;
	}
    
    /**
     * Simply notify the activity the connection state is changed.
     * @param action
     */
    private void updateBroadCast(final String action){
    	final Intent intent = new Intent(action);
    	sendBroadcast(intent);
    }
    
    /**
     * Read characteristics from peripheral devices and send broadcast to activity
     * @param action action of broadcast intent filter
     * @param charac characteristic read from the connection channel
     */
    private void updateBroadCast(final String action,final BluetoothGattCharacteristic charac){
    	final Intent intent = new Intent(action);
    	final byte[] data = charac.getValue();
        Log.i("DATA_IN_BYTE", "data: " + charac.getValue());
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar)); //HEX TO STRING
            Log.d("DATA_STRING", String.format("%s", new String(data)));
            intent.putExtra(EXTRA_DATA, String.format("%s", new String(data)));
        }
        sendBroadcast(intent);
    }
    
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mAdapter == null || mGatt == null) {
            return;
        }
        mGatt.readCharacteristic(characteristic);
    }
    
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
      if (mAdapter == null || mGatt == null) {
         Log.w("WRITE_CHARACTERISTIC", "BluetoothAdapter not initialized");
         return;
      }
        mGatt.writeCharacteristic(characteristic);
    }
    
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
            boolean enabled) {
        if (mAdapter == null || mGatt == null) {
        Log.w("ERROR", "BluetoothAdapter not initialized");
        return;
       }
       mGatt.setCharacteristicNotification(characteristic, enabled);
	   if (UUID_MD_RX_TX.equals(characteristic.getUuid()) ||
			   UUID_ETOH_RX_TX.equals(characteristic.getUuid())) {
           BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
           UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
           descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
           mGatt.writeDescriptor(descriptor);
        }
     }
    
    /**
     * Returns the binder object to activity
     * mBinder has a type of LocalBinder
     */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent){
		closeBLEService();
		return super.onUnbind(intent);
	}
	
	public void closeBLEService(){
		if(mGatt == null){
			return;
		}
		mGatt.close();
		mGatt = null;
	}
	
	public class LocalBinder extends Binder{
		public BLEService getService(){
			return BLEService.this;
		}
	}
	
	public BluetoothGattService getSoftSerialService() {
	   BluetoothGattService _service = mGatt.getService(UUID_SOFT_SERIAL_SERVICE);
	   if (_service == null) {
	   Log.d("SERVICE_FOUND_ERROR", "Soft Serial Service not found!");
	      return null;
	   }
	   return _service;
	 }

}
