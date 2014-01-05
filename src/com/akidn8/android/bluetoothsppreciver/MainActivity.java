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

    // Bluetoothデバイス
    private BluetoothDevice btDevice;
    //　Bluetooth通信ソケット
    private BluetoothSocket btSocket;
    // Bluetoothデータ受信スレッド 
    private Thread threadBTCom; 
    // スレッド終了フラグ
    boolean halt = false;
    //TextViewに表示する文字列
    public String str_txtview = ""; 
    
	// ログ出力用TAG
    private static final String LOG_TAG ="BT_SPP_Receiver";
    
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
        // スレッド終了フラグON
        halt = true;
        // スレッドが終わった頃を見計らってソケット終了とアプリ終了を実行するため、遅延実行させる
        new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
		        try {
		        	// 通信ソケット終了
		            btSocket.close();
		            // アプリ終了
		            MainActivity.this.finish();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
			}
		},1000); 
    }
    
    // Bluetoothデバイス選択ダイアログ表示
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

        //メッセージダイアログの表示
        new AlertDialog.Builder(MainActivity.this)
        .setTitle("BTデバイス選択")
        .setItems(item_list.toArray(new String[item_list.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	//// デバイス選択時の処理
                // 選択したBluetoothDeviceを保持
                BluetoothDevice devices[] = btDeviceSet.toArray(new BluetoothDevice[btDeviceSet.size()]);
                btDevice = devices[which];
                // Bluetooth通信処理を開始
                startCommunication();
            }
        })
        .show();
    }
    
    // Bluetoothデバイスとの通信開始。画面更新タイマー起動。
    private void startCommunication(){
    	// Bluetoothデバイスとの通信確立
    	if (tryBTConnection()){
    		// 通信成功
    		Log.d(LOG_TAG, "Connected.");
            Toast.makeText(this, btDevice.getName() + "  connected", Toast.LENGTH_SHORT).show();
            // 通信スレッド起動
            threadBTCom = createBTComThread();
            threadBTCom.start();
            // 画面更新タイマー起動
            startDisplayUpdater();
    	}else{
    		// 通信失敗
    		Log.e(LOG_TAG, "Bluetooth device connection if failed.");
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    	}
    }
    
    // Bluetoothデバイスとの通信経路確立
    private boolean tryBTConnection(){
        try {
			BluetoothDevice hxm = btDevice;
			Method m;
			m = hxm.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
			btSocket = (BluetoothSocket)m.invoke(hxm, Integer.valueOf(1)); 
			btSocket.connect();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
        return false;
    }

    // Bluetoothデバイスとの通信スレッド作成
    private Thread createBTComThread(){
    	return new Thread(new Runnable() {
            @Override
            public void run() {
            	// 通信スレッド処理本体
                try {
                	while(!halt){
                		// データ受信初期化(改行コード検出でリセットすべき処理）
    					InputStream inStream = btSocket.getInputStream(); //データ受信用ストリーム
    					int size=0; //受信データサイズ
    					int maxsize = 1024; //最大受信データサイズ
    					final byte[] buffer = new byte[maxsize]; //受信データバッファ

    					// 改行コードが出てくるまで読み込み続ける
    					while(!halt){
    						// 読み込み
    						int rsize = inStream.read(buffer, size, 1);

							//改行が出てきたか判断
    						final int CODE_LF = 10; //改行コード
    						size  = size + rsize; //読み込みサイズ更新
    						if (buffer[size-1]==CODE_LF){
    							// 改行が出てきたら、表示文字列を編集してwhile抜ける
    							String str = new String(buffer);
    							str_txtview = str.substring(0, size) + str_txtview;
    							final int str_txtview_maxlen =  1024*20;
    							if (str_txtview.length() > str_txtview_maxlen){
    								//TextViewに表示する文字列が長すぎる時は後ろ部分を切り捨てる
    								str_txtview = str_txtview.substring(0, str_txtview_maxlen); 
    							}
    							// 次のデータを待つためデータ受信初期化を行うべくbreak
    							break;
    						}else if (size+1 > maxsize){
        						// 改行が出ないまま読み込みサイズが閾値以上になったらエラーと判断して読み込み放棄
    		                	Log.e(LOG_TAG, "Data length is too long.");
    							break;
    						}else{
    							// 改行がまだ出ない、且つ、受信データサイズに余裕あり
    							// 次のデータを受信するべく、ここでは何もしない
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
    
    // 画面更新タイマー起動
    private void startDisplayUpdater(){
    	// 一定間隔毎に実行したい処理の定義
		final TextView tv = (TextView)findViewById(R.id.txtMain);
		final Runnable updateUI = new Runnable(){
			@Override
			public void run() {
				// TextViewの文字列を更新
				tv.setText(str_txtview);
			}
		};
		
		// タイマー作成と起動
		// 一定期間毎に画面更新処理を実行
		long period = 50; //画面更新間隔
		final Handler handler = new Handler();
    	Timer timer = new Timer();
    	timer.schedule(new TimerTask(){
			@Override
			public void run() {
				handler.removeCallbacks(updateUI);
				handler.post(updateUI);
			}
    	}, 0, period);
    }
	
}
