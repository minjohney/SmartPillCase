package com.example.user.sd_pill;


import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 10;
    BluetoothAdapter mBluetoothAdapter;
    int mPairedDeviceCount = 0;
    Set<BluetoothDevice> mDevices;
    BluetoothDevice mRemoteDevice;
    BluetoothSocket mSocket = null;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;

    Thread mWorkerThread = null;
    String mStrDelimiter = "\n";
    char mCharDelimiter = '\n';
    byte[] readBuffer;
    int readBufferPosition;


    protected Button btMon, btTue, btWed, btThu, btFri, btCom;
    protected ImageView img, imgAlarm;
    private int nBefore = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // 블루투스가 활성 상태로 변경됨
                    selectDevice();
                } else if (resultCode == RESULT_CANCELED) {
                    // 블루투스가 비활성 상태임
                    finish();    // 어플리케이션 종료
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // 장치가 블루투스를 지원하지 않는 경우
            finish();    // 어플리케이션 종료
        } else {
            // 장치가 블루투스를 지원하는 경우
            if (!mBluetoothAdapter.isEnabled()) {
                // 블루투스를 지원하지만 비활성 상태인 경우
                // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
                Intent enableBtIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // 블루투스를 지원하며 활성 상태인 경우
                // 페어링 된 기기 목록을 보여주고 연결할 장치를 선택
                selectDevice();
            }
        }
    }

    void selectDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDeviceCount = mDevices.size();

        if (mPairedDeviceCount == 0) {
            // 페어링 된 장치가 없는 경우
            finish();        // 어플리케이션 종료
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 장치 선택");
        builder.setIcon(R.drawable.janee_pill);

        // 페어링 된 블루투스 장치의 이름 목록 작성
        List<String> listItems = new ArrayList<String>();
        for (BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        listItems.add("취소");        // 취소 항목 추가

        final CharSequence[] items =
                listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == mPairedDeviceCount) {
                    // 연결할 장치를 선택하지 않고 ‘취소’를 누른 경우
                    finish();
                } else {
                    // 연결할 장치를 선택한 경우
                    // 선택한 장치와 연결을 시도함
                    connectToSelectedDevice(items[item].toString());
                }
            }
        });

        builder.setCancelable(false);    // 뒤로 가기 버튼 사용 금지
        AlertDialog alert = builder.create();
        alert.show();
    }

    void beginListenForData() {
        final Handler handler = new Handler();


        readBuffer = new byte[1024];    // 수신 버퍼
        readBufferPosition = 0;        // 버퍼 내 수신 문자 저장 위치

        // 문자열 수신 쓰레드
        mWorkerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int bytesAvailable = mInputStream.available();    // 수신 데이터 확인
                        if (bytesAvailable > 0) {        // 데이터가 수신된 경우
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == mCharDelimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0,
                                            encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "UTF-8");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            // 수신된 문자열 데이터에 대한 처리 작업
                                            int cds_value = Integer.parseInt(data);

                                            if (cds_value > 300) {
                                                good();
                                                GoodTakeMedicine();
                                                Handler delayHandler = new Handler();
                                                delayHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        // TODO
                                                        Exit1();
                                                    }
                                                }, 10000);

                                            }

                                            if (cds_value < 300) {
                                                bad();
                                                NoTakeMedicine();
                                                Handler delayHandler = new Handler();
                                                delayHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        // TODO
                                                        Exit2();
                                                    }
                                                }, 10000);

                                            }

                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        // 데이터 수신 중 오류 발생
                        finish();
                    }
                }
            }
        });

        mWorkerThread.start();
    }

    void sendData(String msg) {
        msg += mStrDelimiter;    // 문자열 종료 표시
        try {
            mOutputStream.write(msg.getBytes());        // 문자열 전송
        } catch (Exception e) {
            // 문자열 전송 도중 오류가 발생한 경우
            finish();        // 어플리케이션 종료
        }
    }

    BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;

        for (BluetoothDevice device : mDevices) {
            if (name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }

        return selectedDevice;
    }

    @Override
    protected void onDestroy() {
        try {
            mWorkerThread.interrupt();    // 데이터 수신 쓰레드 종료
            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
        } catch (Exception e) {
        }

        super.onDestroy();
    }

    void connectToSelectedDevice(String selectedDeviceName) {
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try {
            // 소켓 생성
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            // RFCOMM 채널을 통한 연결
            mSocket.connect();

            // 데이터 송수신을 위한 스트림 얻기
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();


            // 데이터 수신 준비
            beginListenForData();
        } catch (Exception e) {
            // 블루투스 연결 중 오류 발생
            finish();        // 어플리케이션 종료
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkBluetooth();


        imgAlarm = (ImageView) findViewById(R.id.imgAlarm);
        imgAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Alarm.class);
                startActivity(intent);
            }
        });
        img = (ImageView) findViewById(R.id.img);
        btMon = (Button) findViewById(R.id.btMon);
        btMon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRotation(nBefore + 60);
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO
                        sendData("1");
                        MondayAlarm();
                    }
                }, 1000);

            }
        });

        btTue = (Button) findViewById(R.id.btTue);
        btTue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRotation(nBefore + 60);
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO
                        sendData("2");
                        TuesAlarm();
                    }
                }, 1000);

            }
        });

        btWed = (Button) findViewById(R.id.btWed);
        btWed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRotation(nBefore + 60);
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO
                        sendData("3");
                        WedAlarm();
                    }
                }, 1000);

            }
        });

        btThu = (Button) findViewById(R.id.btThu);
        btThu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRotation(nBefore + 60);
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO
                        sendData("4");
                        ThuAlarm();
                    }
                }, 1000);

            }
        });

        btFri = (Button) findViewById(R.id.btFri);
        btFri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRotation(nBefore + 60);
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO
                        sendData("5");
                        FriAlarm();
                    }
                }, 1000);

            }
        });

        btCom = (Button) findViewById(R.id.btCom);
        btCom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRotation(nBefore - 300);
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO
                        sendData("6");
                        Complete();
                    }
                }, 1000);

            }
        });



    }


    private void Exit2() {
        NotificationManagerCompat.from(this).cancel(2);
    }

    private void bad() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.drawable.janee_pill);
        builder.setContentTitle("janee_pillManager 입니다");
        builder.setContentText("약을 드시지 않았습니다. 얼른 챙겨드세요!");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.janee_pill);
        builder.setLargeIcon(largeIcon);

        builder.setColor(Color.MAGENTA);

        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(ringtoneUri);

        long[] vibrate = {0, 100, 200, 300};
        builder.setVibrate(vibrate);
        builder.setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }
        manager.notify(2, builder.build());
    }

    private void Exit1() {
        NotificationManagerCompat.from(this).cancel(1);
    }

    private void good() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.drawable.janee_pill);
        builder.setContentTitle("janee_pillManager 입니다");
        builder.setContentText("약을 잘 복용하셨습니다!");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.janee_pill);
        builder.setLargeIcon(largeIcon);

        builder.setColor(Color.MAGENTA);

        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(ringtoneUri);

        long[] vibrate = {0, 100, 200, 300};
        builder.setVibrate(vibrate);
        builder.setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }
        manager.notify(1, builder.build());
    }

    void NoTakeMedicine() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("약을 챙겨드시지 않았습니다. 얼른 드세요");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    void GoodTakeMedicine() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("약을 오늘도 잘 챙겨드셨습니다. 최고에요");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    public void testRotation(int i)
    {
        RotateAnimation ra = new RotateAnimation(
                nBefore, i, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        ra.setDuration(1000);
        ra.setFillAfter(true);
        img.startAnimation(ra);
        nBefore = i;
    }

    void MondayAlarm() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("월요일분 약 드실 시간입니다");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Complete take medicine on Monday", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    void TuesAlarm() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("화요일분 약 드실 시간입니다");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Complete take medicine on Tuesday", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    void WedAlarm() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("수요일분 약 드실 시간입니다");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Complete take medicine on Wednesday", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    void ThuAlarm() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("목요일분 약 드실 시간입니다");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Complete take medicine on Thursday", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    void FriAlarm() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("금요일분 약 드실 시간입니다");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Complete take medicine on Friday", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    void Complete() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Jannee_pillManager");
        builder.setMessage("일주일분 약을 모두 챙겨드셨습니다");
        builder.setIcon(R.drawable.janee_pill);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Complete take medicine one week", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }


}
