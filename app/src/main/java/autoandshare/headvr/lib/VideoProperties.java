package autoandshare.headvr.lib;

import android.content.SharedPreferences;
import android.net.Uri;

import com.tencent.mmkv.MMKV;

public class VideoProperties {
    private MMKV prefPostion;
    private SharedPreferences.Editor editorPosition;
    private MMKV prefForce2D;
    private SharedPreferences.Editor editorForce2D;

    public VideoProperties() {

        prefPostion = MMKV.mmkvWithID("VideoProperties-Position");
        editorPosition = prefPostion.edit();
        prefForce2D = MMKV.mmkvWithID("VideoProperties-Force2D");
        editorForce2D = prefForce2D.edit();
    }

    public float getPosition(Uri uri) {
        float position = prefPostion.getFloat(uri.toString(), 0);
        if ((position < 0) || (position > 1)) {
            position = 0;
        }
        return position;
    }

    public void setPosition(Uri uri, float position) {
        if (position == 0) {
            editorPosition.remove(uri.toString());
        } else {
            editorPosition.putFloat(uri.toString(), position);
        }
        editorPosition.apply();
    }

    public boolean getForce2D(Uri uri) {
        return prefForce2D.getBoolean(uri.toString(), false);
    }

    public void setForce2D(Uri uri, boolean force2D) {
        if (force2D) {
            editorForce2D.putBoolean(uri.toString(), true);
        } else {
            editorForce2D.remove(uri.toString());
        }
        editorForce2D.apply();
    }
}
