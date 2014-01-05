package com.akidn8.android.bluetoothsppreciver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// ログ出力用TAG
    private static final String LOG_TAG ="BT_SPP_Receiver";
    
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

        //Bluetoothデバイス選択用ダイアログを表示
        showBTDeviceSeclectDialog();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // スレッド終了フラグ
        halt = true;
        // スレッドが終わった頃を見計らってソケット終了とアプリ終了を実行
        new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
		        try {
		            btSocket.close();
		            MainActivity.this.finish();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
			}
		},1000); 
    }
    
    private void showBTDeviceSeclectDialog(){

        // Android端末ローカルのBTアダプタを取得
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        // ペアリング済みのデバイス一覧を取得
        final Set<BluetoothDevice> btDeviceSet = btAdapter.getBondedDevices();
 
        // デバイス名を取得してダイアログ表示用の文字列を作成
        Iterator<BluetoothDevice> it = btDeviceSet.iterator();
        final ArrayList<String> item_list = new ArrayList<String>();
        while(it.hasNext()){
          btDevice = it.next();
          Log.d(LOG_TAG, "btAddr = " + btDevice.getAddress());
          Log.d(LOG_TAG, "btAddr = " + btDevice.getName());
          
          item_list.add(btDevice.getName());
        }

        //
        //処理：メッセージダイアログの表示
        //
        new AlertDialog.Builder(MainActivity.this)
        .setTitle("BTデバイス選択")
        .setItems(item_list.toArray(new String[item_list.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // 選択したBluetoothDeviceを保持
                BluetoothDevice devices[] = btDeviceSet.toArray(new BluetoothDevice[btDeviceSet.size()]);
                btDevice = devices[which];
                // Bluetooth通信処理を開始
                startCommunication();
            }
        })
        .show();
    }
    
    private void startCommunication(){
    	if (tryBTConnection()){
    		Log.d(LOG_TAG, "Connected.");
    		
            Handler handler = new Handler();
            threadBTCom = createBTComThread(handler);
            threadBTCom.start();
            startUIUpdater(handler);
    	}else{
    		Log.e(LOG_TAG, "Bluetooth device connection if failed.");
    	}
    	
    }
    
    private boolean tryBTConnection(){
        try {
			BluetoothDevice hxm = btDevice;
			Method m;
			m = hxm.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
			btSocket = (BluetoothSocket)m.invoke(hxm, Integer.valueOf(1)); 
			btSocket.connect();
            Toast.makeText(this, btDevice.getName() + "  connected", Toast.LENGTH_SHORT).show();
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
                	while(!halt){
                		if (btSocket!=null){
        					InputStream inStream = btSocket.getInputStream();
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
