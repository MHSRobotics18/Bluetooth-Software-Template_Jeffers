package com.jeffersetechnologies.jetbluetoothspp;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by User on 5/31/2016.
 */
public class BluetoothSPPFragment extends Fragment {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    private ImageButton mDpadButton1;
    private ImageButton mDpadButton2;
    private ImageButton mDpadButton3;
    private ImageButton mDpadButton4;
    private ImageButton mDpadButton5;
    private ImageButton mDpadButton6;
    private ImageButton mDpadButton7;
    private ImageButton mDpadButton8;
    private ImageButton mDpadButton9;
    private ImageButton mDpadButton10;

    private Button mSendButton2;
    private Button mSendButton3;
    private Button mSendButton4;
    private Button mSendButton5;
    private Button mSendButton6;
    private Button mSendButton7;
    private Button mSendButton8;
    private Button mSendButton9;
    private Button mSendButton10;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Array adapter & buffer for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothSPP mChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothSPP.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_spp, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);

        mDpadButton1 = (ImageButton) view.findViewById(R.id.dpadButton1);
        mDpadButton2 = (ImageButton) view.findViewById(R.id.dpadButton2);
        mDpadButton3 = (ImageButton) view.findViewById(R.id.dpadButton3);
        mDpadButton4 = (ImageButton) view.findViewById(R.id.dpadButton4);
        mDpadButton5 = (ImageButton) view.findViewById(R.id.dpadButton5);
        mDpadButton6 = (ImageButton) view.findViewById(R.id.dpadButton6);
        mDpadButton7 = (ImageButton) view.findViewById(R.id.dpadButton7);
        mDpadButton8 = (ImageButton) view.findViewById(R.id.dpadButton8);
        mDpadButton9 = (ImageButton) view.findViewById(R.id.dpadButton9);
        mDpadButton10 = (ImageButton) view.findViewById(R.id.dpadButton10);

        mSendButton2 = (Button) view.findViewById(R.id.remoteButton1);
        mSendButton3 = (Button) view.findViewById(R.id.remoteButton2);
        mSendButton4 = (Button) view.findViewById(R.id.remoteButton3);
        mSendButton5 = (Button) view.findViewById(R.id.remoteButton4);
        mSendButton6 = (Button) view.findViewById(R.id.remoteButton5);
        mSendButton7 = (Button) view.findViewById(R.id.remoteButton6);
        mSendButton8 = (Button) view.findViewById(R.id.remoteButton7);
        mSendButton9 = (Button) view.findViewById(R.id.remoteButton8);
        mSendButton10 = (Button) view.findViewById(R.id.remoteButton9);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mDpadButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("g");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mDpadButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("spinL");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mDpadButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("s");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mDpadButton4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("spinR");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mDpadButton5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("b");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mDpadButton6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("play");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mDpadButton7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("wave");
                }
            }
        });
        // Initialize the send button with a listener that for click events
        mDpadButton8.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("faster");
                }
            }
        });
        // Initialize the send button with a listener that for click events
        mDpadButton9.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("slower");
                }
            }
        });
        // Initialize the send button with a listener that for click events
        mDpadButton10.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd1");
                    sendMessage("play");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mSendButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd2");
                    sendMessage("- 100");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mSendButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd3");
                    sendMessage("+ 100");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mSendButton4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd4");
                    sendMessage("l 100");
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mSendButton5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd5");
                    sendMessage("r 100");
                }
            }
        });
        // Initialize the send button with a listener that for click events
        mSendButton6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd5");
                    sendMessage("blink");
                }
            }
        });
        mSendButton7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd5");
                    sendMessage("button");
                }
            }
        });
        mSendButton8.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd5");
                    sendMessage("sound");
                }
            }
        });
        mSendButton9.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd5");
                    sendMessage("motion");
                }
            }
        });
        mSendButton10.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                Context context = getActivity();
                if (null != view) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    String displayName = sharedPref.getString(SettingsActivity.DISPLAY_NAME, "cmd5");
                    sendMessage("compass");
                }
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothSPP(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothSPP.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothSPP.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothSPP.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothSPP.STATE_LISTEN:
                        case BluetoothSPP.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_spp, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

}
