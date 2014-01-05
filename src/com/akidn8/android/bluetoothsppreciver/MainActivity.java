package com.akidn8.android.bluetoothsppreciver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// ログ出力用TAG
    private static final String LOG_TAG ="BT_Arduino";
    
    // Bluetooth通信用
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
 
    // Bluetoothデータ受信スレッド 
    private Thread threadBTCom; 
    // スレッド終了フラグ
    boolean halt = false;
 
    //TextViewに表示する文字列
    public String str_txtview = ""; 
    
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
        threadBTCom = createBTComThread(handler);
        threadBTCom.start();
        startUIUpdater(handler);
    }
    
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.activity_main, menu);
//		return true;
//	}
//
    
    @Override
    protected void onPause() {
        super.onPause();
        halt = true;
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void initBT(){

        // Android端末ローカルのBTアダプタを取得
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        // ペアリング済みのデバイス一覧を取得
        Set<BluetoothDevice> btDeviceSet = btAdapter.getBondedDevices();
 
        Iterator<BluetoothDevice> it = btDeviceSet.iterator();
 
        while(it.hasNext()){
          btDevice = it.next();
          Log.d(LOG_TAG, "btAddr = " + btDevice.getAddress());
          Log.d(LOG_TAG, "btAddr = " + btDevice.getName());
///          if (btDevice.getName().equals("EasyBT")){
          if (btDevice.getName().equals("BTCOM-SPPB")){
        	  Log.d(LOG_TAG, "BTCOM-SPPB Try connection.");
        	  if (tryBTConnection()){
            	  Log.d(LOG_TAG, "BTCOM-SPPB Connected!!!");
        		  break;
        	  }
          }
          if (btDevice.getName().equals("SBDBT-001bdc0f7281")){
//        	  Log.d(LOG_TAG, "SBDBT-001bdc0f7281 FOUND!!");
        	  Log.d(LOG_TAG, "SBDBT-001bdc0f7281 Try connection.");
        	  if (tryBTConnection()){
            	  Log.d(LOG_TAG, "SBDBT-001bdc0f7281 Connected!!!");
        		  break;
        	  }
          }
        }

    }
    
    private boolean tryBTConnection(){
        try {
			BluetoothDevice hxm = btDevice;
			Method m;
			m = hxm.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
			btSocket = (BluetoothSocket)m.invoke(hxm, Integer.valueOf(1)); 
			btSocket.connect();
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		}
        return false;
    }
    
    private Thread createBTComThread(final Handler handler){
    	
    	return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
					InputStream inStream = btSocket.getInputStream();
                	while(!halt){
    					int size=0;
    					int maxsize = 1024;
    					final byte[] buffer = new byte[maxsize];
    					while(!halt){
    						// 改行コードが出てくるまで読み込み続ける
    						if (size+1 > maxsize){
    							//これ以上は読めない
    							//エラー
    							break;
    						}
    						// 読み込み
    						int rsize = inStream.read(buffer, size, 1);

							//改行が出てきたか判断
    						final int CODE_LF = 10; //改行コード
    						size  = size + rsize;
    						if (buffer[size-1]==CODE_LF){
    							// 改行が出てきたら、表示文字列を編集してwhile抜ける
    							String str = new String(buffer);
    							str_txtview = str.substring(0, size) + str_txtview;
    							final int str_txtview_maxlen =  1024*20;
    							if (str_txtview.length() > str_txtview_maxlen){
    								//TextViewに表示する文字列が長すぎる時は後ろ部分を切り捨てる
    								str_txtview = str_txtview.substring(0, str_txtview_maxlen); 
    							}
    							break;
    						}
    					}
                	}
                	Log.d(LOG_TAG, "Thread ends.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
    }
    
    // UI描画用タイマー生成
    private void startUIUpdater(final Handler handler){
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
			}
    	}, 0, 50);
    }
	
}
