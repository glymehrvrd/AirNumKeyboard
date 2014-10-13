package com.glyme.airnumkeyboard;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
	
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment;
			switch (position) {
			case 0:
				fragment = new MainSectionFragment();
				break;
			case 1:
				fragment = new HistorySectionFragment();
				break;
			default:
				fragment = new MainSectionFragment();
			}
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			}
			return null;
		}
	}

	public final static int UPDATE_SERVER_IP = 1;

	private TextView connStatus;
	private Socket sock = null;
	private OutputStream out = null;
	private EditText edtIP;
	SensorManager sm;

	private long lastTime = 0;
	private float last_x = 0;
	private float last_y = 0;
	private float last_z = 0;
	private long lastShakeTime = 0;

	Handler serverDetectedHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MainActivity.UPDATE_SERVER_IP:
				edtIP.setText(msg.getData().getString("ip"));
				break;
			}
			super.handleMessage(msg);
		}
	};

	Runnable serverDetector = new Runnable() {

		@Override
		public void run() {
			DatagramSocket dataSocket;
			try {
				dataSocket = new DatagramSocket(7732);

				// send magic word
				byte[] sendByte = "ankc".getBytes();
				DatagramPacket sdataPacket = new DatagramPacket(sendByte, sendByte.length,
						InetAddress.getByName("255.255.255.255"), 7732);
				dataSocket.send(sdataPacket);

				// receive magic word
				while (true) {
					byte[] receiveByte = new byte[4];
					byte[] magicByte = "anks".getBytes();
					DatagramPacket rdataPacket = new DatagramPacket(receiveByte, receiveByte.length);
					dataSocket.receive(rdataPacket);
					if (Arrays.equals(receiveByte, magicByte)) {
						Message msg = new Message();
						msg.what = MainActivity.UPDATE_SERVER_IP;
						Bundle b = new Bundle();
						b.putString("ip", rdataPacket.getAddress().getHostAddress());
						msg.setData(b);
						serverDetectedHandler.sendMessage(msg);
						dataSocket.close();
						return;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private SensorEventListener mySensorListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {
				// ��ȡ��ǰʱ�̵ĺ�����
				long curTime = System.currentTimeMillis();

				// interval between two shakes must be greater than 1.3s
				if ((lastShakeTime != 0) && (curTime - lastShakeTime < 2000))
					return;

				// ��ȡ���ٶȴ���������������
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];

				float shake = 0;
				// 100������һ��
				if ((curTime - lastTime) > 100) {
					// ���ǲ��Ǹտ�ʼ�ζ�
					if (!(last_x == 0.0f && last_y == 0.0f && last_z == 0.0f))
						// ���λζ�����
						shake = (Math.abs(x - last_x) + Math.abs(y - last_y) + Math.abs(z - last_z))
								/ (curTime - lastTime) * 10000;

					// �ж��Ƿ�Ϊҡ��
					Log.v("MYTAG", String.format("x:%f y:%f z:%f", x-last_x,y-last_y,z-last_z));
					if (shake > 1200) {
						lastTime = 0;
						last_x = 0;
						last_y = 0;
						last_z = 0;
						Toast.makeText(getApplicationContext(), "shake!", Toast.LENGTH_SHORT).show();
						((Button) findViewById(R.id.button10)).performClick();
						lastShakeTime = System.currentTimeMillis();
					}
					last_x = x;
					last_y = y;
					last_z = z;

					lastTime = curTime;
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	private View.OnClickListener btn_click_listener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			char sender = 0;
			switch (v.getId()) {
			case R.id.button1:
				sender = 'n';
				break;
			case R.id.button2:
				sender = '/';
				break;
			case R.id.button3:
				sender = '*';
				break;
			case R.id.button4:
				sender = '-';
				break;
			case R.id.button5:
				sender = '7';
				break;
			case R.id.button6:
				sender = '8';
				break;
			case R.id.button7:
				sender = '9';
				break;
			case R.id.button8:
				sender = '+';
				break;
			case R.id.button9:
				sender = '4';
				break;
			case R.id.button10:
				sender = '5';
				break;
			case R.id.button11:
				sender = '6';
				break;
			case R.id.button12:
				sender = '1';
				break;
			case R.id.button13:
				sender = '2';
				break;
			case R.id.button14:
				sender = '3';
				break;
			case R.id.button15:
				sender = 'e';
				break;
			case R.id.button16:
				sender = '0';
				break;
			case R.id.button17:
				sender = '.';
				break;
			case R.id.button18:
				sender = 's';
				break;
			default:
				break;
			}
			if (out != null) {
				try {
					out.write(sender);
					out.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow network request in main thread
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		setContentView(R.layout.activity_main);

		connStatus = (TextView) findViewById(R.id.textView2);
		edtIP = (EditText) findViewById(R.id.editText1);
		Button btnConn = (Button) findViewById(R.id.button19);

		// start a thread to detect server
		new Thread(serverDetector).start();

		// bind listener for buttons
		((Button) findViewById(R.id.button1)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button2)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button3)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button4)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button5)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button6)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button7)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button8)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button9)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button10)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button11)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button12)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button13)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button14)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button15)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button16)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button17)).setOnClickListener(btn_click_listener);
		((Button) findViewById(R.id.button18)).setOnClickListener(btn_click_listener);

		// button connect listener
		btnConn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// register sensor listener
				sm = (SensorManager) MainActivity.this.getSystemService(Context.SENSOR_SERVICE);
				int sensorType = android.hardware.Sensor.TYPE_ACCELEROMETER;
				sm.registerListener(mySensorListener, sm.getDefaultSensor(sensorType),
						SensorManager.SENSOR_DELAY_FASTEST);

				// connect to specified server
				String ip = edtIP.getText().toString();

				if (ip == "") {
					connStatus.setText("Input IP address first!");
					return;
				} else if (!ip.matches("(\\d+\\.){3}\\d+")) {
					connStatus.setText("IP address is incorrect!");
					return;
				} else {
					try {
						sock = new Socket(ip, 7732);
						out = sock.getOutputStream();
						connStatus.setText("Connection succeed.");
					} catch (Exception e) {
						connStatus.setText(String.format("Cannot connect to %s", ip));
						e.printStackTrace();
						return;
					}
				}
			}

		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// the system volume change happens here
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			return true;
		} else
			return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// the beep happens here
		// use volume up key to perform '1'
		// volume down key to perform 'enter'
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			((Button) findViewById(R.id.button15)).performClick();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			((Button) findViewById(R.id.button12)).performClick();
			return true;
		} else
			return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void finish() {
		// release all resources
		if (sock != null) {
			try {
				out.write('q');
				out.close();
				sock.close();
				sm.unregisterListener(mySensorListener);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		super.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
