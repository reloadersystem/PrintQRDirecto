package com.example.resembrinkcorrea.printqrresembrink;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CONVERT_PNG";
    ;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    OutputStream outputStream;
    InputStream inputStream;
    Thread thread;
    Bitmap bitmap;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    ImageView ivQRCode;

    TextView lblPrinterName;
    byte FONT_TYPE;

    TextView textBox;

    private static BluetoothSocket btsocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        Button btnPrint = (Button) findViewById(R.id.btnPrint);


        textBox = (EditText) findViewById(R.id.txtText);


        lblPrinterName = (TextView) findViewById(R.id.lblPrinterName);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    FindBluetoothDevice();
                    openBluetoothPrinter();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    disconnectBT();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    printData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void FindBluetoothDevice() {

        try {

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                lblPrinterName.setText("No Bluetooth Adapter found");
            }
            if (bluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 0);
            }

            Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();

            if (pairedDevice.size() > 0) {
                for (BluetoothDevice pairedDev : pairedDevice) {

                    // My Bluetoth printer name is BTP_F09F1A
                    if (pairedDev.getName().equals("MTP-2")) {
                        bluetoothDevice = pairedDev;
                        lblPrinterName.setText("Bluetooth Printer Attached: " + pairedDev.getName());
                        break;
                    }
                }
            }

            lblPrinterName.setText("Bluetooth Printer Attached");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    // Open Bluetooth Printer

    private void openBluetoothPrinter() throws IOException {
        try {

            //Standard uuid from string //


           // beginListenData();
            try {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                String msg = "QRPARKING FACIL";

                BitMatrix bitMatrix = multiFormatWriter.encode(msg, BarcodeFormat.QR_CODE, 200, 200);  //2000, 2000
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                bitmap = barcodeEncoder.createBitmap(bitMatrix);

                UUID uuidSting = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuidSting);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();

                byte[] printformat = new byte[]{0x1B,0x21,0x03};
                outputStream.write(printformat);
                printCustom("Parking Facil",2,1);
                printCustom("QR Imprimir",0,1);
                printPhoto(R.drawable.codeqr, bitmap);
                // printPhoto(R.drawable.ic_icon_pos);
                printCustom("Control Parking",0,1);
                printCustom("PLACA: TUWQE",0,1);
                printCustom("28/09/2019 21:09:12",0,1);
                printCustom(new String(new char[32]).replace("\0", "."),0,1);
                printNewLine();
                printCustom("Iniciando Pruebas",0,1);
                printCustom("Gracias por su Preferencia",0,1);
                printNewLine();
                printNewLine();


                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception ex) {

        }
    }

    private void beginListenData() {
        try {

            final Handler handler = new Handler();
            final byte delimiter = 10;
            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int byteAvailable = inputStream.available();
                            if (byteAvailable > 0) {
                                byte[] packetByte = new byte[byteAvailable];
                                inputStream.read(packetByte);

                                for (int i = 0; i < byteAvailable; i++) {
                                    byte b = packetByte[i];
                                    if (b == delimiter) {
                                        byte[] encodedByte = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedByte, 0,
                                                encodedByte.length
                                        );
                                        final String data = new String(encodedByte, "US-ASCII");
                                        readBufferPosition = 0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                lblPrinterName.setText(data);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            stopWorker = true;
                        }
                    }

                }
            });

            thread.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Printing Text to Bluetooth Printer //
    private void printData() throws IOException {
        try {


            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            String msg = "QRPARKING FACIL";

            BitMatrix bitMatrix = multiFormatWriter.encode(msg, BarcodeFormat.QR_CODE, 500, 500);  //2000, 2000
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(bitMatrix);
            //ivQRCode.setImageBitmap(bitmap);


            //msg += "\n";
           //outputStream.write(msg.getBytes());
            printPhoto(R.drawable.ic_icon_pos, bitmap);

//            outputStream.write(bitmap.getRowBytes());
//            lblPrinterName.setText("Printing Text...");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Disconnect Printer //
    void disconnectBT() throws IOException {
        try {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            lblPrinterName.setText("Printer Disconnected.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void printPhoto(int img, Bitmap bitImage) {
        try {
//            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
//                    img);

            Bitmap bmp = bitImage;
            if(bmp!=null){
                byte[] command = Utils.decodeBitmap(bmp);
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                printText(command);
            }else{
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    private void printText(byte[] msg) {
        try {
            // Print normal text
            outputStream.write(msg);
            printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printNewLine() {
        try {
            outputStream.write(PrinterCommands.FEED_LINE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void printCustom(String msg, int size, int align) {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B,0x21,0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B,0x21,0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B,0x21,0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B,0x21,0x10}; // 3- bold with large text
        try {
            switch (size){
                case 0:
                    outputStream.write(cc);
                    break;
                case 1:
                    outputStream.write(bb);
                    break;
                case 2:
                    outputStream.write(bb2);
                    break;
                case 3:
                    outputStream.write(bb3);
                    break;
            }

            switch (align){
                case 0:
                    //left align
                    outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                    break;
                case 1:
                    //center align
                    outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                    break;
                case 2:
                    //right align
                    outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                    break;
            }
            outputStream.write(msg.getBytes());
            outputStream.write(PrinterCommands.LF);
            //outputStream.write(cc);
            //printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void printUnicode(){
        try {
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
            printText(Utils.UNICODE_TEXT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

