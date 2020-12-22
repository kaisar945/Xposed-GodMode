package com.kaisar.xposed.godmode.injection.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jrsen on 17-10-18.
 */

public final class Property<V> {

    private V v;
    private final List<OnPropertyChangeListener<V>> listeners;

    {
        listeners = new ArrayList<>();
    }

    public Property() {
    }

    public Property(V v) {
        this.v = v;
    }

    public void set(V v) {
        if (this.v != v) {
            this.v = v;
            notifyPropertyHasChanged(v);
        }
    }

    public V get() {
        return v;
    }

    private void notifyPropertyHasChanged(V v) {
        //不知道为什么synchronized老是出问题
        ArrayList<OnPropertyChangeListener<V>> listeners = new ArrayList<>(this.listeners);
        final int N = listeners.size();
        for (int i = 0; i < N; i++) {
            listeners.get(i).onPropertyChange(v);
        }
    }

    public void addOnPropertyChangeListener(OnPropertyChangeListener<V> listener) {
        listeners.add(listener);
    }

    public void removeOnPropertyChangeListener(OnPropertyChangeListener<V> listener) {
        listeners.remove(listener);
    }

    public interface OnPropertyChangeListener<V> {
        void onPropertyChange(V v);
    }
}
