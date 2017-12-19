package com.schibstedspain.leku.ui.search;

import android.content.Context;
import android.location.Address;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.schibstedspain.leku.StringUtils;

/**
 * Created by petrg on 18.12.2017.
 */

public class AddressItemHolder extends RecyclerView.ViewHolder {

    private final TextView tvText;

    public AddressItemHolder(View itemView) {
        super(itemView);
        this.tvText = itemView.findViewById(android.R.id.text1);
    }

    public void bind(Address address) {
        String addressString = StringUtils.joinNullable(", ",
                address.getLocality(),
                address.getThoroughfare(),
                address.getSubThoroughfare());
        this.tvText.setText(addressString);
    }

    public static final class Factory {

        private final LayoutInflater inflater;

        public Factory(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        public AddressItemHolder createViewHolder(ViewGroup parent) {
            View view = this.inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            AddressItemHolder holder = new AddressItemHolder(view);
            return holder;
        }
    }
}
