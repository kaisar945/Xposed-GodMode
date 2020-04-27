package com.viewblocker.jrsen.rule;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.view.View;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

/**
 * Created by jrsen on 17-10-14.
 */
@Keep
public final class ViewRule_V276 implements Parcelable {

    //数据库该条记录id
    public transient int _id;
    //数据库控件缩略图文件路径
    @SerializedName("viewThumbnailFilePath")
    public String viewThumbnailFilePath;
    //该条规则的别名
    @SerializedName("alias")
    public String alias;

    //控件坐标 大小 缩略图
    @SerializedName("x")
    public final int x;//相对于该view所在的window
    @SerializedName("y")
    public final int y;//相对于该view所在的window
    @SerializedName("width")
    public final int width;
    @SerializedName("height")
    public final int height;
    public transient Bitmap viewThumbnail;

    /**
     * Such: {@link Class#getName()}
     */
    @SerializedName("activityClassName")
    public final String activityClassName;

    //验证控件类型
    /**
     * Such: {@link Class#getName()}
     */
    @SerializedName("viewClassName")
    public final String viewClassName;

    //第一维度 根据布局层次
    @SerializedName("viewHierarchyDepth")
    public final int[] viewHierarchyDepth;

    //第二维度 根据id名称反向查找id(可能没有id)Such:com.viewblocker.jrsen:id/v2
    @SerializedName("resourceName")
    public final String resourceName;

    //View可见性
    @SerializedName("visibility")
    public int visibility;

    //规则记录时间
    @SerializedName("recordTimeStamp")
    public final long recordTimeStamp;

    /**
     * for db constructor
     *
     * @param _id
     * @param viewThumbnailFilePath
     * @param x
     * @param y
     * @param width
     * @param height
     * @param activityClassName
     * @param viewClassName
     * @param viewHierarchyDepth
     * @param resourceName
     * @param visibility
     */
    public ViewRule_V276(int _id, String viewThumbnailFilePath, String alias, int x, int y, int width, int height
            , String activityClassName, String viewClassName, int[] viewHierarchyDepth
            , String resourceName, int visibility, long recordTimeStamp) {
        this._id = _id;
        this.viewThumbnailFilePath = viewThumbnailFilePath;
        this.alias = alias;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.activityClassName = activityClassName;
        this.viewClassName = viewClassName;
        this.viewHierarchyDepth = viewHierarchyDepth;
        this.resourceName = resourceName;
        this.visibility = visibility;
        this.recordTimeStamp = recordTimeStamp;
    }

    /**
     * for client process constructor
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @param viewThumbnail
     * @param activityClassName
     * @param viewClassName
     * @param viewHierarchyDepth
     * @param resourceName
     * @param visibility
     */
    public ViewRule_V276(String alias, int x, int y, int width, int height, Bitmap viewThumbnail
            , String activityClassName, String viewClassName, int[] viewHierarchyDepth
            , String resourceName, int visibility, long recordTimeStamp) {
        this.alias = alias;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.viewThumbnail = viewThumbnail;
        this.activityClassName = activityClassName;
        this.viewClassName = viewClassName;
        this.viewHierarchyDepth = viewHierarchyDepth;
        this.resourceName = resourceName;
        this.visibility = visibility;
        this.recordTimeStamp = recordTimeStamp;
    }


    protected ViewRule_V276(Parcel in) {
        x = in.readInt();
        y = in.readInt();
        width = in.readInt();
        height = in.readInt();
        viewThumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        activityClassName = in.readString();
        viewClassName = in.readString();
        viewHierarchyDepth = in.createIntArray();
        resourceName = in.readString();
        visibility = in.readInt();
        recordTimeStamp = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(x);
        dest.writeInt(y);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeParcelable(viewThumbnail, flags);
        dest.writeString(activityClassName);
        dest.writeString(viewClassName);
        dest.writeIntArray(viewHierarchyDepth);
        dest.writeString(resourceName);
        dest.writeInt(visibility);
        dest.writeLong(recordTimeStamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ViewRule> CREATOR = new Creator<ViewRule>() {
        @Override
        public ViewRule createFromParcel(Parcel in) {
            return new ViewRule(in);
        }

        @Override
        public ViewRule[] newArray(int size) {
            return new ViewRule[size];
        }
    };

    public int getViewId(Resources res) {
        if (resourceName != null) {
            String[] start = resourceName.split(":");
            String[] end = start[1].split("/");
            String resourcePackageName = start[0];
            String resourceTypeName = end[0];
            String resourceEntryName = end[1];
            return res.getIdentifier(resourceEntryName, resourceTypeName, resourcePackageName);
        }
        return View.NO_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewRule viewRule = (ViewRule) o;

        if (!activityClassName.equals(viewRule.activityClassName)) return false;
        if (!viewClassName.equals(viewRule.viewClassName)) return false;
        return Arrays.equals(viewHierarchyDepth, viewRule.viewHierarchyDepth);

    }

    @Override
    public int hashCode() {
        int result = activityClassName.hashCode();
        result = 31 * result + viewClassName.hashCode();
        result = 31 * result + Arrays.hashCode(viewHierarchyDepth);
        return result;
    }

    @Override
    public String toString() {
        return "ViewRule{" +
                "_id=" + _id +
                ", viewThumbnailFilePath='" + viewThumbnailFilePath + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", viewThumbnail=" + viewThumbnail +
                ", activityClassName='" + activityClassName + '\'' +
                ", viewClassName='" + viewClassName + '\'' +
                ", viewHierarchyDepth=" + Arrays.toString(viewHierarchyDepth) +
                ", resourceName='" + resourceName + '\'' +
                ", visibility=" + visibility +
                ", recordTimeStamp=" + recordTimeStamp +
                '}';
    }
}
