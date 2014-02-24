package lv.piip.spendigitizer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.samsung.samm.common.SObjectStroke;
import com.samsung.spensdk.SCanvasView;
import com.samsung.spensdk.applistener.SCanvasInitializeListener;
import com.samsung.spensdk.applistener.SPenHoverListener;
import com.samsung.spensdk.applistener.SPenTouchListener;

public class MainActivity extends Activity {
	private SCanvasView mSCanvas;
	private Context mContext = null;
	private DatagramSocket socket;
	private byte[] dtab =new byte[50];
	private DatagramPacket pack;
	private InetAddress broadcastAddress;
	private int counter =0;
	private String signalType;
	private String upSignal="";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 
    	super.onCreate(savedInstanceState);

    	mContext = this;
        setContentView(R.layout.activity_main);
        
		mSCanvas = (SCanvasView) findViewById(R.id.canvas_view);
		mSCanvas.setSCanvasInitializeListener(mSCanvasInitializeListener);
		mSCanvas.setSPenHoverListener(mSPenHoverListener);
		mSCanvas.setSPenTouchListener(mSPenTouchListener);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Init();
    }

    private void SendSignal(float x, float y, float pressure, int action, String type)
    {
		try {
	    	if(signalType!=type)
	    	{
	    		counter=0;
	    	}
	    	
			pack.setData((Float.toString(x)+"|"+Float.toString(y)+"|"+ Float.toString(pressure)+ "|"+Integer.toString(action)+ "|"+type+ "|"+Integer.toString(counter)+"|"+upSignal).getBytes());
			upSignal = "";
			socket.send(pack);
	    	counter++;
	    	
			signalType=type;
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_exit:
            	finish();	
                break;
            case R.id.menu_clear:
            	mSCanvas.clearSCanvasView();
                break;
            case R.id.menu_settings:
            	Intent settingsActivity = new Intent(mContext,Preferences.class);
            	startActivity(settingsActivity);
                break;
        }
        return true;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    
    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager)mContext.getSystemService(WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
          quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
    
    public void Init()
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	
        try {
        	
        	if(prefs.getBoolean("fullscreen", true))
        	{
        		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	}
        	else
        	{
        		this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	}
        	
        	if(socket!=null)
        	{
        		socket.disconnect();
        	}
        	
			socket = new DatagramSocket();
			pack = new DatagramPacket(dtab,dtab.length);
        	
        	if(prefs.getBoolean("autolocate", true))
        	{
	        	broadcastAddress = getBroadcastAddress();
				socket.setBroadcast(true);
        	}
        	else
        	{
            	broadcastAddress =InetAddress.getByName(prefs.getString("host", "192.168.1.101"));
            	socket.setBroadcast(false);
        	}
			
			socket.connect(broadcastAddress, Integer.parseInt(prefs.getString("port", "12333")));
			
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
        
		mSCanvas.setBGColor(Color.BLACK);
		mSCanvas.setSettingStrokeInfo(SObjectStroke.SAMM_STROKE_STYLE_SOLID, 5, Color.DKGRAY);
		mSCanvas.clearSCanvasView();
    }
    
	SCanvasInitializeListener mSCanvasInitializeListener = new SCanvasInitializeListener() {
		public void onInitialized() {
			Init();
			Toast.makeText(mContext, "Digitizer Ready", Toast.LENGTH_SHORT).show();
		}
	};
	
	SPenHoverListener mSPenHoverListener = new SPenHoverListener(){
		public boolean onHover(View view, MotionEvent event) {
			SendSignal(event.getX(), event.getY(), event.getPressure(), event.getAction(), "hover");
			return false;
		}

		public void onHoverButtonDown(View view, MotionEvent event) {
		}

		public void onHoverButtonUp(View view, MotionEvent event) {
		}
	};
	
	SPenTouchListener mSPenTouchListener = new SPenTouchListener(){

		public boolean onTouchFinger(View view, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_UP) {
            	upSignal = "up";
            }
			
			SendSignal(event.getX(), event.getY(), event.getPressure(), event.getAction(), "finger");
			return false;
		}

		public boolean onTouchPen(View view, MotionEvent event) {
			SendSignal(event.getX(), event.getY(), event.getPressure(), event.getAction(), "pen");				
			return false;
		}

		public boolean onTouchPenEraser(View view, MotionEvent event) {
			return false;
		}		

		public void onTouchButtonDown(View view, MotionEvent event) {
		}

		public void onTouchButtonUp(View view, MotionEvent event) {
		}			
	};


	public View onCreateView(String name, Context context, AttributeSet attrs) {
		// TODO Auto-generated method stub
		return null;
	}

	public View onCreateView(View parent, String name, Context context,
			AttributeSet attrs) {
		// TODO Auto-generated method stub
		return null;
	}}