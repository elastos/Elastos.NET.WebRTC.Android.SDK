package org.elastos.carrier.webrtc.demo_apprtc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import org.elastos.carrier.webrtc.WebrtcClient;
import org.elastos.carrier.webrtc.demo_apprtc.apprtc.R;
import org.elastos.carrier.webrtc.demo_apprtc.fixtures.MessagesFixtures;
import org.elastos.carrier.webrtc.demo_apprtc.model.Message;
import org.elastos.carrier.webrtc.demo_apprtc.model.User;
import org.elastos.carrier.webrtc.demo_apprtc.util.AppUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class DefaultMessagesActivity extends DemoMessagesActivity
        implements MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        MessageInput.TypingListener {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String MESAGE_SHARE_PREF = "message_share_save_pref";
    private SharedPreferences sharedPref;

    public static DefaultMessagesActivity INSTANCE;

    private static final String TAG = "DefaultMessagesActivity";

    public static void open(Context context) {
        context.startActivity(new Intent(context, DefaultMessagesActivity.class));
    }

    private MessagesList messagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_messages);

        this.messagesList = findViewById(R.id.messagesList);
        initAdapter();

        MessageInput input = findViewById(R.id.input);
        input.setInputListener(this);
        input.setTypingListener(this);
        input.setAttachmentsListener(this);

        INSTANCE = this;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        loadMessage();
    }

    @Override
    public boolean onSubmit(CharSequence input) {
        try {
            String userId = CarrierClient.getInstance(this).getCarrier().getUserId();
            Log.d(TAG, "onSubmit: from = " + userId + "; message = " + input);
            String id = Long.toString(UUID.randomUUID().getLeastSignificantBits());
            Message message = new Message(id, new User(userId, userId, null, true), input.toString());
            runOnUiThread(() -> super.messagesAdapter.addToStart(message, true));
            WebrtcClient.getInstance().sendMessage(ByteBuffer.wrap(input.toString().getBytes()), false);

            saveMessage(message);
        } catch (Exception e) {
            Log.e(TAG, "onSubmit: ", e);
        }
        return true;
    }

    @Override
    public void onAddAttachments() {
        super.messagesAdapter.addToStart(
                MessagesFixtures.getImageMessage(), true);
    }

    public void receiveMessage(String text, String from) {
        try {
            Log.d(TAG, "receiveMessage: from = " + from + "; message = " + text);
            String id = Long.toString(UUID.randomUUID().getLeastSignificantBits());
            Message message = new Message(id, new User(from, from, null, true), text);
            runOnUiThread(() -> super.messagesAdapter.addToStart(message, true));

            saveMessage(message);
        } catch (Exception e) {
            Log.e(TAG, "receiveMessage: ", e);
        }
    }

    private void initAdapter() {
        try {
            String userId = CarrierClient.getInstance(this).getCarrier().getUserId();
            super.messagesAdapter = new MessagesListAdapter<>(userId, super.imageLoader);
            super.messagesAdapter.enableSelectionMode(this);
            super.messagesAdapter.setLoadMoreListener(this);
            super.messagesAdapter.registerViewClickListener(R.id.messageUserAvatar,
                    new MessagesListAdapter.OnMessageViewClickListener<Message>() {
                        @Override
                        public void onMessageViewClick(View view, Message message) {
                            AppUtils.showToast(DefaultMessagesActivity.this,
                                    message.getUser().getName() + " avatar click",
                                    false);
                        }
                    });
            this.messagesList.setAdapter(super.messagesAdapter);
        } catch (Exception e) {
            Log.e(TAG, "initAdapter: ", e);
        }
    }

    @Override
    public void onStartTyping() {
        Log.v("Typing listener", getString(R.string.start_typing_status));
    }

    @Override
    public void onStopTyping() {
        Log.v("Typing listener", getString(R.string.stop_typing_status));
    }

    private void loadMessage() {
        try {
            String talkTo = WebrtcClient.getInstance().getPeerAddress();
            String jsonString = sharedPref.getString(MESAGE_SHARE_PREF, null);
            if (jsonString != null) {
                JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.has(talkTo)) {
                    JSONArray jsonArray = jsonObject.getJSONArray(talkTo);
                    if (jsonArray != null && jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject mJson = jsonArray.getJSONObject(i);
                            String id = mJson.getString("id");
                            String userId = mJson.getString("userId");
                            String text = mJson.getString("text");
                            Date createAt = SIMPLE_DATE_FORMAT.parse(mJson.getString("date"));
                            Message message = new Message(id, new User(userId, userId), text, createAt);

                            runOnUiThread(() -> super.messagesAdapter.addToStart(message, true));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadMessage: ", e);
        }
    }

    private void saveMessage(Message m) {
        if (m == null) {
            return;
        }

        saveMessage(m.getId(), m.getText(), m.getUser().getId(), SIMPLE_DATE_FORMAT.format(m.getCreatedAt()));
    }

    private void saveMessage(String id, String text, String userId, String date) {
        try {
            String talkTo = WebrtcClient.getInstance().getPeerAddress();
            JSONObject jsonObject;
            String jsonString = sharedPref.getString(MESAGE_SHARE_PREF, null);
            if (jsonString != null) {
                jsonObject = new JSONObject(jsonString);
            } else {
                jsonObject = new JSONObject();
            }

            JSONArray jsonArray;
            if (jsonObject.has(talkTo)) {
                jsonArray = jsonObject.getJSONArray(talkTo);
            } else {
                jsonArray = new JSONArray();
            }

            JSONObject message = new JSONObject();
            message.put("id", id);
            message.put("text", text);
            message.put("userId", userId);
            message.put("date", date);
            jsonArray.put(message);
            jsonObject.put(talkTo, jsonArray);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(MESAGE_SHARE_PREF, jsonObject.toString());
            editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "saveMessage: ", e);
        }
    }

}
