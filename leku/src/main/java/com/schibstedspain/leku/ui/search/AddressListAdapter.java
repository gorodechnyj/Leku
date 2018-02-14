package com.schibstedspain.leku.ui.search;

import android.content.Context;
import android.location.Address;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by petrg on 18.12.2017.
 */

public class AddressListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Address> items;
    private final AddressItemHolder.Factory itemFactory;
    private OnAddressSelectedListener addressSelectedListener;

    public AddressListAdapter(@NonNull Context context) {
        this.items = new ArrayList<>();
        this.itemFactory = new AddressItemHolder.Factory(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return itemFactory.createViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((AddressItemHolder) holder).bind(getItem(position));
        holder.itemView.setOnClickListener(v -> {
            if (this.addressSelectedListener != null) {
                this.addressSelectedListener.onAddressSelected(getItem(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public @Nullable
    Address getItem(int position) {
        try {
            return this.items.get(position);
        } catch (IndexOutOfBoundsException iobe) {
            return null;
        }
    }

    public void clear() {
        this.items.clear();
        notifyDataSetChanged();
    }

    public void add(Address address) {
        this.items.add(address);
        notifyDataSetChanged();
    }

    public void setItems(List<Address> addresses) {
        this.items.clear();
        this.items.addAll(addresses);
        notifyDataSetChanged();
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener addressSelectedListener) {
        this.addressSelectedListener = addressSelectedListener;
    }

    public interface OnAddressSelectedListener {
        void onAddressSelected(Address address);
    }
}
