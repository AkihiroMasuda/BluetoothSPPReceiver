package com.akidn8.android.bluetoothsppreciver;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
//	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

    private static final String LOG_TAG ="BT_Arduino";
    
    /* SPPで繋げる時のUUIDは決まっている模様 */
    //private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
 
    private Thread thread;
    private Timer uiTimer; //UI描画用タイマー
 
    boolean isThreadStop = false;
    
    @Override
    public void onResume(){
    	super.onResume();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
 
        initBT();
        
        Handler handler = new Handler();
        thread = createMyThread(handler);
        thread.start();
//        uiTimer = new Timer();
        uiTimer = createUITimer(handler);
    }

 
    private void initBT(){

        // Android端末ローカルのBTアダプタを取得
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        // ペアリング済みのデバイス一覧を取得
        Set<BluetoothDevice> btDeviceSet = btAdapter.getBondedDevices();
 
        Iterator<BluetoothDevice> it = btDeviceSet.iterator();
 
        while(it.hasNext()){
          btDevice = it.next();
          Log.e(LOG_TAG, "btAddr = " + btDevice.getAddress());
          Log.e(LOG_TAG, "btAddr = " + btDevice.getName());
///          if (btDevice.getName().equals("EasyBT")){
          if (btDevice.getName().equals("BTCOM-SPPB")){
        	  Log.e(LOG_TAG, "BTCOM-SPPB Try connection.");
        	  if (tryBTConnection()){
            	  Log.e(LOG_TAG, "BTCOM-SPPB Connected!!!");
        		  break;
        	  }
          }
          if (btDevice.getName().equals("SBDBT-001bdc0f7281")){
//        	  Log.e(LOG_TAG, "SBDBT-001bdc0f7281 FOUND!!");
        	  Log.e(LOG_TAG, "SBDBT-001bdc0f7281 Try connection.");
        	  if (tryBTConnection()){
            	  Log.e(LOG_TAG, "SBDBT-001bdc0f7281 Connected!!!");
        		  break;
        	  }
          }
        }

    }
    
    private boolean tryBTConnection(){
        try {
        	
        	if (false){
                // RFCOMM用のSocketを生成
        		// こっちだとモジュールによって失敗する
                btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
                btSocket.connect();
        	}else{
                // StuckOverFlowにあった解決法
        		// これだと成功。意味はわからん
//              BluetoothDevice hxm = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device.getAddress());
              BluetoothDevice hxm = btDevice;
              Method m;
              m = hxm.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
              btSocket = (BluetoothSocket)m.invoke(hxm, Integer.valueOf(1)); 
              btSocket.connect();
        	} 
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		}
        return false;
    }
    
    
    public String str = "";
    public String str_txtview = "";
    
    private Thread createMyThread(final Handler handler){
    	
//		final TextView tv = (TextView)findViewById(R.id.txtMain);
//		final Runnable updateUI = new Runnable(){
//			@Override
//			public void run() {
//				tv.setText(str_txtview);
//			}
//		};
    	
    	return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                	while(true){
    					Log.e(LOG_TAG, "while1...top");
    					InputStream inStream = btSocket.getInputStream();
//    					int size=inStream.read();
    					int size=0;
    					int maxsize = 1024;
    					final byte[] buffer = new byte[maxsize];
    					Log.e(LOG_TAG, "while1...");
    					while(true){
//    						int rsize = inStream.read(buffer, size, maxsize-size);
    						if (size+1 > maxsize){
    							//これ以上は読めない
    							//エラー
    							break;
    						}
    						int rsize = inStream.read(buffer, size, 1);
    						final int CODE_LF = 10; //改行コード
    						size  = size + rsize;
    						if (buffer[size-1]==CODE_LF){
    							//改行が出てきたら、出力
    							DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
    							str = new String(buffer);
    							String ss = str.substring(0, size);
    							str_txtview = ss + str_txtview;
//    							str_txtview = "1234567890\n" + str_txtview;
    							final int str_txtview_maxlen =  1024*20;
    							if (str_txtview.length() > str_txtview_maxlen){
    								//長いので程々にカット
    								str_txtview = str_txtview.substring(0, str_txtview_maxlen); 
    							}
    							// UI描画命令
//    							handler.removeCallbacks(updateUI);
//    							handler.post(updateUI);
//    							Thread.sleep(5);
    							break;
    						}
    					}
                		
                	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//                catch (InterruptedException e){
//					e.printStackTrace();
//				}
            }
        });
    }
    
    // UI描画用タイマー生成
    private Timer createUITimer(final Handler handler){
		final TextView tv = (TextView)findViewById(R.id.txtMain);
		final Runnable updateUI = new Runnable(){
			@Override
			public void run() {
				tv.setText(str_txtview);
			}
		};
    	Timer timer = new Timer();
    	timer.schedule(new TimerTask(){
			@Override
			public void run() {
				handler.removeCallbacks(updateUI);
				handler.post(updateUI);
//				tv.setText(str_txtview);
			}
    	}, 0, 50);
    	return timer;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isThreadStop = true;
        thread.stop();
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	
}
