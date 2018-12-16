package com.example.shrungbhatt.nsddemo;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private final String SERVICE_TYPE = "_pnpcontroller._tcp.";

    private RegistrationListener mRegistrationListener;
    private DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager;
    private String mServiceName;
    private NsdServiceInfo mService;
    private ResolveListener mResolveListener;
    private Button mRegisterServicesButton;
    private Button mDiscoverServicesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        initViews();



    }

    private void initViews(){

        mDiscoverServicesButton = findViewById(R.id.discover_services);
        mDiscoverServicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverServices();

            }
        });


        mRegisterServicesButton = findViewById(R.id.register_services);
        mRegisterServicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mNsdManager != null) {

                    try {
                        ServerSocket serverSocket = new ServerSocket(0);
                        registerService(serverSocket.getLocalPort());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            }
        });
    }

    public void registerService(int port){
        //Create the NSDServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        serviceInfo.setServiceName("PnpController");

        //This parameter is used to decide which protocol to use for transport layer
        //syntax is "_<protocol>._<transportlayer>"
        serviceInfo.setServiceType(SERVICE_TYPE);

        serviceInfo.setPort(port);

        initializeRegistrationListener();


        if(mNsdManager != null){
            mNsdManager.registerService(serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,mRegistrationListener);

        }else{
            Log.e(TAG,"NsdManager is null");
        }

    }

    public void initializeRegistrationListener(){

        mRegistrationListener = new RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG,"Service Registration failed:: " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG,"Service Un-registration failed:: " + errorCode);


            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.e(TAG,"Service Registration Successful:: ");
                mServiceName= serviceInfo.getServiceName();
                if(mServiceName != null){

                    Toast.makeText(getApplicationContext(),
                            "Service " + mServiceName +
                    " registered",Toast.LENGTH_SHORT).show();
                    discoverServices();

                }

            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.e(TAG,"Service Un-registration successful:: ");


            }
        };


    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("PnpController")){
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                if(mNsdManager != null) {
                    mNsdManager.stopServiceDiscovery(this);
                }
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                if(mNsdManager != null) {
                    mNsdManager.stopServiceDiscovery(this);
                }
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();

                Toast.makeText(getApplicationContext(),
                        "host: " + host + "\n" +
                "port: " + port,Toast.LENGTH_SHORT).show();

                Log.e(TAG,"port: " + port);
                Log.e(TAG,"host: " + host);

            }
        };
    }

    private void discoverServices(){
        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,mDiscoveryListener);
    }

    //In your application's Activity

    @Override
    protected void onPause() {
        if (mNsdManager != null) {
            tearDown();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        tearDown();
        super.onDestroy();
    }

    // NsdHelper's tearDown method
    public void tearDown() {
        mNsdManager.unregisterService(mRegistrationListener);
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }


}
