package com.phamtuan.calendar.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.phamtuan.calendar.R;
import com.phamtuan.calendar.models.GroupMenu;

import java.util.ArrayList;

/**
 * Created by P.Tuan on 1/23/2018.
 */

public class MainMenuAdapter extends BaseExpandableListAdapter {
    LayoutInflater infalInflater;
    Context mContext;
    ArrayList<GroupMenu> menus;

    public MainMenuAdapter(Context context, ArrayList<GroupMenu> menu) {
        this.mContext = context;
        this.menus = menu;
        infalInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getGroupCount() {
        return menus.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return menus.get(i).getSideMenus().size();
    }

    @Override
    public Object getGroup(int i) {
        return null;
    }

    @Override
    public Object getChild(int i, int i1) {
        return null;
    }

    @Override
    public long getGroupId(int i) {
        return 0;
    }

    @Override
    public long getChildId(int i, int i1) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = infalInflater.inflate(R.layout.layout_header_menu, null);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.tvNameHeader);
        tvName.setText(menus.get(i).getName());

        return convertView;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = infalInflater.inflate(R.layout.item_child_menu, null);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
        ImageView imgAvart = (ImageView) convertView.findViewById(R.id.imgMenu);
        tvName.setText(menus.get(i).getSideMenus().get(i1).getName());
        imgAvart.setImageResource(menus.get(i).getSideMenus().get(i1).getImage());
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
