package org.sil.storyproducer.tools;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.sil.storyproducer.controller.MainActivity;

public class DrawerItemClickListener implements ListView.OnItemClickListener {

    private Context context;

    public DrawerItemClickListener(Context con) {
        context = con;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectItem(position);
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        System.out.println("the position is " + position);
        //TODO add more options
        switch(position) {
            case 0:
                Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                break;
            default:
        }
    }
}
