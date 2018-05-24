package com.deveire.dev.instructacon.remastered;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.util.Log;

/**
 * Created by owenryan on 15/05/2018.
 */

public class NfcScanner
{
    public static NfcAdapter setupNfcScanner(final Activity activity)
    {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if(nfcAdapter == null)
        {
            Log.e("NFCTEST", "Error: default nfcAdapter is null");
        }
        else
        {
            Log.i("NFCTEST", "NFC adapter found");
            if(nfcAdapter.isEnabled())
            {
                Log.i("NFCTEST", "NFC is enabled");
                return nfcAdapter;
            }
        }
        Log.e("NFCTEST", "setupNfcScanner: NFC adapter not setup, returning null");
        return null;
    }

    public static String getTagIDFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.i("NFCTEST", "tag: " + tag);
            Log.i("NFCTEST", "tag translated: " + bytesToHexString(tag.getId()));
            return bytesToHexString(tag.getId());
            //debug.setText(bytesToHexString(tag.getId()));
            /*String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }*/
        }
        return null;
    }


    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        Log.i("NFCTEST", "setupForgroundDispatch Start");
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        IntentFilter[] filters = new IntentFilter[2];
        filters[0] = new IntentFilter();
        filters[0].addAction("android.nfc.action.TECH_DISCOVERED");
        filters[1] = new IntentFilter();
        filters[1].addAction("android.nfc.action.TAG_DISCOVERED");
        filters[1].addCategory(Intent.CATEGORY_DEFAULT);

        Log.i("NFCTEST", "setupForgroundDispatch enabling");
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
        Log.i("NFCTEST", "setupForgroundDispatch End");
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        Log.i("NFCTEST", "stopForegroundDispatch");
        adapter.disableForegroundDispatch(activity);
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }

}
