package activity;
import java.util.ArrayList;
import java.util.List;

import com.rondo.ble.R;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


@SuppressLint("InflateParams")
public class BLEDeviceAdapter extends BaseAdapter {
	
	private List<BluetoothDevice> devices;
	private static LayoutInflater inflater;
	
	public BLEDeviceAdapter(Context context){
		devices = new ArrayList<BluetoothDevice>();
		inflater = LayoutInflater.from(context);
	}
	
	public void addDevice(BluetoothDevice device){
		if(!devices.contains(device)){
		this.devices.add(device);
		}
	}
	
	public void clearDevice(){
		this.devices.clear();
	}

	@Override
	public int getCount() {
		return devices.size();
	}

	@Override
	public Object getItem(int position) {
		return devices.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if(convertView == null){
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.device_item,null);
			holder.device_name = (TextView) convertView.findViewById(R.id.device_name);
			holder.device_addr = (TextView) convertView.findViewById(R.id.device_addr);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		String name = devices.get(position).getName();
		String addr = devices.get(position).getAddress();
		holder.device_name.setText(name);
		holder.device_addr.setText(addr);
		return convertView;
	}
	
	class ViewHolder{
		TextView device_name;
		TextView device_addr;
	}

}
