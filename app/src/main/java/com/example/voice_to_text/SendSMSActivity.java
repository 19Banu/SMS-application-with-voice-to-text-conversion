package com.example.voice_to_text;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class SendSMSActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int PERMISSION_SEND_SMS = 123;
    private static final int REQUEST_CODE_SPEECH_INPUT = 456;

    private EditText phoneNumber, message;
    private Button sendButton, voiceInputButton;
    private TextToSpeech tts;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_smsactivity);

        phoneNumber = findViewById(R.id.phone_number);
        message = findViewById(R.id.message);
        sendButton = findViewById(R.id.send_button);
        voiceInputButton = findViewById(R.id.voice_input_button);

        // Initialize Text-to-Speech engine
        tts = new TextToSpeech(this, this);

        // Check if the app has SEND_SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get phone number and message text
                String phone = phoneNumber.getText().toString();
                String msg = message.getText().toString();

                // Check if phone number and message are not empty
                if (phone.trim().isEmpty() || msg.trim().isEmpty()) {
                    Toast.makeText(SendSMSActivity.this, "Please enter a phone number and a message.", Toast.LENGTH_SHORT).show();
                } else {
                    // Use Text-to-Speech engine to speak out the message
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
                    } else {
                        tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
                    }

                    // Send SMS message
                    sendSMS(phone, msg);
                }
            }
        });

        voiceInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start voice input
                startVoiceInput();
            }
        });

        // Register broadcast receiver to receive SMS messages
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shutdown Text-to-Speech engine
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        unregisterReceiver(receiver);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to US English
            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data or TTS not supported
                sendButton.setEnabled(false);
                voiceInputButton.setEnabled(false);
            } else {
                sendButton.setEnabled(true);
                voiceInputButton.setEnabled(true);
            }
        } else {
            // TTS initialization failed
            sendButton.setEnabled(false);
            voiceInputButton.setEnabled(false);
        }
    }

    private void startVoiceInput() {
        // Create an intent for speech recognition
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            // Start the speech recognition activity
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not supported on your device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                // Get the voice input results
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && result.size() > 0) {
                    // Update the message text with the recognized speech
                    String spokenText = result.get(0);
                    message.setText(spokenText);
                }
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            String messageReceived = "";

            if (bundle != null) {
                // Retrieve SMS message from bundle
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                for (int i = 0; i < msgs.length; i++) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String format = bundle.getString("format");
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                    } else {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                    messageReceived += msgs[i].getMessageBody();
                }

                // Use Text-to-Speech engine to speak out the received message
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(messageReceived, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    tts.speak(messageReceived, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        }
    };

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS sending failed.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
