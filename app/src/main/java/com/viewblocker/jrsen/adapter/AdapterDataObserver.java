package com.viewblocker.jrsen.adapter;

public interface AdapterDataObserver<T> {

    int getSize();

    T getItem(int position);

    void onItemRemoved(int position);

    void onItemChanged(int position);

}
