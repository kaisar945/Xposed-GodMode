package com.viewblocker.jrsen.rule;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;

import java.util.Arrays;

@Keep
public final class ViewRule_V274 implements Parcelable {

    public transient int a;
    public String b;
    public final int c;
    public final int d;
    public final int e;
    public final int f;
    public transient Bitmap g;
    public final String h;
    public final String i;
    public final int[] j;
    public final String k;
    public int l;
    public final long m;

    public ViewRule_V274(int i, int i2, int i3, int i4, Bitmap bitmap, String str, String str2, int[] iArr, String str3, int i5, long j) {
        this.c = i;
        this.d = i2;
        this.e = i3;
        this.f = i4;
        this.g = bitmap;
        this.h = str;
        this.i = str2;
        this.j = iArr;
        this.k = str3;
        this.l = i5;
        this.m = j;
    }

    public ViewRule_V274(int i, String str, int i2, int i3, int i4, int i5, String str2, String str3, int[] iArr, String str4, int i6, long j) {
        this.a = i;
        this.b = str;
        this.c = i2;
        this.d = i3;
        this.e = i4;
        this.f = i5;
        this.h = str2;
        this.i = str3;
        this.j = iArr;
        this.k = str4;
        this.l = i6;
        this.m = j;
    }

    protected ViewRule_V274(Parcel parcel) {
        this.c = parcel.readInt();
        this.d = parcel.readInt();
        this.e = parcel.readInt();
        this.f = parcel.readInt();
        this.g = parcel.readParcelable(Bitmap.class.getClassLoader());
        this.h = parcel.readString();
        this.i = parcel.readString();
        this.j = parcel.createIntArray();
        this.k = parcel.readString();
        this.l = parcel.readInt();
        this.m = parcel.readLong();
    }

    public int a(Resources resources) {
        if (this.k == null) {
            return -1;
        }
        String[] split = this.k.split(":");
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
        ViewRule_V274 bVar = (ViewRule_V274) obj;
        return (this.h.equals(bVar.h) && this.i.equals(bVar.i)) ? Arrays.equals(this.j, bVar.j) : false;
    }

    public int hashCode() {
        return (((this.h.hashCode() * 31) + this.i.hashCode()) * 31) + Arrays.hashCode(this.j);
    }

    public String toString() {
        return "ViewRule{_id=" + this.a + ", viewThumbnailFilePath='" + this.b + '\'' + ", x=" + this.c + ", y=" + this.d + ", width=" + this.e + ", height=" + this.f + ", viewThumbnail=" + this.g + ", activityClassName='" + this.h + '\'' + ", viewClassName='" + this.i + '\'' + ", viewHierarchyDepth=" + Arrays.toString(this.j) + ", resourceName='" + this.k + '\'' + ", visibility=" + this.l + ", recordTimeStamp=" + this.m + '}';
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.c);
        parcel.writeInt(this.d);
        parcel.writeInt(this.e);
        parcel.writeInt(this.f);
        parcel.writeParcelable(this.g, i);
        parcel.writeString(this.h);
        parcel.writeString(this.i);
        parcel.writeIntArray(this.j);
        parcel.writeString(this.k);
        parcel.writeInt(this.l);
        parcel.writeLong(this.m);
    }

    public static final Creator<ViewRule_V274> CREATOR = new Creator<ViewRule_V274>() {
        @Override
        public ViewRule_V274 createFromParcel(Parcel in) {
            return new ViewRule_V274(in);
        }

        @Override
        public ViewRule_V274[] newArray(int size) {
            return new ViewRule_V274[size];
        }
    };
}
