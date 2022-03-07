package com.kaisar.xposed.godmode.bean;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

@Keep
public final class GroupInfo {

    @SerializedName("group_name")
    public String group_name;
    @SerializedName("group_link")
    public String group_link;

}
