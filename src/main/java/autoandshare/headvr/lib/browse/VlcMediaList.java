package autoandshare.headvr.lib.browse;

import android.net.Uri;
import android.util.Log;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;
import org.videolan.medialibrary.media.MediaWrapper;

import java.util.ArrayList;
import java.util.List;

import autoandshare.headvr.activity.VlcHelper;
import autoandshare.headvr.lib.PathUtil;

public class VlcMediaList implements PlayList.ListSource {

    private static String tag = "VlcMediaList";


    private VlcHelper.VlcSelection selection;
    private String listPositionKey;

    public VlcMediaList(VlcHelper.VlcSelection vlcSelection) {
        this.selection = vlcSelection;
    }

    public int getPosition() {
        return selection.position;
    }

    public String getPositionKey() {
        return listPositionKey;
    }

    public List<Uri> loadList() {
        List<Uri> list = new ArrayList<>();

        if (selection.list != null) {
            listPositionKey = PathUtil.getKey(Uri.parse(selection.listUrl));

            for (MediaWrapper m : selection.list) {
                list.add(m.getUri());
            }

        } else if (selection.mw != null) {

            listPositionKey = PathUtil.getKey(selection.mw.getUri());

            if (selection.mw.getType() == MediaWrapper.TYPE_DIR) {
                Media m = new Media(VlcHelper.Instance, selection.mw.getUri());
                expand(m, list);
            } else {
                list.add(selection.mw.getUri());
            }
        }
        return list;
    }

    private void expand(Media m, List<Uri> list) {
        m.parse(Media.Parse.ParseNetwork);
        MediaList ml = m.subItems();
        m.release();
        if (ml != null) {
            Log.d(tag, m.getUri().toString() + " media list size " + ml.getCount());
            for (int i = 0; i < ml.getCount(); i++) {
                Media sub_m = ml.getMediaAt(i);
                Log.d(tag, sub_m.getUri().toString());
                if ((sub_m.getType() == Media.Type.Directory) || (sub_m.getType() == Media.Type.Playlist)) {
                    expand(sub_m, list);
                } else {
                    list.add(sub_m.getUri());
                    sub_m.release();
                }
            }
            ml.release();
        }
    }
}