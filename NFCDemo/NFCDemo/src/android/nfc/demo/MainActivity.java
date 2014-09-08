/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.nfc.demo;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity
{
	// private static final String TAG = "stickynotes";
	private boolean mResumed = false;
	private boolean mWriteMode = false;
	NfcAdapter mNfcAdapter;
	EditText mNote1;
	EditText mNoteRead;
	PendingIntent mNfcPendingIntent;
	IntentFilter[] mWriteTagFilters;
	IntentFilter[] mNdefExchangeFilters;

	Builder dialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		findViewById(R.id.write_tag).setOnClickListener(mTagWriter);
		findViewById(R.id.read_tag).setOnClickListener(mTagRead);
		mNote1 = ((EditText) findViewById(R.id.note1));

		mNoteRead = ((EditText) findViewById(R.id.noteRead));

		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try
		{
			ndefDetected.addDataType("text/plain");
		} catch (MalformedMimeTypeException e)
		{
		}
		mNdefExchangeFilters = new IntentFilter[] { ndefDetected };

		// Intent filters for writing to a tag
		// IntentFilter tagDetected = new
		// IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		// mWriteTagFilters = new IntentFilter[] { tagDetected };
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mResumed = true;
		// Sticky notes received from Android
		// if
		// (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
		// {
		//
		// setIntent(new Intent()); // Consume this intent.
		// }
		// enableNdefExchangeMode();

		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mNdefExchangeFilters, null);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mResumed = false;
		mNfcAdapter.disableForegroundNdefPush(this);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		if (!mWriteMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))
		{
			NdefMessage[] msgs = getNdefMessages(intent);
			String body = new String(msgs[0].getRecords()[0].getPayload());

			System.out.println("****璇诲彇鏁版嵁****" + body);
			mNoteRead.setText(body);
		}

		if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))
		{
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			writeTag(getNoteAsNdef(), detectedTag);
			System.out.println("姝ゅ鍐欐暟鎹�");
		}
	}

	private View.OnClickListener mTagWriter = new View.OnClickListener()
	{
		@Override
		public void onClick(View arg0)
		{
			disableNdefExchangeMode();
			enableTagWriteMode();
			dialog = new AlertDialog.Builder(MainActivity.this);
			dialog.setTitle("Touch tag to write").setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				@Override
				public void onCancel(DialogInterface dialog)
				{
					disableTagWriteMode();
					enableNdefExchangeMode();
				}
			}).create().show();
		}
	};

	private View.OnClickListener mTagRead = new View.OnClickListener()
	{
		@Override
		public void onClick(View arg0)
		{
			disableTagWriteMode();
			enableNdefExchangeMode();

			dialog = new AlertDialog.Builder(MainActivity.this);

			dialog.setTitle("Touch tag to read").setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				@Override
				public void onCancel(DialogInterface dialog)
				{
					disableTagWriteMode();
					enableNdefExchangeMode();
				}
			}).create().show();
		}
	};

	private NdefMessage getNoteAsNdef()
	{

		// TODO
		byte[] textBytes = (mNote1.getText().toString()).getBytes();

		NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(), new byte[] {}, textBytes);
		return new NdefMessage(new NdefRecord[] { textRecord });
	}

	NdefMessage[] getNdefMessages(Intent intent)
	{
		// Parse the intent
		NdefMessage[] msgs = null;
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
		{
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null)
			{
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++)
				{
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			} else
			{
				// Unknown tag type
				byte[] empty = new byte[] {};
				NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
				NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
				msgs = new NdefMessage[] { msg };
			}
		} else
		{
			// Log.d(TAG, "Unknown intent.");
			finish();
		}
		return msgs;
	}

	private void enableNdefExchangeMode()
	{
		mNfcAdapter.enableForegroundNdefPush(MainActivity.this, getNoteAsNdef());
		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mNdefExchangeFilters, null);
	}

	private void disableNdefExchangeMode()
	{
		mNfcAdapter.disableForegroundNdefPush(this);
		mNfcAdapter.disableForegroundDispatch(this);
	}

	private void enableTagWriteMode()
	{
		mWriteMode = true;
		IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { tagDetected };
		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
	}

	private void disableTagWriteMode()
	{
		mWriteMode = false;
		mNfcAdapter.disableForegroundDispatch(this);
	}

	boolean writeTag(NdefMessage message, Tag tag)
	{
		int size = message.toByteArray().length;

		try
		{
			Ndef ndef = Ndef.get(tag);
			if (ndef != null)
			{
				ndef.connect();

				if (!ndef.isWritable())
				{
					System.out.println("Tag is read-only.");
					return false;
				}
				if (ndef.getMaxSize() < size)
				{
					System.out.println("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.");
					return false;
				}

				ndef.writeNdefMessage(message);
				System.out.println("****鍐欏叆鏁版嵁鎴愬姛***");

				return true;
			} else
			{
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null)
				{
					try
					{
						format.connect();
						format.format(message);
						System.out.println("**Formatted tag and wrote message**");
						return true;
					} catch (IOException e)
					{
						System.out.println("==Failed to format tag.==");
						return false;
					}
				} else
				{
					System.out.println("Tag doesn't support NDEF.");
					return false;
				}
			}
		} catch (Exception e)
		{

			System.out.println("!!鍐欏叆鏁版嵁澶辫触!!");
		}
		return false;
	}
}