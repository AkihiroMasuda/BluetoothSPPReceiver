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

    // Bluetooth�f�o�C�X
    private BluetoothDevice btDevice;
    //�@Bluetooth�ʐM�\�P�b�g
    private BluetoothSocket btSocket;
    // Bluetooth�f�[�^��M�X���b�h 
    private Thread threadBTCom; 
    // �X���b�h�I���t���O
    boolean halt = false;
    //TextView�ɕ\�����镶����
    public String str_txtview = ""; 
    
	// ���O�o�͗pTAG
    private static final String LOG_TAG ="BT_SPP_Receiver";
    
    @Override
    public void onResume(){
    	super.onResume();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bluetooth�f�o�C�X�I��p�_�C�A���O��\��
        showBTDeviceSeclectDialog();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // �X���b�h�I���t���OON
        halt = true;
        // �X���b�h���I������������v����ă\�P�b�g�I���ƃA�v���I�������s���邽�߁A�x�����s������
        new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
		        try {
		        	// �ʐM�\�P�b�g�I��
		            btSocket.close();
		            // �A�v���I��
		            MainActivity.this.finish();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
			}
		},1000); 
    }
    
    // Bluetooth�f�o�C�X�I���_�C�A���O�\��
    private void showBTDeviceSeclectDialog(){

        // Android�[�����[�J����BT�A�_�v�^���擾
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        // �y�A�����O�ς݂̃f�o�C�X�ꗗ���擾
        final Set<BluetoothDevice> btDeviceSet = btAdapter.getBondedDevices();
 
        // �f�o�C�X�����擾���ă_�C�A���O�\���p�̕�������쐬
        Iterator<BluetoothDevice> it = btDeviceSet.iterator();
        final ArrayList<String> item_list = new ArrayList<String>();
        while(it.hasNext()){
          btDevice = it.next();
          Log.d(LOG_TAG, "btAddr = " + btDevice.getAddress());
          Log.d(LOG_TAG, "btAddr = " + btDevice.getName());
          item_list.add(btDevice.getName());
        }

        //���b�Z�[�W�_�C�A���O�̕\��
        new AlertDialog.Builder(MainActivity.this)
        .setTitle("BT�f�o�C�X�I��")
        .setItems(item_list.toArray(new String[item_list.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	//// �f�o�C�X�I�����̏���
                // �I������BluetoothDevice��ێ�
                BluetoothDevice devices[] = btDeviceSet.toArray(new BluetoothDevice[btDeviceSet.size()]);
                btDevice = devices[which];
                // Bluetooth�ʐM�������J�n
                startCommunication();
            }
        })
        .show();
    }
    
    // Bluetooth�f�o�C�X�Ƃ̒ʐM�J�n�B��ʍX�V�^�C�}�[�N���B
    private void startCommunication(){
    	// Bluetooth�f�o�C�X�Ƃ̒ʐM�m��
    	if (tryBTConnection()){
    		// �ʐM����
    		Log.d(LOG_TAG, "Connected.");
            Toast.makeText(this, btDevice.getName() + "  connected", Toast.LENGTH_SHORT).show();
            // �ʐM�X���b�h�N��
            threadBTCom = createBTComThread();
            threadBTCom.start();
            // ��ʍX�V�^�C�}�[�N��
            startDisplayUpdater();
    	}else{
    		// �ʐM���s
    		Log.e(LOG_TAG, "Bluetooth device connection if failed.");
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    	}
    }
    
    // Bluetooth�f�o�C�X�Ƃ̒ʐM�o�H�m��
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

    // Bluetooth�f�o�C�X�Ƃ̒ʐM�X���b�h�쐬
    private Thread createBTComThread(){
    	return new Thread(new Runnable() {
            @Override
            public void run() {
            	// �ʐM�X���b�h�����{��
                try {
                	while(!halt){
                		// �f�[�^��M������(���s�R�[�h���o�Ń��Z�b�g���ׂ������j
    					InputStream inStream = btSocket.getInputStream(); //�f�[�^��M�p�X�g���[��
    					int size=0; //��M�f�[�^�T�C�Y
    					int maxsize = 1024; //�ő��M�f�[�^�T�C�Y
    					final byte[] buffer = new byte[maxsize]; //��M�f�[�^�o�b�t�@

    					// ���s�R�[�h���o�Ă���܂œǂݍ��ݑ�����
    					while(!halt){
    						// �ǂݍ���
    						int rsize = inStream.read(buffer, size, 1);

							//���s���o�Ă��������f
    						final int CODE_LF = 10; //���s�R�[�h
    						size  = size + rsize; //�ǂݍ��݃T�C�Y�X�V
    						if (buffer[size-1]==CODE_LF){
    							// ���s���o�Ă�����A�\���������ҏW����while������
    							String str = new String(buffer);
    							str_txtview = str.substring(0, size) + str_txtview;
    							final int str_txtview_maxlen =  1024*20;
    							if (str_txtview.length() > str_txtview_maxlen){
    								//TextView�ɕ\�����镶���񂪒������鎞�͌�땔����؂�̂Ă�
    								str_txtview = str_txtview.substring(0, str_txtview_maxlen); 
    							}
    							// ���̃f�[�^��҂��߃f�[�^��M���������s���ׂ�break
    							break;
    						}else if (size+1 > maxsize){
        						// ���s���o�Ȃ��܂ܓǂݍ��݃T�C�Y��臒l�ȏ�ɂȂ�����G���[�Ɣ��f���ēǂݍ��ݕ���
    		                	Log.e(LOG_TAG, "Data length is too long.");
    							break;
    						}else{
    							// ���s���܂��o�Ȃ��A���A��M�f�[�^�T�C�Y�ɗ]�T����
    							// ���̃f�[�^����M����ׂ��A�����ł͉������Ȃ�
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
    
    // ��ʍX�V�^�C�}�[�N��
    private void startDisplayUpdater(){
    	// ���Ԋu���Ɏ��s�����������̒�`
		final TextView tv = (TextView)findViewById(R.id.txtMain);
		final Runnable updateUI = new Runnable(){
			@Override
			public void run() {
				// TextView�̕�������X�V
				tv.setText(str_txtview);
			}
		};
		
		// �^�C�}�[�쐬�ƋN��
		// �����Ԗ��ɉ�ʍX�V���������s
		long period = 50; //��ʍX�V�Ԋu
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
