package com.viewblocker.jrsen.rule;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;

import java.util.Arrays;

@Keep
public final class ViewRule_V275 implements Parcelable {

    public transient int a;
    public String b;
    public String c;
    public final int d;
    public final int e;
    public final int f;
    public final int g;
    public transient Bitmap h;
    public final String i;
    public final String j;
    public final int[] k;
    public final String l;
    public int m;
    public final long n;

    public ViewRule_V275(int i, String str, String str2, int i2, int i3, int i4, int i5, String str3, String str4, int[] iArr, String str5, int i6, long j) {
        this.a = i;
        this.b = str;
        this.c = str2;
        this.d = i2;
        this.e = i3;
        this.f = i4;
        this.g = i5;
        this.i = str3;
        this.j = str4;
        this.k = iArr;
        this.l = str5;
        this.m = i6;
        this.n = j;
    }

    protected ViewRule_V275(Parcel parcel) {
        this.d = parcel.readInt();
        this.e = parcel.readInt();
        this.f = parcel.readInt();
        this.g = parcel.readInt();
        this.h = parcel.readParcelable(Bitmap.class.getClassLoader());
        this.i = parcel.readString();
        this.j = parcel.readString();
        this.k = parcel.createIntArray();
        this.l = parcel.readString();
        this.m = parcel.readInt();
        this.n = parcel.readLong();
    }

    public ViewRule_V275(String str, int i, int i2, int i3, int i4, Bitmap bitmap, String str2, String str3, int[] iArr, String str4, int i5, long j) {
        this.c = str;
        this.d = i;
        this.e = i2;
        this.f = i3;
        this.g = i4;
        this.h = bitmap;
        this.i = str2;
        this.j = str3;
        this.k = iArr;
        this.l = str4;
        this.m = i5;
        this.n = j;
    }

    public int a(Resources resources) {
        if (this.l == null) {
            return -1;
        }
        String[] split = this.l.split(":");
        String[] split2 = split[1].split("/");
        String str = split[0];
        return resources.getIdentifier(split2[1], split2[0], str);
    }

    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ViewRule_V275 bVar = (ViewRule_V275) obj;
        return (this.i.equals(bVar.i) && this.j.equals(bVar.j)) ? Arrays.equals(this.k, bVar.k) : false;
    }

    public int hashCode() {
        return (((this.i.hashCode() * 31) + this.j.hashCode()) * 31) + Arrays.hashCode(this.k);
    }

    public String toString() {
        return "ViewRule{_id=" + this.a + ", viewThumbnailFilePath='" + this.b + '\'' + ", x=" + this.d + ", y=" + this.e + ", width=" + this.f + ", height=" + this.g + ", viewThumbnail=" + this.h + ", activityClassName='" + this.i + '\'' + ", viewClassName='" + this.j + '\'' + ", viewHierarchyDepth=" + Arrays.toString(this.k) + ", resourceName='" + this.l + '\'' + ", visibility=" + this.m + ", recordTimeStamp=" + this.n + '}';
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.d);
        parcel.writeInt(this.e);
        parcel.writeInt(this.f);
        parcel.writeInt(this.g);
        parcel.writeParcelable(this.h, i);
        parcel.writeString(this.i);
        parcel.writeString(this.j);
        parcel.writeIntArray(this.k);
        parcel.writeString(this.l);
        parcel.writeInt(this.m);
        parcel.writeLong(this.n);
    }

    public static final Creator<ViewRule_V275> CREATOR = new Creator<ViewRule_V275>() {
        @Override
        public ViewRule_V275 createFromParcel(Parcel in) {
            return new ViewRule_V275(in);
        }

        @Override
        public ViewRule_V275[] newArray(int size) {
            return new ViewRule_V275[size];
        }
    };
}
